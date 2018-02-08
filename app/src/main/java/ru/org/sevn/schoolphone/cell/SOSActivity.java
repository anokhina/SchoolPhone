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

package ru.org.sevn.schoolphone.cell;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import ru.org.sevn.schoolphone.AppBroadcastReceiver;
import ru.org.sevn.schoolphone.R;

public class SOSActivity extends AppCompatActivity {

    private final int REQUEST_PERMISSION_CALL_PHONE = 1;

    private AppBroadcastReceiver callReceiver = new AppBroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            String phoneKey = TelephonyManager.EXTRA_INCOMING_NUMBER;
            String phoneNumber = intent.getStringExtra(phoneKey);

            if (state == null) {
                //Outgoing call
                phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            } else
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                //incoming
                //got phoneNumber here
            } else
            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            } else
            if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                getActivity().unregisterReceiver(this);
            }

            System.err.println("++++++++"+state+":"+phoneNumber);
        }
    };
    private Runnable callIt = new Runnable() {
        @Override
        public void run() {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + getPhoneNumber()));
            if (false && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            getActivity().registerReceiver(callReceiver,new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
            try {
                getActivity().startActivityForResult(callIntent, 2);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.err.println("++++++++");
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.err.println("++++++++>"+requestCode+":"+resultCode+":"+data);
    }

    private String getPhoneNumber() {
        return "11111";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doWithPermission(getActivity(), Manifest.permission.CALL_PHONE, REQUEST_PERMISSION_CALL_PHONE, callIt);
            }
        });
    }

    public Activity getActivity() {
        return this;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {

        switch (requestCode) {
            case REQUEST_PERMISSION_CALL_PHONE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    callIt.run();
                } else {
                    Toast.makeText(getActivity(), "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private static void showExplanation(final Activity ctx,
                                 String title,
                                 String message,
                                 final String permissionName,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ActivityCompat.requestPermissions(ctx, new String[]{permissionName}, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private static void doWithPermission(Activity ctx, String permissionName, int permissionRequestCode, Runnable onOk) {
        int permissionCheck = ContextCompat.checkSelfPermission(ctx, permissionName);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(ctx, permissionName)) {
                showExplanation(ctx, "Permission Needed", "Rationale", permissionName, permissionRequestCode);
            } else {
                ActivityCompat.requestPermissions(ctx, new String[]{permissionName}, permissionRequestCode);
            }
        } else {
            onOk.run();
        }
    }

}
