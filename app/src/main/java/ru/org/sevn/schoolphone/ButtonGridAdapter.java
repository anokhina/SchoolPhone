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

import java.util.ArrayList;
import java.util.List;

import ru.org.sevn.schoolphone.andr.AndrUtil;
import ru.org.sevn.schoolphone.andr.AndrUtilGUI;

import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ButtonGridAdapter extends BaseAdapter implements OnItemClickListener {


    private String lastSearch = "";
    public void search(String s) {
        lastSearch = s;
        appDetails.clear();
        if (s == null || s.length() == 0) {
            AndrUtil.copy(appDetails, appDetailsAll);
        } else
            for(Object obj : appDetailsAll) {
                if (obj instanceof AppDetail) {
                    AppDetail ad = (AppDetail)obj;
                    if (ad.getLabel() != null && ad.getLabel().toLowerCase().startsWith(s)) {
                        appDetails.add(ad);
                    }
                }
            }
        notifyDataSetChanged();
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
        if (obj instanceof AppDetail) {
            AppDetail ad = (AppDetail)obj;
            AppDetailManager.runApp(ctx, ad);
        }
    }

    private final List appDetailsAll;
    private final List appDetails;
    private final AppCategory category;
    private final int width;
    private final int height;
    private final Context context;

    public ButtonGridAdapter(Context ctx, AppCategory category, int dw, int dh) {
        this.context = ctx;
        this.category = category;
        appDetailsAll = new ArrayList<>();
        ///////////////////////////////
        AndrUtil.copy(appDetailsAll, AppCategoryManager.getAppsInfo(ctx, category.getId(), new AppDetailComparator()));
        appDetails = new ArrayList<>(appDetailsAll.size());
        AndrUtil.copy(appDetails, appDetailsAll);
        this.width = dw;
        this.height = dh;
    }

    public void renew() {
        appDetailsAll.clear();
        AndrUtil.copy(appDetailsAll, AppCategoryManager.getAppsInfo(context, category.getId(), new AppDetailComparator()));
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
            if (appDetails.get(position) instanceof AppDetail) {
                imageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, null);
//              imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
                imageView.setPadding(1, padd, 1, padd);
            } if (appDetails.get(position) instanceof View) {
                imageView = (View)appDetails.get(position);
            } if (appDetails.get(position) instanceof Intent) {
                imageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, null);
                imageView.setPadding(1, padd, 1, padd);
            } else {

            }
        }
        getButtonView(parent.getContext(), appDetails, position, imageView, width-padd, height-padd);
        return imageView;
    }

    public void arrangeButtonView(Context ctx, View convertView, AppDetail ad, TextView appLabel, ImageView appIcon) {
    }

    public View getButtonView(Context ctx, List apps, int position, View convertView, int dw, int dh) {
        TextView appLabel = (TextView)convertView.findViewById(R.id.item_app_label);
        ImageView appIcon = (ImageView)convertView.findViewById(R.id.item_app_icon);

        if (apps.get(position) instanceof Intent) {
            //Intent.EXTRA_SHORTCUT_INTENT
            //Intent.EXTRA_SHORTCUT_ICON
            Intent i = (Intent)apps.get(position);
            String zzz = "";
            if (appIcon != null) {
                //Intent.EXTRA_SHORTCUT_ICON_RESOURCE
                Drawable dr = null;
                Parcelable bmp = i.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
                AndrUtilGUI.toastLong(ctx, "**b**>"+bmp);
                if (bmp != null && bmp instanceof Bitmap) {
                    dr = new BitmapDrawable(convertView.getContext().getResources(), Bitmap.createScaledBitmap((Bitmap)bmp, dw, dh, true));
                } else {

                    Parcelable extra = i.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                    AndrUtilGUI.toastLong(ctx, "****>"+extra);
                    if (extra != null) {
                        AndrUtilGUI.toastLong(ctx, "****"+extra.getClass().getName());
                        if (extra instanceof ShortcutIconResource) {

                            try {
                                ShortcutIconResource iconResource = (ShortcutIconResource) extra;
                                final PackageManager packageManager = ctx.getPackageManager();
                                Resources resources = packageManager.getResourcesForApplication(
                                        iconResource.packageName);
                                final int id = resources.getIdentifier(iconResource.resourceName, null, null);
                                dr = AndrUtil.getDrawableForDensityMax(resources, id);
                            } catch (Exception e) {
                            }
                        }
                    }
                }

                if (dr != null) {
                    int h = dr.getIntrinsicHeight();
                    int w = dr.getIntrinsicWidth();
                    Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
                    Drawable sdr = new BitmapDrawable(convertView.getContext().getResources(), Bitmap.createScaledBitmap(bitmap, dw, dh, true));

                    appIcon.setImageDrawable(sdr);
                    zzz+=w;
                    zzz+="x";
                    zzz+=h;
                }
            }

            if (appLabel != null) {
                appLabel.setText(i.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
                //appLabel.setText("Btn>"+position+">"+zzz);
            }

        } else
        if (apps.get(position) instanceof AppDetail) {
            AppDetail appDetail = (AppDetail)apps.get(position);

            String zzz = "";
            if (appIcon != null) {
                Drawable dr = appDetail.getIcon();
                if (dr != null) {
                    int h = dr.getIntrinsicHeight();
                    int w = dr.getIntrinsicWidth();
                    Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
                    Drawable sdr = new BitmapDrawable(convertView.getContext().getResources(), Bitmap.createScaledBitmap(bitmap, dw, dh, true));

                    appIcon.setImageDrawable(sdr);
                    zzz+=w;
                    zzz+="x";
                    zzz+=h;
                }
            }

            if (appLabel != null) {
                appLabel.setText(appDetail.getLabel());
                arrangeButtonView(ctx, convertView, appDetail, appLabel, appIcon);
                //appLabel.setText("Btn>"+position+">"+zzz);
            }

            //        TextView appName = (TextView)convertView.findViewById(R.id.item_app_name);
            //        if (appName != null) {
            //          appName.setText(apps.get(position).getName());
            //        }
        }
        return convertView;
    }

}
