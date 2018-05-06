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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
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

import android.support.v4.content.ContextCompat;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
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

import ru.org.sevn.schoolphone.andr.AlarmUtil;
import ru.org.sevn.schoolphone.andr.BatteryUtil;
import ru.org.sevn.schoolphone.andr.IOUtil;
import ru.org.sevn.schoolphone.andr.NetUtil;
import ru.org.sevn.schoolphone.connections.ConnectionUtil;
import ru.org.sevn.schoolphone.mail.MailSenderUtil;
import ru.org.sevn.schoolphone.connections.MobileDataUtil;

//https://www.google.com/maps/?q=55.753786,37.619988
//http://maps.google.com/?q=<lat>,<lng>
//geo:<lat>,<lng>?z=<zoom>

//https://maps.yandex.ru/?ll=37.619988,55.753786&spn=2.124481,0.671008&z=14&l=map&pt=37.619988,55.753786,pmrdm1
public class CheckTopActivityService extends Service {
    private Timer timer = new Timer();
    private AlarmUtil.SetAlarm alarmReceiver;
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

    public static final long TIME_MOBILE_DATA_OFF = 1000 * 60 * 14;
    public static final long TIME_MOBILE_DATA_ON = 1000 * 60 * 5;
    private LocationManager locationManager;
    private TelephonyManager telephonyManager;

    private PhoneStateListener telephonyListener = new PhoneStateListener(){
        public double convertQuartSecToDecDegrees(int quartSec) {
            //from CdmaCellLocation.convertQuartSecToDecDegrees
            if(Double.isNaN(quartSec) || quartSec < -2592000 || quartSec > 2592000){
                // Invalid value
                throw new IllegalArgumentException("Invalid coordiante value:" + quartSec);
            }
            return ((double)quartSec) / (3600 * 4);
        }
        @Override
        public void onCellLocationChanged(CellLocation location) {
            super.onCellLocationChanged(location);

            String networkOperator = telephonyManager.getNetworkOperator();
            int mcc = -1, mnc = -1;
            if (networkOperator != null) {
                try {
                    mcc = Integer.parseInt(networkOperator.substring(0, 3));
                    mnc = Integer.parseInt(networkOperator.substring(3));
                } catch (Exception e) {}
            }

            int cid = -1;
            int lac = -1;

            if (location != null) {
                if (location instanceof GsmCellLocation) {
                    GsmCellLocation gsmCellLocation = ((GsmCellLocation) location);
                    cid = gsmCellLocation.getCid();
                    lac = gsmCellLocation.getLac();
                    GSMLocation gsmLocation = new GSMLocation(mcc,mnc,lac,cid);
                    locationInfo.setGsmLocation(gsmLocation);
                    sendMessage(new Date(), "GSM LOCATION CHANGED");
                }
                else if (location instanceof CdmaCellLocation) {
                    CdmaCellLocation cdmaCellLocation = ((CdmaCellLocation) location);
                    cid = cdmaCellLocation.getBaseStationId();
                    lac = cdmaCellLocation.getSystemId();
                    int latitude = cdmaCellLocation.getBaseStationLatitude();
                    int longitude = cdmaCellLocation.getBaseStationLongitude();
                    if (latitude != Integer.MAX_VALUE && longitude != Integer.MAX_VALUE) {
                        Location cdmaLocation = new Location("CdmaCellLocation");
                        cdmaLocation.setLatitude(convertQuartSecToDecDegrees(latitude));
                        cdmaLocation.setLongitude(convertQuartSecToDecDegrees(longitude));
                        Location prevLocation = locationInfo.setCdmaLocation(cdmaLocation);
                        sendLocationMessage("CDMA LOCATION CHANGED", cdmaLocation, prevLocation);
                    }
                }
            }

            String cellBase = Integer.toString(lac)+"-"+Integer.toString(cid);

        }
    };

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
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        startService();
    }

    private Intent getBatteryStatus() {
        return ctx.getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    static class GSMLocation {
        private int mcc;
        private int mnc;
        private int lac;
        private int cid;

        public GSMLocation() {}
        public GSMLocation(int mcc, int mnc, int lac, int cid) {
            this.mcc = mcc;
            this.mnc = mnc;
            this.lac = lac;
            this.cid = cid;
        }

        public int getMcc() {
            return mcc;
        }

        public void setMcc(int mcc) {
            this.mcc = mcc;
        }

        public int getMnc() {
            return mnc;
        }

        public void setMnc(int mnc) {
            this.mnc = mnc;
        }

        public int getLac() {
            return lac;
        }

        public void setLac(int lac) {
            this.lac = lac;
        }

        public int getCid() {
            return cid;
        }

        public void setCid(int cid) {
            this.cid = cid;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject ret = new JSONObject();
            ret.put("mcc", getMcc());
            ret.put("mnc", getMnc());
            ret.put("cid", getCid());
            ret.put("lac", getLac());
            JSONArray arr = new JSONArray();
            arr.put("http://xinit.ru/bs/#!?mcc="+mcc+"&mnc="+mnc+"&lac="+lac+"&cid="+cid+"&networkType=gsm");
            arr.put("http://find-cell.mylnikov.org/#"+mcc+","+mnc+","+lac+","+cid);
            arr.put("http://api.mylnikov.org/mobile/main.py/get?data=open&mcc="+mcc+"&mnc="+mnc+"&cellid="+cid+"&lac="+lac+"&v=1.1");
            arr.put("http://lbs.ultrastar.ru/?mcc="+mcc+"&mnc="+mnc+"&lac="+lac+"&cid="+Integer.toString(cid, 16));
            arr.put("http://cellidfinder.com");
            ret.put("url", arr);

            //http://antex-e.ru/poleznye_materialy/18649/page/2

            return ret;
        }
    }
    static class LocationInfo {
        private final HashSet<String> enabledProviders = new HashSet<>();
        private Location location;
        private Location cdmaLocation;
        private GSMLocation gsmLocation;
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
        public Location getCdmaLocation() {
            return cdmaLocation;
        }

        public Location setCdmaLocation(Location location) {
            Location prevLocation = this.cdmaLocation;
            this.cdmaLocation = location;
            return prevLocation;
        }

        public GSMLocation getGsmLocation() {
            return gsmLocation;
        }

        public void setGsmLocation(GSMLocation gsmLocation) {
            this.gsmLocation = gsmLocation;
        }

        public HashMap<String, Integer> getStatuses() {
            return statuses;
        }

        private static JSONObject fromLocation(Location location) throws JSONException {
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
                return jlocation;
            }

            return null;
        }
        public JSONObject toJson() throws JSONException {
            JSONObject ret = new JSONObject();
            ret.put("enabledProviders", new JSONArray(enabledProviders));

            if (location != null) {
                ret.put("location", fromLocation(location));
            }
            if (cdmaLocation != null) {
                ret.put("cdmaLocation", fromLocation(cdmaLocation));
            }
            GSMLocation loc = gsmLocation;
            if (loc != null) {
                ret.put("gsmLocation", loc.toJson());
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
        private String callOut;
        private Date callOutDate;
        private Date callOutFinishedDate;
        private String profile;

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

        public String getCallOut() {
            return callOut;
        }

        public synchronized void setCallOut(String callOut) {
            this.callOutFinishedDate = null;
            this.callOutDate = new Date();
            this.callOut = callOut;
        }

        public Date getCallOutDate() {
            return callOutDate;
        }

        public synchronized boolean setCallOutDateNow() {
            if(callOutDate == null) {
                this.callOutDate = new Date();
                return true;
            }
            return false;
        }

        public Date getCallOutFinishedDate() {
            return callOutFinishedDate;
        }

        public void setCallOutFinishedDate(Date callOutFinishedDate) {
            this.callOutFinishedDate = callOutFinishedDate;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject ret = new JSONObject();
            ret.put("battery", battery);
            if (date != null) {
                ret.put("dateString", getDateString(date.getTime()));
                ret.put("date", date.getTime());
            }
            if (profile != null) {
                ret.put("profile", profile);
            }
            if (callOut != null) {
                ret.put("callOut", callOut);
                if (callOutDate != null) {
                    ret.put("callOutDate", callOutDate.getTime());
                    ret.put("callOutDateString", getDateString(callOutDate.getTime()));
                }
                if (callOutFinishedDate != null) {
                    ret.put("callOutFinishedDate", callOutFinishedDate.getTime());
                    ret.put("callOutFinishedDateString", getDateString(callOutFinishedDate.getTime()));
                }
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

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }
    }
    private State state = new State();
    private Intent updateStatus() {
        Intent batteryStatus = getBatteryStatus();
        Date d = new Date();

        LauncherFragment.LauncherAdapter mactivity = getActivityAdapter();
        state.setDate(d);
        state.setBattery(BatteryUtil.getPercentLevel(batteryStatus));
        state.setProfile(mactivity.getProfile());
        return batteryStatus;
    }
    private void sendBatteryMessage() {
        Intent batteryStatus = updateStatus();
        int pct = BatteryUtil.getPercentLevel(batteryStatus);
        String msg = "" + getDateString(state.getDate()) + " : " + BatteryUtil.isCharging(batteryStatus) + " " + pct + "% " + "\n\n";

        //Log.d("battery", msg);
        handler.sendMessage(handler.obtainMessage(HANDLER_BATTERY, new Object[] {state.getDate(), msg} ));
        if (pct < LOW_BATTERY_PCT) {
            sendMessage(state.getDate(), "LOW BATTERY");
        }
    }
    private void sendMessage(Date d, String cause) {
        sendMessage(d, cause, null);
    }
    private void sendMessage(Date d, String cause, String sms) {
        handler.sendMessage(
                handler.obtainMessage(
                        HANDLER_STATE,
                        new Object[] {d, getStateMessage(cause), getStateMessageSubject(cause), sms} ));

    }
    private String getStateMessageSubject(String cause) {
        String locationLnk = "";
        {
            Location l = locationInfo.getLocation();
            if (l != null) {
                locationLnk += (l.getLatitude() + "," + l.getLongitude()+" ");
            }
        }
        {
            Location l = locationInfo.getCdmaLocation();
            if (l != null) {
                locationLnk += (l.getLatitude() + "," + l.getLongitude());
            }
        }
        return cause + ": " + state.getBattery() + "% " + locationLnk + "\n";
    }
    private String getShortStateMessage(String cause) {
        String locationLnk = "";
        {
            Location l = locationInfo.getLocation();
            if (l != null) {
                locationLnk += (l.getLatitude() + "," + l.getLongitude() + " ");
            }
        }
        {
            Location l = locationInfo.getCdmaLocation();
            if (l != null) {
                locationLnk += (l.getLatitude() + "," + l.getLongitude() + " ");
            }
        }
        {
            GSMLocation l = locationInfo.getGsmLocation();
            if (l != null) {
                locationLnk += ("cid="+l.getCid()+",");
                locationLnk += ("lac="+l.getLac()+",");
                locationLnk += ("mcc="+l.getMcc()+",");
                locationLnk += ("mnc="+l.getMnc());
            }
        }
        return cause + ": " + state.getBattery() + "% " + locationLnk + "\n";
    }
    private String getStateMessage(String cause) {
        String locationLnk = "";
        try {
            JSONObject jsonLocationDsc = locationInfo.toJson();
            if (jsonLocationDsc.has("location")) {
                JSONObject jsonLocation = jsonLocationDsc.getJSONObject("location");
                locationLnk += "location\n";
                if (jsonLocation.has("googleUrl")) {
                    String url = jsonLocation.optString("googleUrl");
                    locationLnk += (" " + url + "\n");
                }
                if (jsonLocation.has("yandexUrl")) {
                    String url = jsonLocation.optString("yandexUrl");
                    locationLnk += (" " + url + "\n");
                }
            }
            if (jsonLocationDsc.has("cdmaLocation")) {
                locationLnk += "cdmaLocation";
                JSONObject jsonLocation = jsonLocationDsc.getJSONObject("cdmalocation");
                if (jsonLocation.has("googleUrl")) {
                    String url = jsonLocation.optString("googleUrl");
                    locationLnk += (" " + url + "\n");
                }
                if (jsonLocation.has("yandexUrl")) {
                    String url = jsonLocation.optString("yandexUrl");
                    locationLnk += (" " + url + "\n");
                }
            }
            if (jsonLocationDsc.has("gsmLocation")) {
                locationLnk += "gsmLocation";
                JSONObject jsonLocation = jsonLocationDsc.getJSONObject("gsmLocation");
                if (jsonLocation.has("url")) {
                    JSONArray urlArr = jsonLocation.optJSONArray("url");
                    if (urlArr != null) {
                        for (int i = 0; i < urlArr.length(); i++) {
                            Object url = urlArr.get(i);
                            locationLnk += (" " + url + "\n");
                        }
                    }
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
                    updateStatus();
                    sendMessage(new Date(), "EMERGENCY CALL IN");
                } else if (AppConstants.ACTION_SCHEDULE.equals(intent.getAction())) {
                    updateStatus();
                    String state = intent.getStringExtra(AppConstants.ACTION_SCHEDULE_STATE);
                    sendMessage(new Date(), "SCHEDULE " + state);
                } else if (AppConstants.ACTION_CALL_OUT.equals(intent.getAction())) {
                    String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                    state.setCallOut(phoneNumber);
                } else if (AppConstants.ACTION_CALL_IDLE.equals(intent.getAction())) {
                    updateStatus();
                    if (state.setCallOutDateNow()) {
                        sendMessage(new Date(), "CALL OUT");
                    }
                } else if (AppConstants.ACTION_SOS.equals(intent.getAction())) {
                    updateStatus();
                    sendMessage(new Date(), "SOS", getShortStateMessage("SOS"));
                } else if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                    showLauncher();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConstants.ACTION_EMERGENCY_CALL_IN);
        intentFilter.addAction(AppConstants.ACTION_SCHEDULE);
        intentFilter.addAction(AppConstants.ACTION_CALL_OUT);
        intentFilter.addAction(AppConstants.ACTION_CALL_IDLE);
        intentFilter.addAction(AppConstants.ACTION_SOS);
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        //intentFilter.addAction("another action");
        this.registerReceiver( broadcastReceiver, intentFilter );

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                //handler.sendEmptyMessage(HANDLER);
                handleActivityTop();
                checkInternetAccess();
            }
        }, 0, TIMER_INTERVAL);
        alarmReceiver = AlarmUtil.setAlarm(this,
                AlarmManager.RTC,
                "ru.org.sevn.schoolphone.battery",
                new AlarmUtil.AlarmReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent, BroadcastReceiver receiver) {
                        sendBatteryMessage();
                        toggleInternet();
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
                sendLocationMessage("LOCATION CHANGED", location, prevLocation);
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
        telephonyManager.listen(telephonyListener, PhoneStateListener.LISTEN_CELL_LOCATION);
    }

    private void checkInternetAccess() {
        if ("true".equals(PersonalConstants.get(AppConstants.MOBILE_DATA))) {

        } else {
            forbidInternet();
        }
    }

    private void sendLocationMessage(String cause, Location location, Location prevLocation) {
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
            sendMessage(new Date(), cause);
        }
    }

    public void onDestroy() {
        getContentResolver().unregisterContentObserver(contentObserverMobileData);
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        if (locationManager != null && locationListener != null) locationManager.removeUpdates(locationListener);
        if (telephonyManager != null && telephonyListener != null) telephonyManager.listen(telephonyListener, PhoneStateListener.LISTEN_NONE);;
        if (timer != null) timer.cancel();
        if (alarmReceiver != null) {
            AlarmUtil.unset(this, alarmReceiver);
        }
        super.onDestroy();
    }

    private LauncherFragment.LauncherAdapter getActivityAdapter() {
        LauncherFragment.LauncherAdapter mactivity = null;
        LauncherFragment launcherFragment = PersonalConstants.getAppInstance();
        if (launcherFragment != null) { //TODO
            mactivity = launcherFragment.getLadapter();
        }
        return mactivity;
    }

    private void showLauncher() {
        LauncherFragment.LauncherAdapter mactivity = getActivityAdapter();
        ActivityManager activityManager = (ActivityManager)ctx.getSystemService(Activity.ACTIVITY_SERVICE);
        if (mactivity != null) {
            activityManager.moveTaskToFront(mactivity.getActivity().getTaskId(), 0);
        }
    }

    public static boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
    }
    private boolean isCallIntenet(String s) {
        //Log.d("isCallIntenet>", s);
        if ("com.android.phone/com.android.phone.InCallScreen".equals(s)) {
            return true;
        }
        return false;
    }
    private void handleActivityTop() {
        LauncherFragment.LauncherAdapter mactivity = getActivityAdapter();
        ActivityManager activityManager = (ActivityManager)ctx.getSystemService(Activity.ACTIVITY_SERVICE);
        AppDetail ad = null;
        if (activityManager != null && mactivity != null) {
            ComponentName cname = activityManager.getRunningTasks(1).get(0).topActivity;
            ad = new AppDetail(cname);
            if (mactivity.isProcess2kill(ad.getComponentName()) && !mactivity.isSU()) {
                //LauncherFragment.showLauncher();
                activityManager.moveTaskToFront(mactivity.getActivity().getTaskId(), 0);
            } else if (isBluetoothHeadsetConnected() && isCallIntenet(ad.getComponentName())) {
                activityManager.moveTaskToFront(mactivity.getActivity().getTaskId(), 0);
                final Activity act = mactivity.getActivity();
                if (act instanceof MainActivity) {
                    act.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            ((MainActivity)act).openTab(0);
                        }
                    });
                }
            } else {
                if (mactivity.isProcessInKilled(ad.getComponentName())) {
                    forbidInternet();
                }
            }
        }
//            Toast.makeText(getApplicationContext(),
//                    "test->"+((ad == null) ? "" : ad.getComponentName()),
//                    Toast.LENGTH_SHORT).show();
    }
    private void handleBattery(Date d, String msg) {
        String mainFileName = LauncherFragment.LauncherAdapter.EXT_APP_LOG_DIR + "b-"+getDateDayString(d);
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
                if (params.length > 3) {
                    sendState(params[0], params[1], params[2], params[3]);
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
        private void sendState(final String msg, final String locationInfoStr, final String extraSubj, final String message) {
            if (!ConnectionUtil.isAirplaneModeOn(ctx)) {
                boolean isOnLine = MailSenderUtil.isOnline(ctx);
                int connected = -1;
                if (!isOnLine) {
                    connected = tryConnect(true, true);
                }
                if (MailSenderUtil.isOnline(ctx)) {
                    String ipAddr = NetUtil.getIPAddress(true);
                    if (ipAddr == null) {
                        ipAddr = NetUtil.getIPAddress(!true);
                    }
                    if (ipAddr == null) {
                        ipAddr = "";
                    }

                    String fullMsg = msg + locationInfoStr;
                    try {
                        String mailId = MailSenderUtil.sendMail(
                                PersonalConstants.get(AppConstants.MAIL_FROM_USER),
                                PersonalConstants.get(AppConstants.MAIL_FROM_PSWD),
                                PersonalConstants.get(AppConstants.MAIL_FROM),
                                PersonalConstants.get(AppConstants.MAIL_TO),
                                "school_phone_state " + PersonalConstants.get(AppConstants.CLIENT_ID) + " " + ipAddr + " " + extraSubj,
                                fullMsg
                        );
                    } catch (Exception e) {
                        //TODO last error
                        //send sms
                        if (message != null) {
                            SmsManager sms = SmsManager.getDefault();
                            sms.sendTextMessage(PersonalConstants.get(AppConstants.ADMIN_PHONE), null, message, null, null);
                        }
                    }
                    //System.err.println("======"+mailId);
                } else {
                    //send sms
                    if (message != null) {
                        SmsManager sms = SmsManager.getDefault();
                        sms.sendTextMessage(PersonalConstants.get(AppConstants.ADMIN_PHONE), null, message, null, null);
                    }
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
        String mainFileName = LauncherFragment.LauncherAdapter.EXT_APP_LOG_DIR + "l-"+getDateDayString(d);
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
                        if (arr.length > 3) {
                            new SendStateTask(ctx).execute(
                                    getDateString((Date) arr[0]) + " ",
                                    (String) arr[1],
                                    (String) arr[2],
                                    (String) arr[3]
                            );
                        } else
                        if (arr.length > 2) {
                            new SendStateTask(ctx).execute(
                                    getDateString((Date) arr[0]) + " ",
                                    (String) arr[1],
                                    (String) arr[2],
                                    null
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

    private Date lastConnect;
    private void forbidInternet() {
        int mobileDataEnabled = MobileDataUtil.isMobileDataEnabled(ctx);
        if (mobileDataEnabled > 0) {
            MobileDataUtil.setMobileDataEnabled(ctx, false);
        }
        if (lastConnect != null) {
            lastConnect = new Date();
        }
    }
    private void toggleInternet() {
        if (!ConnectionUtil.isAirplaneModeOn(ctx) && "true".equals(PersonalConstants.get(AppConstants.MOBILE_DATA))) {
            boolean isOnLine = MailSenderUtil.isOnline(ctx);
            int mobileDataEnabled = MobileDataUtil.isMobileDataEnabled(ctx);

            if (lastConnect == null) {
                if (isOnLine || mobileDataEnabled > 0) {
                } else {
                    int stat = MobileDataUtil.setMobileDataEnabled(ctx, true);
                    if (stat > 0) {
                        lastConnect = new Date();
                    }
                }
            } else {
                Date now = new Date();
                long dtime = now.getTime() - lastConnect.getTime();
                if (mobileDataEnabled > 0) {
                    if (dtime > TIME_MOBILE_DATA_ON) {
                        MobileDataUtil.setMobileDataEnabled(ctx, false);
                    }
                } else {
                    if (dtime > TIME_MOBILE_DATA_OFF) {
                        lastConnect = null;
                    }
                }
            }
        }
    }
}