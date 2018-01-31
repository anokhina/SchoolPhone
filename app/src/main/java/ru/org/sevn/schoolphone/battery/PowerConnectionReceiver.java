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
package ru.org.sevn.schoolphone.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.org.sevn.schoolphone.AppNotifier;
import ru.org.sevn.schoolphone.andr.BatteryUtil;

public class PowerConnectionReceiver extends BroadcastReceiver {

    public static final String TITLE_NOTIFY = "Battery power";
    
    public PowerConnectionReceiver() {
        
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
            batteryPowerDisConnected(context, intent);
        } else if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
            batteryPowerConnected(context, intent);
        } else if (intent.getAction().equals(Intent.ACTION_BATTERY_OKAY)) {
            batteryOkay(context, intent);
        } else if (intent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
            batteryLow(context, intent);
        }
    }

    public static void batteryChanged(Context context) {
        int pct = BatteryUtil.getPercentLevel(context);
        new AppNotifier().batteryPowerNotify(context, TITLE_NOTIFY, "" + pct + "%");
    }

    private void batteryPowerDisConnected(Context context, Intent intent) {
        int pct = BatteryUtil.getPercentLevel(context);
        new AppNotifier().batteryPowerNotify(context, TITLE_NOTIFY, "Disconnected " + pct + "%");
    }
    private void batteryPowerConnected(Context context, Intent intent) {
        int pct = BatteryUtil.getPercentLevel(context);
    	new AppNotifier().batteryPowerNotify(context, TITLE_NOTIFY, "Connected " + pct + "%");
    }
    private void batteryOkay(Context context, Intent intent) {
//        notify(context, TITLE_NOTIFY, "batteryOkay");
    }
    private void batteryLow(Context context, Intent intent) {
//        notify(context, TITLE_NOTIFY, "batteryLow");
    }
    
}