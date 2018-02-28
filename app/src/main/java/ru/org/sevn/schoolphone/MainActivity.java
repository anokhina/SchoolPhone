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

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import java.util.Collection;

import ru.org.sevn.schoolphone.andr.AlarmUtil;
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

    private Collection<AlarmUtil.SetAlarm> alarms;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tab_scroll);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setCurrentItem(1); //TODO position

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            public void onPageSelected(int position) {
                Fragment fragment = sectionsPagerAdapter.getItem(position);
                if (fragment instanceof ScheduleFragment) {
                    ((ScheduleFragment)fragment).refresh();
                }
            }
        });
        alarms = ScheduleFragment.setAlarms(this);
    }
    @Override
    protected void onDestroy() {
        if (alarms != null) {
            final AlarmManager manager = (AlarmManager)(this.getSystemService( Context.ALARM_SERVICE ));
            for(AlarmUtil.SetAlarm br : alarms) {
                AlarmUtil.unset(this, br);
            }
        }
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (sectionsPagerAdapter != null) {
            try {
                Fragment fragment = sectionsPagerAdapter.getItem(0); //TODO position
                if (fragment instanceof ScheduleFragment) {
                    ((ScheduleFragment) fragment).refresh();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}
