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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.util.Collection;
import java.util.List;

import ru.org.sevn.schoolphone.AppDetail;
import ru.org.sevn.schoolphone.MainActivity;

public class ActivityManagerUtil {
    //<uses-permission android:name="android.permission.REORDER_TASKS" />
    public static void moveApp2Front(final Activity ctx) {
        ActivityManager activityManager = (ActivityManager) ctx.getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        activityManager.moveTaskToFront(ctx.getTaskId(), 0);
    }
    public static ComponentName getForegroundActivity(final Context ctx) {
        ActivityManager activityManager = (ActivityManager)ctx.getSystemService(Activity.ACTIVITY_SERVICE);
        return activityManager.getRunningTasks(1).get(0).topActivity;
    }
    public static void killProcesses(final Context ctx, Collection<String> packageNames2kill) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Activity.ACTIVITY_SERVICE);
        if (manager != null) {
            List<ActivityManager.RunningAppProcessInfo> listOfProcesses = manager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo process : listOfProcesses) {
                Log.e("Proccess", process.processName + " : " + process.pid);
                String packageName = process.processName.split(":")[0];
                if (packageNames2kill.contains(packageName)) {
                    //manager.restartPackage(packageName);
//                android.os.Process.killProcess(process.pid);
//                android.os.Process.sendSignal(process.pid, android.os.Process.SIGNAL_KILL);
                    try {
                        manager.killBackgroundProcesses(packageName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        ToastUtil.anyToast(ctx, "Can't stop process:" + process.processName);
                    }
                }
            }
        }
        //manager.killBackgroundProcesses(packageName);
        //Process.sendSignal(pid, Process.SIGNAL_KILL);
        /*
    ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
    if(am != null) {
        List<ActivityManager.AppTask> tasks = am.getAppTasks();
        if (tasks != null && tasks.size() > 0) {
            tasks.get(0).setExcludeFromRecents(true);
        }
    }

<uses-permission android:name="android.permission.FORCE_STOP_PACKAGES"></uses-permission>
    ActivityManager am = (ActivityManager) getApplicationContext().getSystemService("activity");
    Method forceStopPackage;
    forceStopPackage =am.getClass().getDeclaredMethod("forceStopPackage",String.class);
    forceStopPackage.setAccessible(true);
    forceStopPackage.invoke(am, pkg);
         */
    }

    private void recentTasks(final Context ctx) {
        ActivityManager mgr = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RecentTaskInfo> recents = mgr.getRecentTasks(1000, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
        for( int i=1; i < recents.size(); i++ ) {
            ActivityManager.RecentTaskInfo ti = recents.get(i);
            System.err.println("++++++++???+" + ti.baseIntent.getComponent().getPackageName() + ":" + ti.baseIntent.getComponent().getClassName());
            AppDetail ad = new AppDetail(ti.baseIntent.getComponent().getPackageName(), ti.baseIntent.getComponent().getClassName());
        }
    }

}
