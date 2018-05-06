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

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import ru.org.sevn.schoolphone.andr.AndrUtilGUI;
import ru.org.sevn.schoolphone.page.ScheduleFragment;
import ru.org.sevn.schoolphone.page.SectionsPagerAdapter;

public class MainActivity extends AppCompatActivity {

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (! hasFocus) {
//            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
//            sendBroadcast(closeDialog);
        }
    }

    private SectionsPagerAdapter sectionsPagerAdapter;

    private ViewPager viewPager;

    private int defaultTurnOffTime;
    private BroadcastReceiver onOffReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            int dtnow = Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT, 1000);
            int dt = defaultTurnOffTime;
            if (dt < 60000) {
                dt = 60000 * 2;
            }
            if (dtnow < 60000) {
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT, dt);
            }
            //AndrUtilGUI.toastLong(MainActivity.this, "TurnOffTime>"+Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT, 60000));
            openTab(0);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tab_scroll);

        Button bt = (Button) findViewById(R.id.btSwOff);

        if (bt != null) {
            bt.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try2off();
                }
            });
        }

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setCurrentItem(1); //TODO position

//        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            public void onPageScrollStateChanged(int state) {}
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
//
//            public void onPageSelected(int position) {
//                Fragment fragment = sectionsPagerAdapter.getItem(position);
//                if (fragment instanceof ScheduleFragment) {
//                    ((ScheduleFragment)fragment).refresh();
//                }
//            }
//        });
        ScheduleFragment.initAlarms(this);

        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(onOffReceiver, screenStateFilter);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window wind = this.getWindow();
        if (wind != null) {
            wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

    @Override
    protected void onDestroy() {
        ScheduleFragment.destroyAlarms(this);
        unregisterReceiver(onOffReceiver);
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
//        if (sectionsPagerAdapter != null) {
//            try {
//                Fragment fragment = sectionsPagerAdapter.getItem(0); //TODO position
//                if (fragment instanceof ScheduleFragment) {
//                    ((ScheduleFragment) fragment).refresh();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    public void openTab(int pos) {
        if (viewPager != null) {
            PagerAdapter pa = viewPager.getAdapter();
            if (pa != null) {
                int len = viewPager.getAdapter().getCount();
                if (pos != viewPager.getCurrentItem() && pos < len && pos >= 0) {
                    viewPager.setCurrentItem(pos);
                }
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        openTab(1); //TODO position
    }

    private void try2off() {
        defaultTurnOffTime = Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT, 60000);
        Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT, 1000);

        AndrUtilGUI.toastLong(this, "TurnOffTime>"+Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT, 60000));
    }
}
