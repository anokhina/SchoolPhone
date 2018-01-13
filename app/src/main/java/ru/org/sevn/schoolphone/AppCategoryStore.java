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
import java.util.LinkedHashMap;
import java.util.List;

public class AppCategoryStore {

    private AppCategoryStore() {}

    private static class SingletonHelper {
        private static final AppCategoryStore instance = new AppCategoryStore();
    }

    public static AppCategoryStore getInstance() {
        return SingletonHelper.instance;
    }

    private final LinkedHashMap<Long, AppCategory> categories = new LinkedHashMap<>();
    private final List<AppCategory> values = new ArrayList<>();

    public LinkedHashMap<Long, AppCategory> getCategories() {
        return categories;
    }
    public synchronized void refreshFromCategories() {
        values.clear();
        values.addAll(categories.values());
    }
    public synchronized void refreshFromValues() {
        categories.clear();
        int i = 0;
        for (AppCategory c : values) {
            i++;
            c.setSortOrder(i);
            categories.put(c.getId(), c);
        }
    }
    public List<AppCategory> getValues() {
        return values;
    }

    public static void fillDefaults(LinkedHashMap<Long, AppCategory> categories) {
        int i = 0;
        for (String s : new String[]{"apps", "favorite", "util", "internet", "office", "games", "other"}) {
            i++;
            AppCategory c = makeAppCategory(i, s);
            categories.put(c.getId(), c);
        }
    }

    public static final int ID_APPS = 1;

    public static AppCategory makeAppCategory(int i, String s) {
        AppCategory c = new AppCategory();
        c.setId(i);
        c.setSortOrder(i);
        c.setName(s);
        return c;
    }
}
