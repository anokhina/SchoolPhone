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
import java.util.List;

import ru.org.sevn.schoolphone.andr.AndrUtil;
import android.content.Context;

public class AppCategoryManager {
    //private static List<AppDetail> allApps = new ArrayList<>();

    public static List<AppDetail> getAppsInfo(final Context ctx, final long category, final AppDetailComparator cmpr){
//        if (allApps.size() == 0) { //TODO sync and comparator
//            allApps = AppDetailManager.makeAppDetailList(ctx, AndrUtil.getAvailableActivities(ctx), new AppDetailComparator());
//        }
        List<AppDetail> apps = new ArrayList<AppDetail>();
        AndrUtil.copy(apps, AppDetailManager.makeAppDetailList(ctx, AndrUtil.getAvailableActivities(ctx), new AppDetailComparator()));

        return apps;
    }

}
