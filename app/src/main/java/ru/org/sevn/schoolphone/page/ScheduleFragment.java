/*
 * Copyright 2018 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.org.sevn.schoolphone.page;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import ru.org.sevn.schoolphone.AppConstants;
import ru.org.sevn.schoolphone.PersonalConstants;
import ru.org.sevn.schoolphone.R;
import ru.org.sevn.schoolphone.andr.AlarmUtil;
import ru.org.sevn.schoolphone.andr.DialogUtil;
import ru.org.sevn.schoolphone.andr.MediaUtil;

public class ScheduleFragment extends Fragment {

    private static ArrayList<Event> events = new ArrayList<>();

    static {
        fillEvents(events, (Collection<Event>)PersonalConstants.getObject(AppConstants.EVENTS));
    }

    public static Collection<AlarmUtil.SetAlarm> setAlarms(Context ctx) {
        ArrayList<AlarmUtil.SetAlarm> ret = new ArrayList<>();
        ArrayList<Event> events = new ArrayList<>();
        fillEvents(events, (Collection<Event>)PersonalConstants.getObject(AppConstants.EVENTS));
        for (Event e : events) {
            AlarmUtil.SetAlarm r = e.setAlarm(ctx);
            if (r != null) {
                ret.add(r);
            }
        }
        return ret;
    }
    private static void fillEvents(ArrayList<Event> events, Collection<Event> from) {
        for(Event e : from) {
            Event evt = new Event(e);
            boolean repeated = false;
            for(int i = 0; i < e.days.length; i++) {
                if (e.days[i]) {
                    repeated = true;
                    evt.days[i] = true;
                    events.add(evt);
                    evt = new Event(e);
                }
            }
            if (!repeated) {
                events.add(evt);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        textView.setMovementMethod(new ScrollingMovementMethod());
        return rootView;
    }
    public void refresh() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/dd");

            View rootView = getView();
            Calendar now = GregorianCalendar.getInstance();
            Collections.sort(events, new EventComparator(now));
            if (rootView != null) {
                TextView textView = (TextView) rootView.findViewById(R.id.section_label);
                String s = "";
                Calendar from1 = null;
                for (Event evt : events) {
                    Calendar fromc = GregorianCalendar.getInstance();
                    fromc.setTime(evt.getFromDate(now));
                    if (from1 == null) {
                        s += sdf1.format(fromc.getTime());
                        s += "\n";
                    } else {
                        if (fromc.get(Calendar.YEAR) == from1.get(Calendar.YEAR)
                                && fromc.get(Calendar.MONTH) == from1.get(Calendar.MONTH)
                                && fromc.get(Calendar.DATE) == from1.get(Calendar.DATE)
                                ) {

                        } else {
                            s += "\n";
                            s += sdf1.format(fromc.getTime());
                            s += "\n";
                        }
                    }
                    from1 = fromc;
                    s += evt.getString(now, !true);
                    s += "\n";
                }
                String txt = /*"" + sdf.format(now.getTime()) + "["+now.get(Calendar.DAY_OF_WEEK)+ "]\n\n" + */s;
                textView.setText(txt);
            }
            //Html.fromHtml(
        } catch (Exception e) {
            //clone calendar not supported
            e.printStackTrace();
        }
    }
    //[days] from to name
    //attime totime name

    public static class EventComparator implements Comparator<Event> {
        private Calendar now;
        public EventComparator(Calendar now) {
            this.now = now;
        }
        @Override
        public int compare(Event o1, Event o2) {
            return o1.getFromDate(now).compareTo(o2.getFromDate(now));
        }
    }

    public static class Event {
        private String name = "";
        private long from = -1;
        private long to = -1;
        private boolean[] days = new boolean[7];

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getFrom() {
            return from;
        }

        public Date getFromDate(Calendar now) {
            long dto = to;
            if (to < 0) {
                dto = from;
            }
            Date till =  getDate(now, false, dto);

            Calendar c = GregorianCalendar.getInstance();
            c.setTime(till);
            if (now.compareTo(c) >= 0) {
                return getDate(now, true, from);
            }
            return getDate(now, false, from);
        }
        public Date getDate(Calendar now, boolean trans, long from1) {
            Calendar now1 = (Calendar)now.clone();
            now1.set(Calendar.HOUR_OF_DAY, 0);
            now1.set(Calendar.MINUTE, 0);
            now1.set(Calendar.SECOND, 0);
            now1.set(Calendar.MILLISECOND, 1);
            int weekDay = now.get(Calendar.DAY_OF_WEEK)- 2;
            if (weekDay < 0) {
                weekDay = 6;
            }
            int o1d = -1;
            for (int i = 0; i < days.length; i++) {
                if (days[i]) {
                    if (o1d < 0) {
                        o1d = i;
                    } else {
                        o1d = Math.min(o1d, i);
                    }
                    if (i >= weekDay) { //TODO =
                        o1d = i;
                        break;
                    }
                }
            }
            if (o1d >= 0 && o1d < weekDay) {
                if (trans) {
                    o1d = 7 + o1d;
                }
            }
            if (o1d >= 0) {
                long h = from1 / 1000 / 60 / 60;
                long m = (from1 - h * 1000 * 60 * 60) / 1000 / 60;
                now1.set(Calendar.HOUR_OF_DAY, (int)h);
                now1.set(Calendar.MINUTE, (int)m);
                now1.add(Calendar.DATE, (o1d - weekDay));
                from1 = now1.getTimeInMillis();
                if (from1 < now.getTimeInMillis()) {
                    if (trans) {
                        now1.add(Calendar.DATE, 7);
                        from1 = now1.getTimeInMillis();
                    }
                }
            }
            return new Date(from1);
        }

        public Date getToDate(Calendar now) {
            long dto = to;
            if (to < 0) {
                dto = from;
            }
            Date till =  getDate(now, false, dto);

            Calendar c = GregorianCalendar.getInstance();
            c.setTime(till);
            if (now.compareTo(c) >= 0) {
                return getDate(now, true, dto);
            }
            return till;
        }

        public void setFrom(long from) {
            this.from = from;
        }

        public long getTo() {
            return to;
        }

        public void setTo(long to) {
            this.to = to;
        }

        public boolean[] getDays() {
            return days;
        }

        public void setDays(boolean[] days) {
            this.days = days;
        }

        private static long before = 1000 * 60 * 59;
        public String getString(Calendar now, boolean withDate) {
            String ret = "";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");//TODO
            Date toDate = getToDate(now);
            Date fromDate = getFromDate(now);
            long lnow = now.getTimeInMillis();
            if (lnow >= (fromDate.getTime() - before) && lnow <= toDate.getTime()) {
                ret += "*";
            } else {
                ret += " ";
            }

            if (withDate) {
                ret += sdf.format(fromDate);
                ret += " ";
            }
            ret += sdf1.format(fromDate);
            ret += " ";
            ret += sdf1.format(toDate);
            ret += " ";
            ret += name;
            ret += " ";
            ret += "[";
            boolean[] ldays = days;
            ret += printDays(days);
            if (parent != null) {
                ret += " ";
                ret += printDays(parent.days);
            }
            ret += "]";

            return ret;
        }
        private String printDays(boolean[] ldays) {
            String ret = "";
            for (int i = 0; i < ldays.length; i++) {
                if (ldays[i]) {
                    ret += ("" + (i+1));
                }
            }
            return ret;
        }

        public AlarmUtil.SetAlarm setAlarm(Context context) {
            String id = UUID.randomUUID().toString();
            final Calendar now = GregorianCalendar.getInstance();
            long delayFromNowMs = -1;
            final long interval = AlarmManager.INTERVAL_DAY * 7;
            now.add(Calendar.MINUTE, 5);

            final Date from = getFromDate(now);
            boolean isRepeat = false;
            for (int i = 0; i < days.length; i++) {
                if (days[i]) {
                    isRepeat = true;
                    break;
                }
            }

            delayFromNowMs = from.getTime() - 5*60*1000 - GregorianCalendar.getInstance().getTimeInMillis();

            AlarmUtil.AlarmReceiver receiver = new AlarmUtil.AlarmReceiver() {
                @Override
                public void onReceive(Context context, Intent intent, BroadcastReceiver receiver) {
                    System.err.println("=======3="+Event.this.getString(now, true));
                    final MediaPlayer mp = MediaUtil.playLoopAlarm(context);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

                    DialogUtil.alert(context, "ALARM", "" + name + " " + sdf.format(from), new Runnable(){
                        public void run() {
                            if (mp != null) {
                                mp.stop();
                                mp.release();
                            }
                        }
                    });

                    Toast.makeText(context, "ALARM", Toast.LENGTH_LONG).show();
                }
            };

            AlarmUtil.SetAlarm alarmReceiver = null;
            if (delayFromNowMs >= 0) {
                if (isRepeat) {
                    alarmReceiver = AlarmUtil.setAlarmExact(context,
                            AlarmManager.RTC_WAKEUP,
                            "ru.org.sevn.schoolphone.schedule."+id,
                            receiver,
                            delayFromNowMs,
                            interval
                    );
                } else {
                    alarmReceiver = AlarmUtil.setAlarmExact(context,
                            AlarmManager.RTC_WAKEUP,
                            "ru.org.sevn.schoolphone.schedule."+id,
                            receiver,
                            delayFromNowMs
                    );
                }
            }
            return alarmReceiver;
        }

        private Event parent;
        public Event(Event e) {
            parent = e;
            name = e.name;
            from = e.from;
            to = e.to;
        }
        public Event(String s) {
            s = s.trim();
            if (s.startsWith("[")) {
                int idx = s.indexOf("]");
                if (idx < 0) {
                    idx = s.indexOf(" ");
                }
                if (idx > 0) {
                    String daysStr = s.substring(1, idx);
                    for (int i = 0; i < days.length; i++) {
                        String dname = "" + (i +1);
                        if (daysStr.contains(dname)) {
                            days[i] = true;
                        }
                    }
                    s = s.substring(idx + 1);
                }
            }
            s = s.trim();
            String from = "";
            int idx = s.indexOf(" ");
            if (idx > 0) {
                from = s.substring(0, idx);
                s = s.substring(idx);
            }
            s = s.trim();
            String to = "";
            idx = s.indexOf(" ");
            if (idx > 0) {
                to = s.substring(0, idx);
                s = s.substring(idx);
            }
            name = s.trim();

            SimpleDateFormat sdfHHmm = new SimpleDateFormat("HH:mm");
            SimpleDateFormat sdfYYYYMMDDHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date dateFrom = null;
            try {
                dateFrom = sdfHHmm.parse(from);
                Calendar calendar = GregorianCalendar.getInstance();
                calendar.setTime(dateFrom);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minutes = calendar.get(Calendar.MINUTE);
                setFrom(hour * 60 * 60 * 1000 + minutes * 60 * 1000);
            } catch (Exception e1) {
                try {
                    dateFrom = sdfYYYYMMDDHHmm.parse(from);
                    setFrom(dateFrom.getTime());
                } catch (Exception e2) {

                }
            }
            Date dateTo = null;
            try {
                dateTo = sdfHHmm.parse(to);
                Calendar calendar = GregorianCalendar.getInstance();
                calendar.setTime(dateTo);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minutes = calendar.get(Calendar.MINUTE);
                setTo(hour * 60 * 60 * 1000 + minutes * 60 * 1000);
            } catch (Exception e1) {
                try {
                    dateTo = sdfYYYYMMDDHHmm.parse(from);
                    setTo(dateTo.getTime());
                } catch (Exception e2) {

                }
            }
        }
    }
}