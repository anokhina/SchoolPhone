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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.org.sevn.schoolphone.andr.AndrUtil;
import ru.org.sevn.schoolphone.andr.AndrUtilGUI;

import android.app.Activity;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.TextView;

public class AppDetailManager {
    public static List<AppDetail> filter(final List<AppDetail> lst, final String packageNameLowercase) {
        ArrayList<AppDetail> ret = new ArrayList<>();
        for (AppDetail ad : lst) {
            if (packageNameLowercase != null && ad.getPackageName() != null && !ad.getPackageName().toLowerCase().startsWith(packageNameLowercase)) {} else {
                ret.add(ad);
            }
        }
        return ret;
    }
    public static List<AppDetail> makeAppDetailList(final Context ctx, final List<ResolveInfo> rilst, final AppDetailComparator cmpr) {
        List<AppDetail> ret = new ArrayList<>();
        for (ResolveInfo ri : rilst) {
            ret.add(makeAppDetail(ctx, ri));
        }
        if (cmpr != null) {
            Collections.sort(ret, cmpr);
        }
        return ret;
    }
    public static AppDetail makeAppDetail(final Context ctx, final ResolveInfo ri) {
        PackageManager manager = ctx.getPackageManager();
        AppDetail app = new AppDetail();
        app.setLabel(ri.loadLabel(manager).toString());
        app.setPackageName(ri.activityInfo.packageName);
        app.setName(ri.activityInfo.name);
        //ri.activityInfo.applicationInfo.sourceDir;
        //ri.activityInfo.applicationInfo.publicSourceDir;
        //ri.activityInfo.applicationInfo.loadIcon(pm);

        app.setIcon(AndrUtil.getIcon(manager, ri.activityInfo));
        return app;
    }
    public static AppDetail makeAppDetail(final Context ctx, final AppWidgetProviderInfo wi) {
        AppDetail app = new AppDetail();
        app.setWidget(wi);
        app.setLabel(wi.label);
        String packageName = wi.provider.getPackageName();
        try {
            app.setIcon(AndrUtil.getDrawableForDensityMax(AndrUtil.getAppResources(ctx, packageName), wi.previewImage));
        } catch (Exception e) {}
        return app;
    }
    public static Intent runApp(Context ctx, AppDetail ad) {
        return AndrUtil.runApp(ctx, ad.getPackageName(), ad.getName());
    }

    public static Intent getIntent2Start(Context ctx, AppDetail ad) {
        return AndrUtil.getIntent2Start(ctx, ad.getPackageName(), ad.getName());
    }
}
