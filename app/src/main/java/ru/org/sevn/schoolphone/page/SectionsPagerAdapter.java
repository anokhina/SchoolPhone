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

package ru.org.sevn.schoolphone.page;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import ru.org.sevn.schoolphone.LauncherFragment;

public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private Fragment launcherFragment;
    private PhoneFragment phoneFragment;
    private ScheduleFragment scheduleFragment;

    public SectionsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch(position) {
            case 0:
                if (scheduleFragment == null) {
                    scheduleFragment = new ScheduleFragment();
                }
                return scheduleFragment;
                //return PlaceholderFragment.newInstance(position);
            case 1:
                if (launcherFragment == null) {
                    launcherFragment = new LauncherFragment();
                }
                return launcherFragment;
            case 2:
                if (phoneFragment == null) {
                    phoneFragment = new PhoneFragment();
                }
                return phoneFragment;
        }
        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        /*
        switch (position) {
            case 0:
                return "Launcher";
            case 1:
                return "fake";
        }
        */
        return null;
    }
}