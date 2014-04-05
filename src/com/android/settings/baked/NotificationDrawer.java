/*
 * Copyright (C) 2012 The CyanogenMod project
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

package com.android.settings.baked;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ColorPickerPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Display;
import android.view.Window;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.baked.SeekBarPreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.io.File;
import java.io.IOException;

public class NotificationDrawer extends SettingsPreferenceFragment implements
            Preference.OnPreferenceChangeListener {
    private static final String TAG = "NotificationDrawer";

    private static final String UI_COLLAPSE_BEHAVIOUR = "notification_drawer_collapse_on_dismiss";
    private static final String PANEL_VIEW_BACKGROUND = "panel_view_background";
    private static final String PANEL_BACKGROUND_STYLE = "panel_background_style";
    private static final String PANEL_WALLPAPER_ALPHA = "panel_wallpaper_alpha";
    private static final String PANEL_VIEW_BACKGROUND_COLOR_FILL = "panel_view_background_color_fill";

    private static final int REQUEST_PICK_WALLPAPER = 201;
    private static final int COLOR_FILL = 0;
    private static final int CUSTOM_IMAGE = 1;
    private static final int DEFAULT = 2;

    private ColorPickerPreference mPanelColorFill;
    private ListPreference mCollapseOnDismiss;
    private ListPreference mPanelBackground;
    private SeekBarPreference mWallpaperAlpha;

    private File wallpaperImage;
    private File wallpaperTemporary;
    private PreferenceCategory mPanelViewBackground;

    private int seekbarProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.notification_drawer);
        PreferenceScreen prefScreen = getPreferenceScreen();

        mPanelViewBackground = (PreferenceCategory) findPreference(PANEL_VIEW_BACKGROUND);

        mCollapseOnDismiss = (ListPreference) findPreference(UI_COLLAPSE_BEHAVIOUR);
        mPanelBackground = (ListPreference) findPreference(PANEL_BACKGROUND_STYLE);
        mWallpaperAlpha = (SeekBarPreference) findPreference(PANEL_WALLPAPER_ALPHA);
        mPanelColorFill = (ColorPickerPreference) findPreference(PANEL_VIEW_BACKGROUND_COLOR_FILL);

        wallpaperImage = new File(getActivity().getFilesDir()+"/notifwallpaper");
        wallpaperTemporary = new File(getActivity().getCacheDir()+"/notifwallpaper.tmp");
    }

    @Override
    public void onResume() {
        super.onResume();
        setListeners();
        setDefaultValues();
        updateSummaries();
        updateVisiblePreferences();
    }

    protected void setListeners() {
        mCollapseOnDismiss.setOnPreferenceChangeListener(this);
        mPanelBackground.setOnPreferenceChangeListener(this);
        mWallpaperAlpha.setOnPreferenceChangeListener(this);
        mPanelColorFill.setOnPreferenceChangeListener(this);
    }

    protected void setDefaultValues() {
        mCollapseOnDismiss.setValue(Integer.toString(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_COLLAPSE_ON_DISMISS,
                Settings.System.STATUS_BAR_COLLAPSE_IF_NO_CLEARABLE)));
        mPanelBackground.setValue(Integer.toString(Settings.System.getInt(getContentResolver(),
                Settings.System.PANEL_BACKGROUND_STYLE, 2)));
        float mWallpaperAlphaTransparency = 1.0f;
        try {
            mWallpaperAlphaTransparency = Settings.System.getFloat(getContentResolver(),
                    Settings.System.PANEL_WALLPAPER_ALPHA);
        } catch (Exception e) {
            mWallpaperAlphaTransparency = 1.0f;
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.PANEL_WALLPAPER_ALPHA, 1.0f);
        }
        mWallpaperAlpha.setInitValue((int) (mWallpaperAlphaTransparency * 100));
    }

    protected void updateSummaries() {
        mCollapseOnDismiss.setSummary(mCollapseOnDismiss.getEntry());
        mPanelBackground.setSummary(mPanelBackground.getEntry());
        mPanelColorFill.setSummary(ColorPickerPreference.convertToARGB(Settings.System.getInt(
                getContentResolver(), Settings.System.PANEL_BACKGROUND_COLOR, 0xFF000000)));
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mCollapseOnDismiss) {
            int index = mCollapseOnDismiss.findIndexOfValue(objValue.toString());
            preference.setSummary(mCollapseOnDismiss.getEntries()[index]);
            int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_COLLAPSE_ON_DISMISS, value);
            return true;
        } else if (preference == mPanelBackground) {
            int index = mPanelBackground.findIndexOfValue(objValue.toString());
            preference.setSummary(mPanelBackground.getEntries()[index]);
            return handleBackgroundSelection(index);
        } else if (preference == mWallpaperAlpha) {
            float value = Float.parseFloat((String) objValue);
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.PANEL_WALLPAPER_ALPHA, value / 100);
            return true;
        } else if (preference == mPanelColorFill) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int value = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PANEL_BACKGROUND_COLOR, value);
            return true;
        }
        return false;
    }

    private void updateVisiblePreferences() {
        int visible = Settings.System.getInt(getContentResolver(),
                Settings.System.PANEL_BACKGROUND_STYLE, 2);
        if (visible == 1) {
            mPanelViewBackground.addPreference(mWallpaperAlpha);
        } else {
            mPanelViewBackground.removePreference(mWallpaperAlpha);
        }
        if (visible == 0) {
            mPanelViewBackground.addPreference(mPanelColorFill);
        } else {
            mPanelViewBackground.removePreference(mPanelColorFill);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_WALLPAPER) {
            if (resultCode == Activity.RESULT_OK) {
                if (wallpaperTemporary.exists()) {
                    wallpaperTemporary.renameTo(wallpaperImage);
                }
                wallpaperImage.setReadable(true, false);
                Toast.makeText(getActivity(), getResources().getString(R.string.
                        background_result_successful), Toast.LENGTH_LONG).show();
                Settings.System.putInt(getContentResolver(),
                        Settings.System.PANEL_BACKGROUND_STYLE, 1);
                updateVisiblePreferences();
            } else {
                if (wallpaperTemporary.exists()) {
                    wallpaperTemporary.delete();
                }
                Toast.makeText(getActivity(), getResources().getString(R.string.
                        background_result_not_successful), Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean handleBackgroundSelection(int index) {
        if (index == COLOR_FILL) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PANEL_BACKGROUND_STYLE, 0);
            updateVisiblePreferences();
            return true;
        } else if (index == CUSTOM_IMAGE) {
            // Used to reset the image when already set
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PANEL_BACKGROUND_STYLE, 2);
            // Launches intent for user to select an image/crop it to set as background
            final Display display = getActivity().getWindowManager().getDefaultDisplay();
            final Rect rect = new Rect();
            final Window window = getActivity().getWindow();

            window.getDecorView().getWindowVisibleDisplayFrame(rect);

            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;

            int width = display.getWidth();
            int height = display.getHeight() - titleBarHeight;
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            if (Utils.isTablet(getActivity())) {
                width = display.getWidth() / width;
                height = width;
                intent.putExtra("scaleType", 3);
            } else {
                intent.putExtra("scaleType", 6);
            }
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            intent.putExtra("aspectX", isPortrait ? width : height);
            intent.putExtra("aspectY", isPortrait ? height : width);

            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(wallpaperTemporary));
                intent.putExtra("return-data", false);
                getActivity().startActivityFromFragment(this, intent, REQUEST_PICK_WALLPAPER);
            } catch (IOException e) {
            } catch (ActivityNotFoundException e) {
            }
        } else if (index == DEFAULT) {
            // Sets background to default
            Settings.System.putInt(getContentResolver(),
                            Settings.System.PANEL_BACKGROUND_STYLE, 2);
            updateVisiblePreferences();
            return true;
        }
        return false;
    }
}
