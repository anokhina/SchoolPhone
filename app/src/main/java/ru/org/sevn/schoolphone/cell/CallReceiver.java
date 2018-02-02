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

package ru.org.sevn.schoolphone.cell;

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.lang.reflect.Method;

import ru.org.sevn.schoolphone.AppBroadcastReceiver;
import ru.org.sevn.schoolphone.AppConstants;
import ru.org.sevn.schoolphone.MainActivity;
import ru.org.sevn.schoolphone.PersonalConstants;
import ru.org.sevn.schoolphone.andr.AudioUtil;
import ru.org.sevn.schoolphone.andr.ToastUtil;

import static ru.org.sevn.schoolphone.AppConstants.ADMIN_PHONE;

public class CallReceiver extends AppBroadcastReceiver {
    private boolean endCallByTMReflection(Context ctx) {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class c = Class.forName(tm.getClass().getName());
            //getITelephonyMSim
            //getITelephony
            Method m = null;
            try {
                m = c.getDeclaredMethod("getITelephony");
                m.setAccessible(true);
                Object telephonyService = m.invoke(tm);
                Method mcall = telephonyService.getClass().getMethod("endCall");
                return (boolean)mcall.invoke(telephonyService);
            } catch (Exception ex) {
                m = c.getDeclaredMethod("getITelephonyMSim");
                m.setAccessible(true);
                Object telephonyService = m.invoke(tm);
                Method mcall = telephonyService.getClass().getMethod("endCall", int.class);
                Method mgetPreferredDataSubscription = telephonyService.getClass().getMethod("getPreferredDataSubscription");
                int csub = (int)mgetPreferredDataSubscription.invoke(telephonyService);
                boolean ret = (boolean)mcall.invoke(telephonyService,csub);
                if (ret) return ret;
                ret = (boolean)mcall.invoke(telephonyService,csub + 1);
                return ret;
            }
            //telephonyService.silenceRinger();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void endCall(Context ctx) {
        try {
            if (!endCallByTMReflection(ctx)) {
                setResultData(null);
            } else {
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent){
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        String phoneKey = TelephonyManager.EXTRA_INCOMING_NUMBER;
        String phoneNumber = intent.getStringExtra(phoneKey);


        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
//            String phoneKey = Intent.EXTRA_PHONE_NUMBER;
//            String phoneNumber = intent.getStringExtra(phoneKey);
        } else
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            if (isAdminCallProfileAll(phoneNumber)) {
                getMainActivity().setProfileTo(MainActivity.PROFILE_ALL, true);
            } else
            if (isAdminPhone(phoneNumber)) {
                getMainActivity().setProfileTo(MainActivity.PROFILE_SCHOOL, true);
            }

            if (isEmergencyCall(phoneNumber)) {
                getMainActivity().saveSettings();
                AudioUtil.setSMSCallVolume(context, 100);
            }
        } else
        if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            getMainActivity().restoreSettings();
        } else
        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            getMainActivity().restoreSettings();
        }

        //int id = getCallNum(phoneNumber);
        //System.err.println("+++++++++++"+state+":"+phoneNumber);
        //new AppNotifier().showCallNotify(id, context,"Call", ""+state+":"+msg);
//        if (msg != null) {
//            toast(context, msg, Toast.LENGTH_LONG);
//        }
    }

    private int getCallNum(String phoneNumber) {
        int ret = -1;
        if (phoneNumber != null) {
            ret = phoneNumber.hashCode();
            if (ret > 0) ret *= -1;
        }
        return ret;
    }

    private void toast(Context context, String msg, int length) {
        try {
            Toast.makeText(context, msg, length).show();
        } catch (Exception e) {
            ToastUtil.anyToast(context, msg, length);
        }
    }
    private boolean isEmergencyCall(String ph) {
        String adminPhone = PersonalConstants.get(AppConstants.EMERGENCY_PHONE);
        if (ph != null && adminPhone != null && adminPhone.contains(","+ph.trim()+",")) {
            return true;
        }
        return false;
    }

    private boolean isAdminPhone(String ph) {
        String adminPhone = PersonalConstants.get(ADMIN_PHONE);
        if (ph != null && adminPhone != null && adminPhone.equals(ph)) {
            return true;
        }
        return false;
    }

    private boolean isAdminCallProfileAll(String ph) {
        String adminPhone = PersonalConstants.get(AppConstants.ADMIN_PHONE_PROFILE_ALL);
        if (ph != null && adminPhone != null && adminPhone.equals(ph)) {
            return true;
        }
        return false;
    }

}