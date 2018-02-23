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

package ru.org.sevn.schoolphone;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import ru.org.sevn.schoolphone.andr.AlarmUtil;
import ru.org.sevn.schoolphone.andr.BatteryUtil;
import ru.org.sevn.schoolphone.andr.IOUtil;
import ru.org.sevn.schoolphone.connections.ConnectionUtil;
import ru.org.sevn.schoolphone.mail.MailSenderUtil;
import ru.org.sevn.schoolphone.connections.MobileDataUtil;

//https://www.google.com/maps/?q=55.753786,37.619988
//http://maps.google.com/?q=<lat>,<lng>
//geo:<lat>,<lng>?z=<zoom>

//https://maps.yandex.ru/?ll=37.619988,55.753786&spn=2.124481,0.671008&z=14&l=map&pt=37.619988,55.753786,pmrdm1
public class CheckTopActivityService extends Service {
    private Timer timer = new Timer();
    private BroadcastReceiver alarmReceiver;
    private BroadcastReceiver broadcastReceiver;
    private Context ctx;

    public static final long TIMER_INTERVAL = 1000 * 15;
    public static final long TIMER_INTERVAL_BATTERY = 1000 * 60 * 5;
    public static final long TIMER_INTERVAL_LOCATION = 1000 * 60 * 5;

    public static final int LOW_BATTERY_PCT = 20;

    public static final int HANDLER = 0;
    public static final int HANDLER_BATTERY = 1;
    public static final int HANDLER_LOCATION = 2;
    public static final int HANDLER_STATE = 3;

    public static final double LATITUDE_DELTA = 0.001;
    public static final double LONGITUDE_DELTA = 0.001;

    private LocationManager locationManager;

    ContentObserver contentObserverMobileData = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
//            int mobileDataEnabled = MobileDataUtil.isMobileDataEnabled(ctx);
//            System.err.println("=========="+selfChange+":"+uri+":"+ mobileDataEnabled);
//            if (mobileDataEnabled == 0) {
//                int setMobile = MobileDataUtil.setMobileDataEnabled(ctx, true);
//                System.err.println("=========="+selfChange+":"+uri+":"+ mobileDataEnabled+":"+setMobile);
//            }
        }
    };

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        ctx = this;
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        startService();
    }

    private Intent getBatteryStatus() {
        return ctx.getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    static class LocationInfo {
        private final HashSet<String> enabledProviders = new HashSet<>();
        private Location location;
        private final HashMap<String, Integer> statuses = new HashMap<>();

        public HashSet<String> getEnabledProviders() {
            return enabledProviders;
        }

        public Location getLocation() {
            return location;
        }

        public Location setLocation(Location location) {
            Location prevLocation = this.location;
            this.location = location;
            return prevLocation;
        }

        public HashMap<String, Integer> getStatuses() {
            return statuses;
        }
        public JSONObject toJson() throws JSONException {
            JSONObject ret = new JSONObject();
            ret.put("enabledProviders", new JSONArray(enabledProviders));

            if (location != null) {
                String googleLL = "" + location.getLatitude() + "," + location.getLongitude();
                String yandexLL = "" + location.getLongitude() + "," + location.getLatitude();
                String googleUrl = "https://www.google.com/maps/?q=" + googleLL;
                String yandexUrl = "https://maps.yandex.ru/?ll="+yandexLL+"&spn=2.124481,0.671008&z=14&l=map&pt="+yandexLL+",pmrdm1";
                JSONObject jlocation = new JSONObject();
                jlocation.put("googleUrl", googleUrl);
                jlocation.put("yandexUrl", yandexUrl);
                jlocation.put("Provider", location.getProvider());
                jlocation.put("Latitude", location.getLatitude());
                jlocation.put("Longitude", location.getLongitude());
                jlocation.put("Altitude", location.getAltitude());
                jlocation.put("Accuracy", location.getAccuracy());
                jlocation.put("Bearing", location.getBearing());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    jlocation.put("ElapsedRealtimeNanos", location.getElapsedRealtimeNanos());
                }
                jlocation.put("Speed", location.getSpeed());
                jlocation.put("TimeString", getDateString(location.getTime()));
                jlocation.put("Time", location.getTime());
                ret.put("location", jlocation);
            }
            JSONObject jstatuses = new JSONObject();
            for (String k : statuses.keySet()) {
                jstatuses.put(k, statuses.get(k));
            }
            ret.put("statuses", jstatuses);
            return ret;
        }
        public String toString(int n) {
            try {
                return toJson().toString(2);
            } catch (JSONException e) {
                return super.toString();
            }
        }
        public String toString() {
            try {
                return toJson().toString();
            } catch (JSONException e) {
                return super.toString();
            }
        }
    }
    private LocationInfo locationInfo = new LocationInfo();
    private LocationListener locationListener;

    static class State {
        private Date date;
        private int battery;

        public Date getDate() {
            return date;
        }

        public synchronized void setDate(Date date) {
            this.date = date;
        }

        public int getBattery() {
            return battery;
        }

        public synchronized void setBattery(int battery) {
            this.battery = battery;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject ret = new JSONObject();
            ret.put("battery", battery);
            if (date != null) {
                ret.put("dateString", getDateString(date.getTime()));
                ret.put("date", date.getTime());
            }
            return ret;
        }

        public String toString() {
            try {
                return toJson().toString(2);
            } catch (Exception e) {
                e.printStackTrace();
                return super.toString();
            }
        }

    }
    private State state = new State();
    private void sendBatteryMessage() {
        Date d = new Date();
        Intent batteryStatus = getBatteryStatus();
        int pct = BatteryUtil.getPercentLevel(batteryStatus);
        state.setDate(d);
        state.setBattery(pct);
        String msg = "" + getDateString(d) + " : " + BatteryUtil.isCharging(batteryStatus) + " " + pct + "% " + "\n\n";

        //Log.d("battery", msg);
        handler.sendMessage(handler.obtainMessage(HANDLER_BATTERY, new Object[] {d, msg} ));
        if (pct < LOW_BATTERY_PCT) {
            handler.sendMessage(
                    handler.obtainMessage(
                            HANDLER_STATE,
                            new Object[] {d, getStateMessage("LOW BATTERY"), getStateMessageSubject("LOW BATTERY")} ));
        }
    }
    private String getStateMessageSubject(String cause) {
        String locationLnk = "";
        Location l = locationInfo.getLocation();
        if (l != null) {
            locationLnk = l.getLatitude() + "," + l.getLongitude();
        }
        return cause + ": " + state.getBattery() + "% " + locationLnk + "\n";
    }
    private String getStateMessage(String cause) {
        String locationLnk = "";
        try {
            JSONObject jsonLocationDsc = locationInfo.toJson();
            if (jsonLocationDsc.has("location")) {
                JSONObject jsonLocation = jsonLocationDsc.getJSONObject("location");
                if (jsonLocation.has("googleUrl")) {
                    String url = jsonLocation.optString("googleUrl");
                    locationLnk += (" " + url);
                }
                if (jsonLocation.has("yandexUrl")) {
                    String url = jsonLocation.optString("yandexUrl");
                    locationLnk += (" " + url);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return cause + ": " + state.toString() + " " + locationInfo.toString(2) + " "+locationLnk + "\n";
    }
    private void startService() {
        Uri mobileDataSettingUri = Settings.Secure.getUriFor("mobile_data");
        getApplicationContext()
                .getContentResolver()
                .registerContentObserver(mobileDataSettingUri, true,
                        contentObserverMobileData);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (AppConstants.ACTION_EMERGENCY_CALL_IN.equals(intent.getAction())) {
                    handler.sendMessage(
                            handler.obtainMessage(
                                    HANDLER_STATE,
                                    new Object[]{
                                            new Date(),
                                            getStateMessage("EMERGENCY CALL IN"),
                                            getStateMessageSubject("EMERGENCY CALL IN")
                                    }));
                } else if (AppConstants.ACTION_SOS.equals(intent.getAction())) {
                    handler.sendMessage(
                            handler.obtainMessage(
                                    HANDLER_STATE,
                                    new Object[]{
                                            new Date(),
                                            getStateMessage("SOS"),
                                            getStateMessageSubject("SOS")
                                    }));
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConstants.ACTION_EMERGENCY_CALL_IN);
        intentFilter.addAction(AppConstants.ACTION_SOS);
        //intentFilter.addAction("another action");
        this.registerReceiver( broadcastReceiver, intentFilter );

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                //handler.sendEmptyMessage(HANDLER);
                handleActivityTop();
            }
        }, 0, TIMER_INTERVAL);
        alarmReceiver = AlarmUtil.setAlarm(this,
                AlarmManager.RTC,
                "ru.org.sevn.schoolphone.battery",
                new AlarmUtil.AlarmReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent, BroadcastReceiver receiver) {
                        sendBatteryMessage();
                    }
                },
                -1,
                TIMER_INTERVAL_BATTERY
                );

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Location prevLocation = locationInfo.setLocation(location);
                sendLocation();
                boolean sendIt = false;
                if (prevLocation != null) {
                    if (Math.abs(prevLocation.getLatitude() - location.getLatitude()) > LATITUDE_DELTA ) {
                        sendIt = true;
                    }
                    if (Math.abs(prevLocation.getLongitude() - location.getLongitude()) > LONGITUDE_DELTA ) {
                        sendIt = true;
                    }
                } else {
                    sendIt = true;
                }
                if (sendIt) {
                    handler.sendMessage(
                            handler.obtainMessage(
                                    HANDLER_STATE,
                                    new Object[]{
                                            new Date(),
                                            getStateMessage("LOCATION CHANGED"),
                                            getStateMessageSubject("LOCATION CHANGED")
                                    }));
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                locationInfo.getStatuses().put(provider, status);
                sendLocation();
            }

            @Override
            public void onProviderEnabled(String provider) {
                locationInfo.getEnabledProviders().add(provider);
                sendLocation();
            }

            @Override
            public void onProviderDisabled(String provider) {
                locationInfo.getEnabledProviders().remove(provider);
                sendLocation();
            }
            private void sendLocation() {
                Date d = new Date();
                String msg = "" + getDateString(d) + " : " + locationInfo.toString() + "\n\n";

                //Log.d("location", msg);

                handler.sendMessage(handler.obtainMessage(HANDLER_LOCATION, new Object[] {d, msg}));
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        } else {
//            try {
//                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TIMER_INTERVAL_LOCATION, 10, locationListener);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TIMER_INTERVAL_LOCATION, 10, locationListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, TIMER_INTERVAL_LOCATION, 10, locationListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onDestroy() {
        getContentResolver().unregisterContentObserver(contentObserverMobileData);
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        if (locationManager != null && locationListener != null) locationManager.removeUpdates(locationListener);
        if (timer != null) timer.cancel();
        if (alarmReceiver != null) {
            unregisterReceiver(alarmReceiver);
        }
        super.onDestroy();
    }

    private void handleActivityTop() {
        MainActivity mactivity = MainActivity.SELF;
        ActivityManager activityManager = (ActivityManager)ctx.getSystemService(Activity.ACTIVITY_SERVICE);
        AppDetail ad = null;
        if (activityManager != null && mactivity != null) {
            ComponentName cname = activityManager.getRunningTasks(1).get(0).topActivity;
            ad = new AppDetail(cname);
            if (mactivity.isProcess2kill(ad.getComponentName()) && !mactivity.isSU()) {
                activityManager.moveTaskToFront(mactivity.getTaskId(), 0);
            }
        }
//            Toast.makeText(getApplicationContext(),
//                    "test->"+((ad == null) ? "" : ad.getComponentName()),
//                    Toast.LENGTH_SHORT).show();
    }
    private void handleBattery(Date d, String msg) {
        String mainFileName = MainActivity.EXT_APP_LOG_DIR + "b-"+getDateDayString(d);
        try {
            IOUtil.saveExt(mainFileName, msg.getBytes(IOUtil.FILE_ENCODING), true);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    static class SendStateTask extends AsyncTask<String, Void, Exception> {
        private final Context ctx;
        public SendStateTask(Context c) {
            this.ctx = c;
        }
        @Override
        protected Exception doInBackground(String... params) {
            try {
                if (params.length > 2) {
                    sendState(params[0], params[1], params[2]);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return e;
            }
            return null;
        }
        private int tryConnect(boolean isMobileDataEnabled, boolean sleep) {
            int ret = -1;
            if ("true".equals(PersonalConstants.get(AppConstants.MOBILE_DATA))) {
                int mobileDataEnabled = MobileDataUtil.isMobileDataEnabled(ctx);
                if (isMobileDataEnabled) {
                    if (mobileDataEnabled == 0) {
                        ret = MobileDataUtil.setMobileDataEnabled(ctx, isMobileDataEnabled);
                    }
                } else {
                    if (mobileDataEnabled > 0) {
                        ret = MobileDataUtil.setMobileDataEnabled(ctx, isMobileDataEnabled);
                    }
                }
                if (sleep) {
                    try {
                        Thread.currentThread().sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return ret;
        }
        private void sendState(final String msg, final String locationInfoStr, final String extraSubj) {
            if (!ConnectionUtil.isAirplaneModeOn(ctx)) {
                boolean isOnLine = MailSenderUtil.isOnline(ctx);
                int connected = -1;
                if (!isOnLine) {
                    connected = tryConnect(true, true);
                }
                if (MailSenderUtil.isOnline(ctx)) {
                    String fullMsg = msg + locationInfoStr;
                    String mailId = MailSenderUtil.sendMail(
                            PersonalConstants.get(AppConstants.MAIL_FROM_USER),
                            PersonalConstants.get(AppConstants.MAIL_FROM_PSWD),
                            PersonalConstants.get(AppConstants.MAIL_FROM),
                            PersonalConstants.get(AppConstants.MAIL_TO),
                            "school_phone_state " + PersonalConstants.get(AppConstants.CLIENT_ID) + " " + extraSubj,
                            fullMsg
                    );
                    //System.err.println("======"+mailId);
                }
                if (!isOnLine && connected > 0) {
                    tryConnect(false, false);
                }
            } else {
                //TODO
            }
        }
    }
    private void handleLocation(Date d, String msg) {
        String mainFileName = MainActivity.EXT_APP_LOG_DIR + "l-"+getDateDayString(d);
        try {
            IOUtil.saveExt(mainFileName, msg.getBytes(IOUtil.FILE_ENCODING), true);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    private static String getDateString(long ms) {
        return getDateString(new Date(ms));
    }
    private static String getDateString(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return sdf.format(d);
    }
    private static String getDateDayString(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(d);
    }
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case HANDLER:
                    //handleActivityTop();
                    break;
                case HANDLER_BATTERY: {
                    Object[] arr = (Object[]) msg.obj;
                    handleBattery((Date) arr[0], (String) arr[1]);
                }
                    break;
                case HANDLER_LOCATION: {
                    Object[] arr = (Object[]) msg.obj;
                    handleLocation((Date) arr[0], (String) arr[1]);
                }
                    break;
                case HANDLER_STATE: {
                    Object[] arr = (Object[]) msg.obj;
                    try {
                        if (arr.length > 2) {
                            new SendStateTask(ctx).execute(
                                    getDateString((Date) arr[0]) + " ",
                                    (String) arr[1],
                                    (String) arr[2]
                            );
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }

        }
    };
}