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
import android.os.Bundle;
import android.telephony.SmsMessage;

import ru.org.sevn.schoolphone.AppBroadcastReceiver;
import ru.org.sevn.schoolphone.MainActivity;
import ru.org.sevn.schoolphone.PersonalConstants;
import ru.org.sevn.schoolphone.andr.AudioUtil;

import static ru.org.sevn.schoolphone.AppConstants.ADMIN_PHONE;

public class SMSReceiver extends AppBroadcastReceiver {
    private static final String TAG = "SMSReceiver";
    private static final String PROFILE_MSG = "profile=";
    private static final String VOLUME_MSG = "volume=";
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus.length == 0) {
            return;
        }
        SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdus[0]);
        String msgBody = msg.getMessageBody();
        //Log.i(TAG, msgBody);
        //Log.i(TAG, msg.getOriginatingAddress());
        if (isAdminPhone((msg.getOriginatingAddress()))) {
            MainActivity activity = getMainActivity(); //TODO it
            int idxProf = msgBody.indexOf(PROFILE_MSG);
            if (idxProf >= 0) {
                int nextDelim = msgBody.indexOf(",", idxProf);
                if (nextDelim < 0) {
                    nextDelim = msgBody.length();
                }
                String profile = msgBody.substring(idxProf + PROFILE_MSG.length(), nextDelim).trim();
                if (activity != null) {
                    activity.setProfileTo(profile, true, -1);
                }
                noForw();
            }
            int idxVol = msgBody.indexOf(VOLUME_MSG);
            if (idxVol >= 0) {
                int nextDelim = msgBody.indexOf(",", idxVol);
                if (nextDelim < 0) {
                    nextDelim = msgBody.length();
                }
                String volume = msgBody.substring(idxVol + VOLUME_MSG.length(), nextDelim).trim();
                try {
                    int volPct = Integer.parseInt(volume);
                    if (activity != null) {
                        AudioUtil.setSMSCallVolume(activity, volPct);
                    }
                    noForw();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void noForw() {
        try {
            abortBroadcast();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isAdminPhone(String ph) {
        String adminPhone = PersonalConstants.get(ADMIN_PHONE);
        if (adminPhone != null && adminPhone.equals(ph)) {
            return true;
        }
        return false;
    }
}