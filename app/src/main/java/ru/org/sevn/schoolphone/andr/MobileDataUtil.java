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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MobileDataUtil {
    public static boolean getMobileDataEnabled(Context context) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {

        final ConnectivityManager conman = (ConnectivityManager)  context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Field connectivityManagerField = ConnectivityManager.class.getDeclaredField("mService");
        connectivityManagerField.setAccessible(true);
        final Object connectivityManager = connectivityManagerField.get(conman);
        final Method setMobileDataEnabledMethod = connectivityManager.getClass().getDeclaredMethod("getMobileDataEnabled");
        setMobileDataEnabledMethod.setAccessible(true);

        Boolean ret = (Boolean)setMobileDataEnabledMethod.invoke(connectivityManager);
        return ret;
    }
    public static void setMobileDataEnabled(Context context, boolean enabled) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        final ConnectivityManager conman = (ConnectivityManager)  context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Field connectivityManagerField = ConnectivityManager.class.getDeclaredField("mService");
        connectivityManagerField.setAccessible(true);
        final Object connectivityManager = connectivityManagerField.get(conman);
        final Method setMobileDataEnabledMethod = connectivityManager.getClass().getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
        setMobileDataEnabledMethod.setAccessible(true);

        setMobileDataEnabledMethod.invoke(connectivityManager, enabled);
    }
    public static void setWifiEnabled(Context context, boolean isOn) {
    	try {
	    	WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	    	wifi.setWifiEnabled(isOn);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    public static boolean isWifiEnabled(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifi.isWifiEnabled();
    }
    public static boolean isWifiConnected(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifi.getConnectionInfo();
        if (wifiInfo != null) {
            return (wifiInfo.getNetworkId() >= 0);
        }
        return false;
    }
    public static NetworkInfo getActiveNetworkInfo(Context context) {
        ConnectivityManager connec = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connec.getActiveNetworkInfo();
        //netInfo.getTypeName()+":"+netInfo.getState()
        return netInfo;
    }
}
