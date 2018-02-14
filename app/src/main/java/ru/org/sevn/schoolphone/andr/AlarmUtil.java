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

package ru.org.sevn.schoolphone.andr;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

import java.util.Calendar;

public class AlarmUtil {

    //https://developer.android.com/training/scheduling/alarms.html#type
    public static void setAlarm(final Context context, String actionName, int startInMS, int repeatIntervalMS) {
        Intent intent = new Intent(actionName);
        //Intent intent = new Intent(context, alarmReceiverClass);
        PendingIntent pintent = PendingIntent.getBroadcast( context, 0, intent, 0 );

        AlarmManager alarmManager = (AlarmManager)(context.getSystemService( Context.ALARM_SERVICE ));
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + startInMS, repeatIntervalMS, pintent);
    }
    public static void cancelAlarm(Context context, String actionName) {
        //Intent intent = new Intent(context, alarmReceiverClass);
        Intent intent = new Intent(actionName);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

    public static interface AlarmReceiver {
        void onReceive(Context context, Intent intent, BroadcastReceiver receiver);
    }

    public static BroadcastReceiver setAlarm(
            final Context context,
            final int alarmType, //AlarmManager.ELAPSED_REALTIME_WAKEUP
            final String intentFilterStr,
            final AlarmReceiver alarmReceiver,
            final long delayFromNowMs) {

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive( Context context, Intent intent ) {
                try {
                    alarmReceiver.onReceive(context, intent, this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                context.unregisterReceiver(this);
            }
        };

        context.registerReceiver( receiver, new IntentFilter(intentFilterStr) );

        PendingIntent pintent = PendingIntent.getBroadcast( context, 0, new Intent(intentFilterStr), 0 );
        AlarmManager manager = (AlarmManager)(context.getSystemService( Context.ALARM_SERVICE ));

        manager.set( alarmType, SystemClock.elapsedRealtime() + delayFromNowMs, pintent );
        return receiver;
    }

    public static BroadcastReceiver setAlarm(
            final Context context,
            final int alarmType, //AlarmManager.RTC_WAKEUP
            final String intentFilterStr,
            final AlarmReceiver alarmReceiver,
            final long delayFromNowMs, //calendar.getTimeInMillis()
            final long interval) { //AlarmManager.INTERVAL_DAY

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive( Context context, Intent intent ) {
                try {
                    alarmReceiver.onReceive(context, intent, this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        context.registerReceiver( receiver, new IntentFilter(intentFilterStr) );

        PendingIntent pintent = PendingIntent.getBroadcast( context, 0, new Intent(intentFilterStr), 0 );
        AlarmManager manager = (AlarmManager)(context.getSystemService( Context.ALARM_SERVICE ));

        manager.setInexactRepeating(
                alarmType,
                ((delayFromNowMs < 0) ? System.currentTimeMillis() : delayFromNowMs),
                interval,
                pintent);
        return receiver;
    }

    public static Calendar makeAlarmTime(int h, int m) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, h);
        calendar.set(Calendar.MINUTE, m);
        return calendar;
    }
}
