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
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

public class AndrUtilGUI {
    /*
     * set full screen
requestWindowFeature(Window.FEATURE_NO_TITLE); getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
     *
     */
    public static WindowManager getWindowManager(Context ctx) {
        if (ctx instanceof Activity) {
            return ((Activity)ctx).getWindowManager();
        } else {
            return (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
        }
    }
    public static Point getWidthHeightByWM(Context ctx) {
        return getWidthHeight(getWindowManager(ctx));
    }
    public static Point getWidthHeight(WindowManager w) {
        int wd = 0;
        int ht = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            w.getDefaultDisplay().getSize(size);
            wd = size.x;
            ht = size.y;
        } else {
            Display d = w.getDefaultDisplay();
            wd = d.getWidth();
            ht = d.getHeight();
        }
        return new Point(wd, ht);
    }
    public static Point getWidthHeight(Context ctx) {
        final DisplayMetrics dmetrics = ctx.getResources().getDisplayMetrics();
        final float scale = dmetrics.density;
        int wd = (int) (dmetrics.widthPixels * scale + 0.5f);
        int ht = (int) (dmetrics.heightPixels * scale + 0.5f);
        return new Point(wd, ht);
    }
    public static void toastLong(final Context ctx, final String s) {
        toast(ctx, s, Toast.LENGTH_LONG);
    }
    public static void toastShort(final Context ctx, final String s) {
        toast(ctx, s, Toast.LENGTH_SHORT);
    }
    public static void toast(final Context ctx, final String s, final int length) {
        Toast t = Toast.makeText(ctx, s, length);
        t.setGravity(Gravity.TOP|Gravity.LEFT, 0, 0);
        t.show();
    }
}
