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
        int vol = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        //System.err.println("+++++++++>" + vol);
        return vol;
    }
    public static void setSMSCallVol(Context ctx, int vol) {
        setVol(ctx, vol, AudioManager.STREAM_NOTIFICATION);
    }
    public static void setVol(Context ctx, int vol, int streamType) {
        //System.err.println("+++++++++>>" + vol);

        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        int currentVol = am.getStreamVolume(streamType);
        if (currentVol != vol) {
            am.setStreamVolume(streamType, vol, 0);
        }
    }
    public static void setSMSCallVolume(Context ctx, int pct) {
        setVolume(ctx, pct, AudioManager.STREAM_NOTIFICATION);
    }
    public static int getVolumeFromPctSMSCall(Context ctx, int pct) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        return getVolumeFromPct(pct, am, AudioManager.STREAM_NOTIFICATION);
    }
    public static int getVolumeFromPct(Context ctx, int pct, int streamType) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        return getVolumeFromPct(pct, am, streamType);
    }
    public static int getVolumeFromPct(int pct, AudioManager am, int streamType) {
        int vol = 0;
        int maxVol = am.getStreamMaxVolume(streamType);
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        if (pct == 100) {
            vol = am.getStreamMaxVolume(streamType);
        } else if (pct == 0) {
            vol = 0;
        } else {
            vol = maxVol * pct / 100;
        }
        return vol;
    }
    public static void setVolume(Context ctx, int pct, int streamType) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        int currentVol = am.getStreamVolume(streamType);

        int vol = getVolumeFromPct(pct, am, streamType);
        //System.err.println("++++>>>>>>>"+vol+":"+maxVol);
        if (currentVol != vol) {
            am.setStreamVolume(streamType, vol, 0);
        }
    }
}
