package ru.org.sevn.schoolphone;

import android.content.Context;

import ru.org.sevn.schoolphone.andr.Notifier;

public class AppNotifier extends Notifier {
	public AppNotifier() {

		super(R.mipmap.ic_launcher, MainActivity.class);
	}

	public static final int BATTERY_POWER = 1;
	public static final int SHOW_NOTE = 2;

    public void batteryPowerNotify(Context ctx, String title, String text) {
    	notify(BATTERY_POWER, ctx, title, text);
    }
    public void showNoteNotify(Context ctx, String title, String text) {
    	notify(SHOW_NOTE, ctx, title, text);
    }
	public void showCallNotify(int id, Context ctx, String title, String text) {
		notify(id, ctx, title, text);
	}

    @Override
    protected boolean isAutoCancel(int mId) {
    	if (SHOW_NOTE == mId) {
    		return false;
    	}
    	return true;
    }
}
