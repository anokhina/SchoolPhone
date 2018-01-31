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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

public class Notifier {
	private final int smallIconId;
    private final Class intentClass;
	
	public Notifier(int smallIconId, Class intentClass) {
		this.smallIconId = smallIconId;
        this.intentClass = intentClass;
	}
	
	public static PendingIntent makePendingNotificationIntent(Context ctx, Intent intent) {
        int requestCode = (int)System.currentTimeMillis();
        requestCode = 3;
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
	}
	public static Intent makeNotificationIntent(Context ctx, Class activityClass) {
        Intent intent = new Intent(ctx, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
	}
	protected NotificationCompat.Builder makeNotifyBuilder(Context ctx, String title, String text) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctx)
                .setSmallIcon(smallIconId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                //.setLights(Color.RED, 3000, 3000);
                //.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
//                    .setSound(Uri.parse(PreferenceManager.getDefaultSharedPreferences(ctx).getString(
//                            ConstUtil.get(ctx, R.string.const_pref_charge_ringtone), "default ringtone")))
                .setContentTitle(title)
                .setContentText(text);
//                    .setContent(contentView);
        return mBuilder;
    }
	protected Bitmap getLargeNotificationIcon(int mId) {
		return null;
	}
	protected int makeNotifyNumber(int mId) {
    	return 0;
    }
	protected boolean isAutoCancel(int mId) {
    	return true;
    }
    
    public void notify(int mId, Context ctx, String title, String text) {
        
        NotificationCompat.Builder mBuilder = makeNotifyBuilder(ctx, title, text);
        
        mBuilder.setLargeIcon(getLargeNotificationIcon(mId));
        mBuilder.setNumber(makeNotifyNumber(mId));
        
        mBuilder.setAutoCancel(isAutoCancel(mId));
        mBuilder.setContentIntent(makePendingNotificationIntent(ctx, makeNotificationIntent(ctx, this.intentClass)));
        NotificationManager mNotificationManager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification noti = mBuilder.build();
        mNotificationManager.notify(mId, noti);        
    }
    
    public static void cancelNotification(Context ctx, int notifyId) {
        NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notifyId);
    }    
}
