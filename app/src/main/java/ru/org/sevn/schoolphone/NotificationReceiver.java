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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.org.sevn.schoolphone.andr.Notifier;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String MAIN_NOTE = "ru.org.sevn.schoolphone.MAIN_NOTE";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(MAIN_NOTE)) {
            AppNotifier notifier = new AppNotifier();
            boolean extra = intent.getBooleanExtra("action", true);
            if (extra) {
                notifier.showNoteNotify(context, "Note:", "Some long text to display\n in several lines");
            } else {
                Notifier.cancelNotification(context, AppNotifier.SHOW_NOTE);
            }
        }
    }
}
