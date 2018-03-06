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
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.AsyncTask;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class MediaUtil {
    public static ToneGenerator beep(final Callable<Boolean> callable) {
        final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); //volume
        //toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150);//length
        //toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    while (callable.call()) {
                        try {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
                            try {
                                TimeUnit.SECONDS.sleep(2);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception ex) {}
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return toneGenerator;
    }
    public static void beepPlay(Context ctx) {
        try {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (soundUri != null) {
                Ringtone r = RingtoneManager.getRingtone(ctx, soundUri);
                r.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static int STREAM_ALARM = AudioManager.STREAM_MUSIC;
    public static int getVolumeAlarm(Context ctx) {
        int streamType = STREAM_ALARM;
        final AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(streamType);
    }
    public static void setVolumeAlarm(Context ctx, int volume) {
        try {
            int streamType = STREAM_ALARM;
            final AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            int maxVal = audioManager.getStreamMaxVolume(streamType);
            if (volume < 0) volume = 0;
            else if (volume > maxVal) volume = maxVal;
            audioManager.setStreamVolume(streamType, volume, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static MediaPlayer playLoopAlarm(Context ctx, int volumePct) {
        Uri soundUri = null;
        try {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("===soundUri="+soundUri);
        if (soundUri != null) {
            try {
                final MediaPlayer mp = MediaPlayer.create(ctx, soundUri);
                int streamType = STREAM_ALARM;
                mp.setAudioStreamType(streamType);
                mp.setLooping(true);
                final AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                if (volumePct > 0) {
                    if (volumePct > 100) volumePct = 100;
                    try {
                        int maxVal = audioManager.getStreamMaxVolume(streamType);
                        audioManager.setStreamVolume(streamType, volumePct * maxVal / 100, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (audioManager.getStreamVolume(streamType) != 0) {
                    mp.prepare();
                    mp.start();
                    return mp;
                } else {
                    mp.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static void beepMP(Context ctx) {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (soundUri != null) {
            final MediaPlayer mp = MediaPlayer.create(ctx, soundUri);
            int streamType = STREAM_ALARM;//AudioManager.STREAM_NOTIFICATION
            mp.setAudioStreamType(streamType);
//            try {
//                AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
//                mp.setVolume(
//                        (float) (audioManager.getStreamVolume(streamType) / 7.0),
//                        (float) (audioManager.getStreamVolume(streamType) / 7.0));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            mp.start();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {

                    mp.stop();
                    mp.release();
                }
            });
        }
    }
}
