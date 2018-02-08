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

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

public class AppDetail {
    private long id;
    private AppCategory category;
    private int sortOrder;
    private String label;
    private String packageName;
    private String name;
    private Drawable icon;
    private boolean widgetFlag;
    private boolean shortcutFlag;
    private AppWidgetProviderInfo widget;
    private ResolveInfo info;

    public boolean isShortcutFlag() {
        return shortcutFlag;
    }
    public void setShortcutFlag(boolean shortcutFlag) {
        this.shortcutFlag = shortcutFlag;
    }
    public ResolveInfo getInfo() {
        return info;
    }
    public void setInfo(ResolveInfo info) {
        this.info = info;
    }
    public boolean isWidgetFlag() {
        return widgetFlag;
    }
    public void setWidgetFlag(boolean widgetFlag) {
        this.widgetFlag = widgetFlag;
    }
    public AppWidgetProviderInfo getWidget() {
        return widget;
    }
    public void setWidget(AppWidgetProviderInfo widget) {
        setWidgetFlag((widget != null));
        this.widget = widget;
    }
    public AppCategory getCategory() {
        return category;
    }
    public void setCategory(AppCategory category) {
        this.category = category;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public String getComponentName() {
        return packageName + "/" + name;
    }
    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String name) {
        this.packageName = name;
    }
    public Drawable getIcon() {
        return icon;
    }
    public void setIcon(Drawable icon) {
        this.icon = icon;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public int getSortOrder() {
        return sortOrder;
    }
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
    public void setShortcut(ResolveInfo i) {
        setShortcutFlag((i != null));
        setInfo(i);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AppDetail() {}
    public AppDetail(ComponentName cname) {
        this(cname.getPackageName(), cname.getClassName());
    }
    public AppDetail(String p, String n) {
        setPackageName(p);
        setName(n);
    }

}
