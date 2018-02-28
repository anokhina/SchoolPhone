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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

import ru.org.sevn.schoolphone.FakeActivity;

public class AndrUtil {
    public static Intent getAppIntent(final Context ctx, final String packageName) {
        if (packageName == null) return null;
        PackageManager pm = ctx.getPackageManager();
        Intent i = pm.getLaunchIntentForPackage(packageName);
        if (i == null) {
            Intent intent = new Intent();
            intent.setPackage(packageName);

            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
            Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(pm));

            if(resolveInfos.size() > 0) {
                ResolveInfo launchable = resolveInfos.get(0);
                ActivityInfo activity = launchable.activityInfo;
                ComponentName name=new ComponentName(activity.applicationInfo.packageName, activity.name);
                i.setComponent(name);
            }
        }
        return i;
    }
    public static void setIntentFlags(Intent i) {
        i=new Intent(Intent.ACTION_MAIN);

        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }
    public static Intent runApp(final Context ctx, final String packageName) {
        return runApp(ctx, packageName, null);
    }
    public static Intent runApp(final Context ctx, final String packageName, final String name) {
        Intent i = getIntent2Start(ctx, packageName, name);
        if (i != null) {
            setIntentFlags(i);
            ctx.startActivity(i);
        }
        return i;
    }
    public static Intent getIntent2Start(final Context ctx, final String packageName, final String name) {
        Intent i = getAppIntent(ctx, packageName);
        if (name != null) {
            ComponentName componentName = new ComponentName(packageName, name);
            i.setComponent(componentName);
        }
        return i;
    }
    public static void copy(Collection dest, Collection src) {
        for(Object o : src) {
            dest.add(o);
        }
    }

    public static void resetPreferredLauncherAndOpenChooser(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, FakeActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        Intent selector = new Intent(Intent.ACTION_MAIN);
        selector.addCategory(Intent.CATEGORY_HOME);
        selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(selector);

        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
    }

    public interface CanAdd<T> {
        boolean canAdd(T o);
    }

    public static void copy(Collection dest, Collection src, CanAdd canAdd) {
        for(Object o : src) {
            if (canAdd.canAdd(o)) {
                dest.add(o);
            }
        }
    }

    public static Resources getAppResources(Context ctx, String packageName) {
        try {
            return ctx.getPackageManager().getResourcesForApplication(packageName);
        } catch (NameNotFoundException e) {
            return null;
        }
    }
    public static Drawable getDrawableForDensityMax(Resources resources, int iconid) {
        if (resources == null) return null;
        try {
            //resources.getDisplayMetrics().densityDpi
            return resources.getDrawableForDensity(iconid, DisplayMetrics.DENSITY_XHIGH);
        } catch (Exception e) {
            return resources.getDrawable(iconid);
        }
    }
    public static Drawable getIcon(PackageManager manager, PackageItemInfo pii) {
        //return pii.loadIcon(manager);
        Drawable appIcon = null;
        try {
            Resources resourcesForApplication = manager.getResourcesForApplication(pii.packageName);

            appIcon = getDrawableForDensityMax(resourcesForApplication, DisplayMetrics.DENSITY_XHIGH);
            /*
            Configuration config = resourcesForApplication.getConfiguration();
            Configuration originalConfig = new Configuration(config);

            DisplayMetrics displayMetrics = resourcesForApplication.getDisplayMetrics();
            DisplayMetrics originalDisplayMetrics = resourcesForApplication.getDisplayMetrics();
            displayMetrics.densityDpi = DisplayMetrics.DENSITY_DEFAULT;
            resourcesForApplication.updateConfiguration(config, displayMetrics);

            appIcon = resourcesForApplication.getDrawable(pii.icon);
            resourcesForApplication.updateConfiguration(originalConfig, originalDisplayMetrics);
            */
        } catch (Exception e) {
            Log.e("check", "error getting Hi Res Icon :", e);
        }
        if (appIcon == null) {
            appIcon = pii.loadIcon(manager);
        }
        return appIcon;
    }

    public static List<ResolveInfo> getAvailableActivities(final Context ctx) {
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager manager = ctx.getPackageManager();
        return manager.queryIntentActivities(i, 0);
    }
    public static List<ResolveInfo> getAvailableReferences(final Context ctx) {
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        PackageManager manager = ctx.getPackageManager();
        return manager.queryIntentActivities(i, 0);
    }
    public static List<AppWidgetProviderInfo> getInstalledAppWidgetList(Context ctx) {
        AppWidgetManager manager;
        manager = AppWidgetManager.getInstance(ctx);
        return manager.getInstalledProviders();
    }

    public static String getDefaultHome(Context ctx) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = ctx.getPackageManager().resolveActivity(intent, 0);
        if (res.activityInfo == null) {
        } else if ("android".equals(res.activityInfo.packageName)) {
            // No default selected
            return res.activityInfo.packageName;
        } else {
            // res.activityInfo.packageName and res.activityInfo.name gives you the default app
            return res.activityInfo.packageName;
        }
        //com.android.internal.app.ResolverActivity
        return null;
    }

}
