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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;

import ru.org.sevn.schoolphone.andr.AndrUtil;
import ru.org.sevn.schoolphone.andr.AndrUtilGUI;
import ru.org.sevn.schoolphone.andr.AudioUtil;
import ru.org.sevn.schoolphone.andr.DialogUtil;
import ru.org.sevn.schoolphone.andr.IOUtil;
import ru.org.sevn.schoolphone.connections.ConnectionUtil;

public class LauncherFragment extends Fragment {
    private LauncherAdapter ladapter;

    public LauncherAdapter getLadapter() {
        return ladapter;
    }

    public LauncherFragment() {
        ladapter = new LauncherAdapter(this);
    }

    ButtonGridAdapter badapter;
    private String password ="fake"; //TODO encript
    private String profile = LauncherAdapter.PROFILE_SCHOOL;
    private ArrayList<String> allowedApps = new ArrayList<>();
    private ArrayList<String> allowedAppsAnother = new ArrayList<>();
    private ArrayList<String> killApps = new ArrayList<>();
    private ArrayList<String> killAppsOther = new ArrayList<>();

    private void newPassword() {
        if (isSU()) {
            DialogUtil.askPassword(this.getActivity(), "Enter new password:", new DialogUtil.InputValidator() {
                @Override
                public void validate(String newPassword) {
                    if (newPassword == null || newPassword.length() == 0) {} else {
                        savePreferences(VERSION, false, newPassword);
                        badapter.renew();
                    }
                }
            });
        }
    }
    private boolean isInKilled(AppDetail ad) {
        return isInKilled(ad.getComponentName());
    }
    private boolean isInKilled(String componentName) {
        return killAppsOther.contains(componentName);
    }
    private boolean isInAllowed(AppDetail ad) {
        return allowedApps.contains(ad.getComponentName());
    }
    private boolean isInAllowedPackage(AppDetail ad) {
        return allowedApps.contains(ad.getPackageName());
    }
    private void allow() {
        if (currentAppDetail != null) {
            allowedApps.add(currentAppDetail.getComponentName());
            renewKillApps();
            badapter.invalidate();
            savePreferences(VERSION, true, null);
        }
    }
    private void disable() {
        if (currentAppDetail != null) {
            allowedApps.remove(currentAppDetail.getComponentName());
            allowedApps.remove(currentAppDetail.getName());
            allowedApps.remove(currentAppDetail.getPackageName()); //TODO
            renewKillApps();
            badapter.invalidate();
            savePreferences(VERSION, true, null);
        }
    }

    private void resetDefault() {
        AndrUtil.resetPreferredLauncherAndOpenChooser(this.getActivity());
        /*
        PackageManager manager = getPackageManager();
        ComponentName component = new ComponentName("ru.org.sevn.schoolphone", "ru.org.sevn.schoolphone.FakeActivity");
        manager.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        manager.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        */
    }

    private boolean isSU() {
        View view = getView();
        if (view != null) {
            final EditText editTextPassword = (EditText) view.findViewById(R.id.editText);
            if (password != null) {
                return password.equals(editTextPassword.getText().toString());
            }
        }
        return false;
    }
    private boolean isAllowedInProfile(AppDetail ad) {//TODO
        if (allowedApps != null && allowedApps.contains(ad.getName()) || allowedApps.contains(ad.getPackageName()) || allowedApps.contains(ad.getComponentName())) {
            return true;
        }
        return false;
    }
    private boolean isAllowed(AppDetail ad) {//TODO
        if (isAllowedInProfile(ad)) {
            return true;
        }
        return isSU();
    }

    private void fixExtDir() {
        File fl = IOUtil.getExternalFile(true, LauncherAdapter.EXT_APP_DIR);
        if (fl != null && fl.exists() && !fl.isDirectory()) {
            fl.delete();
        }
    }
    private String getDefaultCollection(String collectionName, String versionWithPoint, String defValue) {
        String fileName = LauncherAdapter.EXT_APP_DIR + collectionName + profile + versionWithPoint;
        File fl = IOUtil.getExternalFile(false, fileName);
        if (fl != null && !fl.exists() && versionWithPoint.length() > 0) {
            fileName = LauncherAdapter.EXT_APP_DIR + collectionName + versionWithPoint;
        }

        String defVal = IOUtil.readExt(fileName);
        if (defVal == null) {
            defVal = defValue;
            AndrUtilGUI.toastLong(this.getActivity(), "Can't read from:" + fileName);
            Log.d("EXT_APP_DIR", "Can't read from:" + fileName);
        }
        return defVal;
    }
    //fixExtDir();
    private void restorePreferences() {
        SharedPreferences prefs = getPrivateSharedPreferences();
        if (prefs != null) {
            password = prefs.getString("password", "zzz");
            restoreProfile(prefs);
            restoreCollection(profile, "allowedApps", allowedApps, prefs, getDefaultCollection("allowedApps", "", "[]"), false);
            ArrayList<String> another = new ArrayList<>();
            String p = LauncherAdapter.PROFILE_ALL;
            if (LauncherAdapter.PROFILE_ALL.equals(profile)) {
                p = LauncherAdapter.PROFILE_SCHOOL;
            }
            restoreCollection(p, "allowedApps", allowedAppsAnother, prefs, getDefaultCollection("allowedApps", "", "[]"), false);
            //restoreCollection(profile, "killApps", killApps, prefs, getDefaultCollection("killApps", "", "[]"), false);
            renewKillApps();
        }
    }
    private SharedPreferences getPrivateSharedPreferences() {
        return getActivity().getPreferences(Context.MODE_PRIVATE);
    }
    private void restoreProfile(SharedPreferences prefs) {
        String tmpProfile = prefs.getString(PREF_SELECTED_PROFILE, LauncherAdapter.PROFILE_SCHOOL);
        if (isAllowedProfile(tmpProfile)) {
            profile = tmpProfile;
            adjustVolume();
        }
    }
    private static void restoreCollection(String profile, String collectionName, Collection<String> collection, SharedPreferences prefs, String defVal, boolean fromDefault) {
        String arrStr;
        if (fromDefault) {
            arrStr = defVal;
        } else {
            arrStr = prefs.getString(collectionName+profile, defVal);
        }
        try {
            JSONArray arr = new JSONArray(arrStr);
            collection.clear();
            for (int i = 0; i < arr.length(); i++) {
                collection.add(arr.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static String VERSION = "8";

    private void savePreferencesProfile() {
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_SELECTED_PROFILE, profile).commit();
    }
    private void savePreferences(String version, boolean async, String pswd) {
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (pswd != null) {
            password = pswd;
            editor.putString("password", pswd);
        }
        editor.putString(PREF_SELECTED_PROFILE, profile);

        String msg = "";
        msg += savePrefsCollection("allowedApps", allowedApps, editor, version);
//        msg += savePrefsCollection("killApps", killApps, editor, version);

        if (async) {
            editor.apply();
            AndrUtilGUI.toastLong(this.getActivity(), "Settings are applied." + msg);
        } else {
            editor.commit();
            AndrUtilGUI.toastLong(this.getActivity(), "Settings are committed." + msg);
        }
    }
    private String savePrefsCollection(String collectionName, Collection<String> collection, SharedPreferences.Editor editor, String version) {
        String mainFileName = LauncherAdapter.EXT_APP_DIR + collectionName + profile;
        String msg = "";
        JSONArray arr = new JSONArray(collection);
        boolean saveExt = false;
        try {
            String jsonStr = arr.toString(2);
            editor.putString(collectionName+profile, jsonStr);
            saveExt = IOUtil.saveExt(mainFileName, jsonStr.getBytes(IOUtil.FILE_ENCODING));
            saveExt = IOUtil.saveExt(mainFileName + "." + version, jsonStr.getBytes(IOUtil.FILE_ENCODING));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        msg += ("Save ext:" + saveExt);
        msg += (":" + mainFileName);
        return msg;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        return rootView;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        restorePreferences();
        View rootView = getView();

        final EditText editTextPassword = (EditText) rootView.findViewById(R.id.editText);

        EditText editText = (EditText) rootView.findViewById(R.id.editText_searchApp);

        AndrUtilGUI.setClearAction(editText);
        AndrUtilGUI.setClearAction(editTextPassword);

        int dw = AppConstants.CELL_WIDTH;
        Point wh = AndrUtilGUI.getWidthHeight(getActivity());
        int numColumns = wh.x / dw;

        final GridView gridView = (GridView) rootView.findViewById(R.id.gridViewAppButtons);
        registerForContextMenu(gridView);
        gridView.setNumColumns(numColumns);
        badapter = new ButtonGridAdapter(getActivity(), AppCategoryStore.makeAppCategory(1, "apps"), dw, dw) {
            @Override
            public boolean canShow(AppDetail ad) {
                boolean ret = isAllowed(ad);
                //Log.d("zzz>", ""+ret);
                return ret;
            }

            @Override
            public void arrangeButtonView(Context ctx, View convertView, AppDetail ad, TextView appLabel, ImageView appIcon) {
                if (isAllowed(ad)) {
                    if (isInAllowed(ad)) {
                        appLabel.setTextColor(Color.BLUE);
                    } else if (isInAllowedPackage(ad)) {
                        appLabel.setTextColor(Color.GREEN);
                    } else {
                        appLabel.setTextColor(Color.BLACK);
                    }

                    if (isInKilled(ad)) {
                        appLabel.setTextColor(Color.RED);
                        //appLabel.setTypeface(null, Typeface.BOLD);
                    }
                } else {
                    appLabel.setTextColor(Color.RED);
                }
            }
            @Override
            public void onItemClick(final Object obj, final Context ctx) {
                if (obj instanceof AppDetail) {
                    final AppDetail ad = (AppDetail)obj;
                    if (isAllowed(ad)) {
                        Intent intent = AppDetailManager.getIntent2Start(ctx, ad);
                        if (intent != null) {
                            //TODO
//                            if (PROFILE_SCHOOL.equals(profile) &&
//                                    isProcess2kill(ad.getComponentName())) {
//                                intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
//                                                Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                            }
                            ctx.startActivity(intent);
                        }
                        //AppDetailManager.runApp(ctx, ad);
                    } else {
                        DialogUtil.alert(getActivity(), "Forbidden", "You are not allowed to run " + ad.getLabel(), null);
                    }
                }
            }
        };
        gridView.setAdapter(badapter);
        gridView.setOnItemClickListener(badapter);
        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                badapter.renew();
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            private CharSequence seq;

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                seq = cs;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int arg1, int arg2, int arg3) {

            }

            @Override
            public void afterTextChanged(Editable arg0) {
                //Toast.makeText(getContext(), ">>>afterTextChanged>>" + seq.toString().trim().toLowerCase(), Toast.LENGTH_SHORT).show();

                badapter.search(seq.toString().trim());
            }

        });
        badapter.renew();

        String myHome = AndrUtil.getDefaultHome(getActivity());
        AndrUtilGUI.toastLong(getActivity(), "Home:" + myHome);
        if (!"ru.org.sevn.schoolphone".equals(myHome)) {
            resetDefault();
        }

        //AlarmUtil.setAlarm(this, AlarmReceiver.POWER_ALARM, 1000*1, 1000*5);

        Intent i = new Intent(getActivity(), CheckTopActivityService.class);
        getActivity().startService(i);
    }

    private static final int CANCEL = 0;
    private AppDetail currentAppDetail;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        currentAppDetail = null;
        if (v.getId() == R.id.gridViewAppButtons) {
            GridView gv = (GridView) v;
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
            AppDetail obj = (AppDetail) gv.getItemAtPosition(acmi.position);
            MenuInflater inflater = getActivity().getMenuInflater();
            if (isSU()) {
                inflater.inflate(R.menu.context_menu_apps, menu);
            } else {
                inflater.inflate(R.menu.context_menu_apps_empty, menu);

            }
            MenuItem mi = menu.add(0, CANCEL, Menu.NONE, "[" + obj.getLabel() + "]");
            currentAppDetail = obj;
        }
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_allow:
                allow();
                return true;
            case R.id.action_forbid:
                disable();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
    }

    private static final String PREF_SELECTED_PROFILE = "profile";

    private void setProfileTo(String txt) {
        setProfileTo(txt, false, -1);
    }
    private int setProfileTo(String txt, boolean noui, int volPctFromCall) {
        int retVol = -1;
        if (txt != null) {
            if (!txt.equals(profile)) {
                txt = txt.trim();
                if (txt.length() > 0 && isAllowedProfile(txt)) {
                    profile = txt;
                    retVol = adjustVolume(volPctFromCall);
                    savePreferencesProfile();
                    restorePreferences();
                    badapter.renew(true);
                    savePreferences(VERSION, true, null);
                    //if (PROFILE_SCHOOL.equals(profile)) {
                    //killProcesses();
                    //}
                } else {
                    if (noui) {
                    } else {
                        forbiddenMsg(" set the profile name to " + txt);
                    }
                }
            } else {
                retVol = adjustVolume(volPctFromCall);
            }
        }
        return retVol;
    }

    private boolean isProcess2kill(String name) {
        return killApps.contains(name);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id) {
            case R.id.action_profile:
                if (!isSU()) {
                    DialogUtil.alert(getActivity(), "Profile:", profile, null);
                } else {
                    DialogUtil.ask(getActivity(), "Set profile to ", profile, new DialogUtil.InputValidator() {
                        @Override
                        public void validate(String txt) {
                            setProfileTo(txt);
                        }
                    });
                }
                return true;
            case R.id.action_restore:
                if (!isSU()) {
                    forbiddenMsg("import settings ");
                } else {
                    importSettings();
                }
                return true;
            case R.id.action_export:
                if (!isSU()) {
                    forbiddenMsg("export settings ");
                } else {
                    exportSettings();
                }
                return true;
            case R.id.action_settings:
                if (!isSU()) {
                    forbiddenMsg("set parameters ");
                } else {
                    newPassword();
                }
                return true;
            case R.id.set_default_launcher:
                String myHome = AndrUtil.getDefaultHome(getActivity());
                AndrUtilGUI.toastLong(getActivity(), "Home:" + myHome);
                if ("ru.org.sevn.schoolphone".equals(myHome)) {
                    if (!isSU()) {
                        forbiddenMsg("reset defaults ");
                    } else {
                        resetDefault();
                    }
                } else {
                    resetDefault();
                }

                return true;
            case R.id.action_renew:
                this.badapter.renew(true);
                return true;
            case R.id.action_clear:
                if (ConnectionUtil.isAirplaneModeOn(getActivity())) {
                    ConnectionUtil.setAirPlaneModeOn(getActivity(), false);
                } else {
                    getActivity().sendBroadcast(new Intent(AppConstants.ACTION_SOS));
                }

//                final EditText editTextPassword = (EditText) findViewById(R.id.editText);
//                editTextPassword.setText("");
//                this.badapter.renew();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isAllowedProfile(String n) {
        return (LauncherAdapter.PROFILE_SCHOOL.equals(n) || LauncherAdapter.PROFILE_ALL.equals(n));
    }

    private void exportSettings() {
        DialogUtil.ask(getActivity(), "Export for ", VERSION, new DialogUtil.InputValidator() {
            @Override
            public void validate(String txt) {
                if (txt != null) {
                    txt = txt.trim();
                    if (txt.length() > 0) {
                        savePreferences(txt, true, null);
                    }
                }
            }
        });
    }

    private void importSettings() {
        DialogUtil.ask(getActivity(), "Import for ", VERSION, new DialogUtil.InputValidator() {
            @Override
            public void validate(String txt) {
                if (txt == null) { txt = ""; }
                else {
                    txt = txt.trim();
                    if (txt.length() > 0) {
                        if (!txt.startsWith(".")) {
                            txt = "." + txt;
                        }
                    }
                }

                String restoreStr = getDefaultCollection("allowedApps", txt, null);
                if (restoreStr != null) {
                    restoreCollection(profile, "allowedApps", allowedApps, getPrivateSharedPreferences(), restoreStr, true);
                    renewKillApps();
                    badapter.renew(true);
                    savePreferences(VERSION, true, null);
                }
            }
        });
    }

    private void forbiddenMsg(String actionName) {
        DialogUtil.alert(getActivity(), "Forbidden", "You are not allowed to " + actionName, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        PersonalConstants.putObject(AppConstants.INSTANCE, this);
    }
    @Override
    public void onDestroy() {
        getActivity().stopService(new Intent(getActivity(), CheckTopActivityService.class));
        super.onDestroy();
        PersonalConstants.putObject(AppConstants.INSTANCE, null);
    }

    private int adjustVolume() {
        return adjustVolume(-1);
    }
    private int adjustVolume(int realVolPct) {
        int setRetVal = 0;
        if (LauncherAdapter.PROFILE_SCHOOL.equals(profile)) {
        } else {
            setRetVal = 100;
        }
        if (realVolPct >= 0) {
            AudioUtil.setSMSCallVolume(getActivity(), realVolPct);
        } else {
            AudioUtil.setSMSCallVolume(getActivity(), setRetVal);
        }
        return setRetVal;
    }

    private int callVol = -1;
    private void saveSettings() {
        saveSettings(-1);
    }
    private void saveSettings(int vol) {
        //TODO
        if (vol >= 0) {
            callVol = vol;
        } else {
            callVol = AudioUtil.getSMSCallVol(getActivity());
        }
        //System.err.println("+++++++++>" + callVol);
    }
    private void restoreSettings() {
        if (callVol >= 0) {
            AudioUtil.setSMSCallVol(getActivity(), callVol);
            //System.err.println("+++++++++>>" + callVol);
            callVol = -1;
        }
    }
    private String getProfile() {
        return profile;
    }

    private void renewKillApps() {
        killApps = getKillApps();
        killAppsOther = getKillAppsOther();
    }
    private ArrayList<String> getKillApps() {
        ArrayList<String> killApps = new ArrayList<>();
        killApps.addAll(allowedAppsAnother);
        killApps.removeAll(allowedApps);
        return killApps;
    }
    private ArrayList<String> getKillAppsOther() {
        ArrayList<String> killAppsOther = new ArrayList<>();
        killAppsOther.addAll(allowedApps);
        killAppsOther.removeAll(allowedAppsAnother);
        return killAppsOther;
    }

    public static class LauncherAdapter {
        public static final String PROFILE_SCHOOL = "_school";
        public static final String PROFILE_ALL = "_school1";
        public static String EXT_APP_DIR = "ru.org.sevn/schoolphone/";
        public static String EXT_APP_LOG_DIR = EXT_APP_DIR + "log/";
        private LauncherFragment activity;

        LauncherAdapter(LauncherFragment ma) {
            this.activity = ma;
        }

        public Activity getActivity() {
            return activity.getActivity();
        }
        public boolean isSU() {
            return activity.isSU();
        }
        public int setProfileTo(String txt, boolean noui, int volPctFromCall) {
            return activity.setProfileTo(txt, noui, volPctFromCall);
        }
        public boolean isProcess2kill(String name) {
            return activity.isProcess2kill(name);
        }
        public boolean isProcessInKilled(String name) {
            return activity.isInKilled(name);
        }
        public void saveSettings() {
            activity.saveSettings();
        }
        public void saveSettings(int vol) {
            activity.saveSettings(vol);
        }
        public void restoreSettings() {
            activity.restoreSettings();
        }
        public String getProfile() {
            return activity.getProfile();
        }
    }
    public static void showLauncher() {
        LauncherFragment.LauncherAdapter mactivity = null;
        LauncherFragment launcherFragment = PersonalConstants.getAppInstance();
        if (launcherFragment != null) {
            mactivity = launcherFragment.getLadapter();
            ActivityManager activityManager = (ActivityManager)mactivity.getActivity().getSystemService(Activity.ACTIVITY_SERVICE);
            activityManager.moveTaskToFront(mactivity.getActivity().getTaskId(), 0);
        }
    }
}