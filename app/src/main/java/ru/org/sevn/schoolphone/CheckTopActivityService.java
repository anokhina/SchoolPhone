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
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

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

import ru.org.sevn.schoolphone.andr.BatteryUtil;
import ru.org.sevn.schoolphone.andr.IOUtil;

public class CheckTopActivityService extends Service {
    private static Timer timer = new Timer();
    private static Timer timerBattery = new Timer();
    private Context ctx;
    public static final long TIMER_INTERVAL = 1000 * 30;
    public static final long TIMER_INTERVAL_BATTERY = 1000 * 60 * 5;
    public static final long TIMER_INTERVAL_LOCATION = 1000 * 60 * 5;

    public static final int HANDLER = 0;
    public static final int HANDLER_BATTERY = 1;
    public static final int HANDLER_LOCATION = 2;

    private Intent batteryStatus;
    private LocationManager locationManager;


    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        ctx = this;
        batteryStatus = ctx.getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        startService();
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

        public void setLocation(Location location) {
            this.location = location;
        }

        public HashMap<String, Integer> getStatuses() {
            return statuses;
        }
        public JSONObject toJson() throws JSONException {
            JSONObject ret = new JSONObject();
            ret.put("enabledProviders", new JSONArray(enabledProviders));

            if (location != null) {
                JSONObject jlocation = new JSONObject();
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

    private void startService() {
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                handler.sendEmptyMessage(HANDLER);
            }
        }, 0, TIMER_INTERVAL);
        timerBattery.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                handler.sendEmptyMessage(HANDLER_BATTERY);
            }
        }, 0, TIMER_INTERVAL_BATTERY);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationInfo.setLocation(location);
                handler.sendEmptyMessage(HANDLER_LOCATION);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                locationInfo.getStatuses().put(provider, status);
                handler.sendEmptyMessage(HANDLER_LOCATION);
            }

            @Override
            public void onProviderEnabled(String provider) {
                locationInfo.getEnabledProviders().add(provider);
                handler.sendEmptyMessage(HANDLER_LOCATION);
            }

            @Override
            public void onProviderDisabled(String provider) {
                locationInfo.getEnabledProviders().remove(provider);
                handler.sendEmptyMessage(HANDLER_LOCATION);
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
        if (locationManager != null && locationListener != null) locationManager.removeUpdates(locationListener);
        if (timer != null) timer.cancel();
        if (timerBattery != null) timerBattery.cancel();
        super.onDestroy();
    }

    private void handleActivityTop() {
        MainActivity mactivity = MainActivity.SELF;
        ActivityManager activityManager = (ActivityManager)ctx.getSystemService(Activity.ACTIVITY_SERVICE);
        AppDetail ad = null;
        if (activityManager != null && mactivity != null) {
            ComponentName cname = activityManager.getRunningTasks(1).get(0).topActivity;
            ad = new AppDetail(cname);
            if (mactivity.isProcess2kill(ad.getComponentName()) && mactivity.isSU()) {
                activityManager.moveTaskToFront(mactivity.getTaskId(), 0);
            }
        }
//            Toast.makeText(getApplicationContext(),
//                    "test->"+((ad == null) ? "" : ad.getComponentName()),
//                    Toast.LENGTH_SHORT).show();
    }
    private void handleBattery() {
        Date d = new Date();
        int pct = BatteryUtil.getPercentLevel(batteryStatus);
        String msg = "" + getDateString(d) + " : " + BatteryUtil.isCharging(batteryStatus) + " " + pct + "% " + "\n";

        //Log.d("battery", msg);

        String mainFileName = MainActivity.EXT_APP_LOG_DIR + "b-"+getDateDayString(d);
        try {
            IOUtil.saveExt(mainFileName, msg.getBytes(IOUtil.FILE_ENCODING), true);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    private void handleLocation() {
        Date d = new Date();
        String msg = "" + getDateString(d) + " : " + locationInfo.toString() + "\n";

        //Log.d("location", msg);

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
                    handleActivityTop();
                    break;
                case HANDLER_BATTERY:
                    handleBattery();
                    break;
                case HANDLER_LOCATION:
                    handleLocation();
                    break;
            }

        }
    };
}