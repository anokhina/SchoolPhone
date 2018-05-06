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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ru.org.sevn.schoolphone.AppConstants;
import ru.org.sevn.schoolphone.LauncherFragment;
import ru.org.sevn.schoolphone.R;
import ru.org.sevn.schoolphone.andr.AndrUtilGUI;
import ru.org.sevn.schoolphone.andr.IOUtil;

public class PhoneFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list_phones, container, false);
        return rootView;
    }

    private ButtonGridAdapter badapter;

    private class PhoneDetail extends ButtonDetail {
        private String phone;
        public PhoneDetail() {
            try {
                this.setIcon(getResources().getDrawable(R.mipmap.ic_launcher));
            } catch (Exception e) {

            }
        }
        public PhoneDetail(Context ctx, String content) {
            this();
            setPhone(getParam(content, "phone"));
            setLabel(getParam(content, "name"));
            setIcon(ctx, getParam(content, "icon"));
        }
        private String getParam(String content, String paramName) {
            String l = "";
            String bParam = "<" + paramName + ">";
            String eParam = "</" + paramName + ">";
            int idx = content.indexOf(bParam);
            if (idx >= 0) {
                int idx2 = content.indexOf(eParam, idx);
                if (idx2<0) {
                    l = content.substring(idx + bParam.length());//TODO
                } else {
                    l = content.substring(idx + bParam.length(), idx2);
                }
            }
            return l;
        }
        public void setIcon(Context ctx, String s) {
            if (s != null) {
                s = s.trim();
                if (s.length() > 0) {
                    try {
                        //BASE64;JPEG:
                        int idx = s.indexOf(":");
                        String sdata = s;
                        if (idx > 0) {
                            sdata = s.substring(idx + 1);
                        }
                        byte[] bmp = Base64.decode(sdata, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bmp, 0, bmp.length);
                        if (bitmap != null) {
                            BitmapDrawable drawable = new BitmapDrawable(ctx.getResources(), bitmap);
                            setIcon(drawable);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        public PhoneDetail(String label, String phone) {
            this();
            this.setLabel(label);
            this.setPhone(phone);
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    //TODO
    public static String EXT_APP_PHONES_DIR = LauncherFragment.LauncherAdapter.EXT_APP_DIR + "phones/";

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        ButtonListStore store = new ButtonListStore() {
            final ArrayList<ButtonDetail> buttonDetails = new ArrayList<>();
            private void readList() {
                File root = IOUtil.getExternalDir(false);
                if (root != null) {
                    try {
                        File directory = new File(root, EXT_APP_PHONES_DIR);

                        File[] files = directory.listFiles();//TODO filter
                        if (files != null) {
                            Arrays.sort(files, new Comparator<File>() {
                                @Override
                                public int compare(File o1, File o2) {
                                    return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
                                }
                            });
                            for (int i = 0; i < files.length; i++) {
                                String fname = files[i].getName();
                                if (fname.endsWith(".phone")) {
                                    String content = IOUtil.readExt(files[i]);
                                    if (content != null) {
                                        buttonDetails.add(new PhoneDetail(getActivity(), content));
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            public List getObjectList(boolean refresh){
                if (buttonDetails.size() == 0 || refresh) {
                    buttonDetails.clear();
                    readList();
                }
                return buttonDetails;
            }
        };
        View rootView = getView();

        int dw = AppConstants.CELL_WIDTH;
        Point wh = AndrUtilGUI.getWidthHeight(getActivity());
        int numColumns = wh.x / dw;

        final GridView gridView = (GridView) rootView.findViewById(R.id.gridViewAppButtons);
        registerForContextMenu(gridView);
        gridView.setNumColumns(numColumns);
        badapter = new ButtonGridAdapter(getActivity(), store, dw, dw) {
            @Override
            public boolean canShow(ButtonDetail ad) {
                return true;
            }

            @Override
            public void arrangeButtonView(Context ctx, View convertView, ButtonDetail ad, TextView appLabel, ImageView appIcon) {
                appLabel.setTextColor(Color.BLUE);
            }
            @Override
            public void onItemClick(final Object obj, final Context ctx) {
                if (obj instanceof PhoneDetail) {
                    final PhoneDetail ad = (PhoneDetail)obj;
                    if (ad.getPhone() != null) {
                        Uri uri = Uri.fromParts("tel", ad.getPhone(), null);
                        Intent intent = null;
                        try {
                            intent = new Intent(Intent.ACTION_CALL, uri);
                            startActivity(intent);
                        } catch (Exception e) {
                            intent = new Intent(Intent.ACTION_DIAL, uri);
                            startActivity(intent);
                        }
                    }
                }
            }
        };
        gridView.setAdapter(badapter);
        gridView.setOnItemClickListener(badapter);

        EditText editText = (EditText) rootView.findViewById(R.id.editText_searchApp);
        AndrUtilGUI.setClearAction(editText);
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

    @Override
    public void onResume() {
        super.onResume();
        if (badapter.getCount() == 0) {
            badapter.renew();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_phones, menu);//action_renew
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_renew:
                this.badapter.renew(true);
                return true;
        }
        return false;
    }
}
