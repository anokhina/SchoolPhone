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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class CheckTopActivityService extends Service {
    private static Timer timer = new Timer();
    private Context ctx;
    public static final long TIMER_INTERVAL = 1000*30;

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        ctx = this;
        startService();
    }

    private void startService() {
        timer.scheduleAtFixedRate(new mainTask(), 0, TIMER_INTERVAL);
    }

    private class mainTask extends TimerTask {
        public void run() {
            handler.sendEmptyMessage(0);
        }
    }

    public void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            MainActivity mactivity = MainActivity.SELF;
            ActivityManager activityManager = (ActivityManager)ctx.getSystemService(Activity.ACTIVITY_SERVICE);
            AppDetail ad = null;
            if (activityManager != null && mactivity != null) {
                ComponentName cname = activityManager.getRunningTasks(1).get(0).topActivity;
                ad = new AppDetail(cname);
                if (mactivity.isProcess2kill(ad.getComponentName())) {
                    activityManager.moveTaskToFront(mactivity.getTaskId(), 0);
                }
            }
//            Toast.makeText(getApplicationContext(),
//                    "test->"+((ad == null) ? "" : ad.getComponentName()),
//                    Toast.LENGTH_SHORT).show();

        }
    };
}