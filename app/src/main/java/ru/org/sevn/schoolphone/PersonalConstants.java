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

import android.content.Context;

import java.util.HashMap;

public class PersonalConstants {

    public static interface ConstantFiller {
        void fill(HashMap<String, Object> settings);
    }

    private static ConstantFiller getConstantFiller() {
        return new PrivateConstants();
    }

    private static final HashMap<String, Object> settings = new HashMap<>();

    static {
        getConstantFiller().fill(settings);
    }

    public static String get(String key) {
        return (String)settings.get(key);
    }

    public static Object getObject(String key) {
        return settings.get(key);
    }

    public static void putObject(String key, Object o) {
        settings.put(key, o);
    }

    public static LauncherFragment getAppInstance() {
        return ((LauncherFragment)PersonalConstants.getObject(AppConstants.INSTANCE));
    }
    public static Context getAppContext() {
        LauncherFragment launcherFragment = getAppInstance();
        if (launcherFragment != null) {
            return launcherFragment.getActivity();
        }
        return null;
    }
}
