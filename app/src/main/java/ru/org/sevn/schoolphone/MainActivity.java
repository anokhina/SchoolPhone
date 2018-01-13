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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import ru.org.sevn.schoolphone.andr.AndrUtilGUI;
import ru.org.sevn.schoolphone.andr.DialogUtil;

public class MainActivity extends FragmentActivity {

    ButtonGridAdapter badapter;
    private String password ="fake"; //TODO encript
    private ArrayList<String> allowedApps = new ArrayList<>();

    private void newPassword() {
        if (isSU()) {
            DialogUtil.ask(this, "Enter new password:", new DialogUtil.InputValidator() {
                @Override
                public void validate(String newPassword) {
                    if (newPassword == null || newPassword.length() == 0) {} else {
                        savePreferences(false, newPassword);
                        badapter.invalidate();
                    }
                }
            });
        }
    }
    private boolean isInAllowed(AppDetail ad) {
        return allowedApps.contains(ad.getPackageName());
    }
    private void allow() {
        if (currentAppDetail != null) {
            allowedApps.add(currentAppDetail.getPackageName());
            badapter.invalidate();
            savePreferences(true, null);
        }
    }
    private void disable() {
        if (currentAppDetail != null) {
            allowedApps.remove(currentAppDetail.getPackageName());
            badapter.invalidate();
            savePreferences(true, null);
        }
    }
    public static void resetPreferredLauncherAndOpenChooser(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, FakeActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        Intent selector = new Intent(Intent.ACTION_MAIN);
        selector.addCategory(Intent.CATEGORY_HOME);
        selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(selector);

        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
    }
    public void resetDefault() {
        resetPreferredLauncherAndOpenChooser(this);
        /*
        PackageManager manager = getPackageManager();
        ComponentName component = new ComponentName("ru.org.sevn.schoolphone", "ru.org.sevn.schoolphone.FakeActivity");
        manager.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        manager.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        */
    }

    private boolean isSU() {
        final EditText editTextPassword = (EditText) findViewById(R.id.editText);
        return password.equals(editTextPassword.getText().toString());

    }
    private boolean isAllowed(AppDetail ad) {
        if (allowedApps.contains(ad.getPackageName())) {
            return true;
        }
        return isSU();
    }

    private void restorePreferences() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        if (prefs != null) {
            password = prefs.getString("password", "zzz");
            String arrStr = prefs.getString("allowedApps", "[]");
            try {
                JSONArray arr = new JSONArray(arrStr);
                allowedApps.clear();
                for (int i = 0; i < arr.length(); i++) {
                    allowedApps.add(arr.getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    private void savePreferences(boolean async, String pswd) {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (pswd != null) {
            password = pswd;
            editor.putString("password", pswd);
        }
        JSONArray arr = new JSONArray(allowedApps);
        try {
            editor.putString("allowedApps", arr.toString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (async) {
            editor.apply();
            AndrUtilGUI.toastLong(this, "Settings are applied");
        } else {
            editor.commit();
            AndrUtilGUI.toastLong(this, "Settings are commited");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restorePreferences();

        setContentView(R.layout.fragment_list);
        final EditText editTextPassword = (EditText) findViewById(R.id.editText);

        EditText editText = (EditText) findViewById(R.id.editText_searchApp);

        int dw = AppConstants.CELL_WIDTH;
        Point wh = AndrUtilGUI.getWidthHeight(this);
        int numColumns = wh.x / dw;

        final GridView gridView = (GridView) findViewById(R.id.gridViewAppButtons);
        registerForContextMenu(gridView);
        gridView.setNumColumns(numColumns);
        badapter = new ButtonGridAdapter(this, AppCategoryStore.makeAppCategory(1, "apps"), dw, dw) {
            @Override
            public void arrangeButtonView(Context ctx, View convertView, AppDetail ad, TextView appLabel, ImageView appIcon) {
                if (isAllowed(ad)) {
                    if (isInAllowed(ad)) {
                        appLabel.setTextColor(Color.GREEN);
                    } else {
                        appLabel.setTextColor(Color.BLACK);
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
                        AppDetailManager.runApp(ctx, ad);
                    } else {
                        DialogUtil.alert(MainActivity.this, "Forbidden", "You are not allowed to run " + ad.getLabel(), null);
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
                badapter.invalidate();
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

    }

    public static final int CANCEL = 0;
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
            MenuInflater inflater = getMenuInflater();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id) {
            case R.id.action_settings:
                if (isSU()) {
                    newPassword();
                } else {
                    DialogUtil.alert(MainActivity.this, "Forbidden", "You are not allowed to set parameters", null);
                }
                return true;
            case R.id.set_default_launcher:
                if (isSU()) {
                    resetDefault();
                } else {
                    DialogUtil.alert(MainActivity.this, "Forbidden", "You are not allowed to reset defaults ", null);
                }
                return true;
            case R.id.action_renew:
                this.badapter.renew();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
