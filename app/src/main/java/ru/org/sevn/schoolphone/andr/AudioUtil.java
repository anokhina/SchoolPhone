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
import android.media.AudioManager;

public class AudioUtil {
    public static int getSMSCallVol(Context ctx) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        return am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
    }
    public static void setSMSCallVol(Context ctx, int vol) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_NOTIFICATION,
                vol,
                0);
    }
    public static void setSMSCallVolume(Context ctx, int pct) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);

        int vol = 0;
        int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        if (pct == 100) {
            vol = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        } else if (pct == 0) {
            vol = 0;
        } else {
            vol = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) * pct / 100;
        }
        am.setStreamVolume(AudioManager.STREAM_NOTIFICATION,
                vol,
                0);
    }
}
