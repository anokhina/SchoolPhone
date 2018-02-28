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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.org.sevn.schoolphone.R;
import ru.org.sevn.schoolphone.andr.AndrUtil;

public class ButtonGridAdapter extends BaseAdapter implements OnItemClickListener {

    class AppDetailCanAdd implements AndrUtil.CanAdd<ButtonDetail> {

        @Override
        public boolean canAdd(ButtonDetail o) {
            return canShow(o);
        }
    }

    private String lastSearch = "";
    public void search(String s) {
        lastSearch = s;
        appDetails.clear();
        if (s == null || s.length() == 0) {
            AndrUtil.copy(appDetails, appDetailsAll, new AppDetailCanAdd());
        } else
            for(Object obj : appDetailsAll) {
                if (obj instanceof ButtonDetail) {
                    ButtonDetail ad = (ButtonDetail)obj;
                    if (ad.getLabel() != null && ad.getLabel().toLowerCase().startsWith(s) && canShow(ad)) {
                        appDetails.add(ad);
                    }
                }
            }
        notifyDataSetChanged();
    }

    public boolean canShow(ButtonDetail ad) {
        return true;
    }

    public void invalidate() {
        List lst = new ArrayList();
        AndrUtil.copy(lst, appDetails);
        appDetails.clear();
        AndrUtil.copy(appDetails, lst);
        notifyDataSetChanged();
    }

    public void addIntent(Intent data) {
        appDetailsAll.add(data);
        appDetails.add(data);

    }

    public void addView(View newWidget) {
        appDetailsAll.add(newWidget);
        appDetails.add(newWidget);
    }

    @Override
    public void onItemClick(AdapterView parent, View v, int position, long id) {
        onItemClick(appDetails.get(position), v.getContext());
    }

    public void onItemClick(Object obj, Context ctx) {
        if (obj instanceof ButtonDetail) {
            ButtonDetail ad = (ButtonDetail)obj;
        }
    }

    private final List appDetailsAll;
    private final List appDetails;
    private final int width;
    private final int height;
    private final Context context;
    private final ButtonListStore store;

    public ButtonGridAdapter(Context ctx, ButtonListStore store, int dw, int dh) {
        this.store = store;
        this.context = ctx;
        appDetailsAll = new ArrayList<>();
        AndrUtil.copy(appDetailsAll, store.getObjectList(true));
        appDetails = new ArrayList<>(appDetailsAll.size());
        AndrUtil.copy(appDetails, appDetailsAll);
        this.width = dw;
        this.height = dh;
    }

    public void renew() {
        renew(false);
    }
    public void renew(boolean fetchApp) {
        appDetailsAll.clear();
        AndrUtil.copy(appDetailsAll, store.getObjectList(fetchApp));
        search(lastSearch);
    }

    public int getCount() {
        return appDetails.size();
    }

    public Object getItem(int position) {
        return appDetails.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        int padd = 3;
        View imageView = convertView;
        if (convertView == null) {
            if (appDetails.get(position) instanceof View) {
                imageView = (View) appDetails.get(position);
            } else {
                imageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, null);
//              imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
                imageView.setPadding(1, padd, 1, padd);
            }
        }
        getButtonView(parent.getContext(), appDetails, position, imageView, width-padd, height-padd);
        return imageView;
    }

    public void arrangeButtonView(Context ctx, View convertView, ButtonDetail ad, TextView appLabel, ImageView appIcon) {
    }

    public View getButtonView(Context ctx, List apps, int position, View convertView, int dw, int dh) {
        TextView appLabel = (TextView)convertView.findViewById(R.id.item_app_label);
        ImageView appIcon = (ImageView)convertView.findViewById(R.id.item_app_icon);

        if (apps.get(position) instanceof ButtonDetail) {
            ButtonDetail appDetail = (ButtonDetail)apps.get(position);

            if (appIcon != null) {
                Drawable dr = appDetail.getIcon();
                if (dr != null) {
                    Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
                    Drawable sdr = new BitmapDrawable(convertView.getContext().getResources(), Bitmap.createScaledBitmap(bitmap, dw, dh, true));

                    appIcon.setImageDrawable(sdr);
                }
            }

            if (appLabel != null) {
                appLabel.setText(appDetail.getLabel());
                arrangeButtonView(ctx, convertView, appDetail, appLabel, appIcon);
            }
        }
        return convertView;
    }

}
