/*
 * Copyright (C) 2012 The CyanogenMod Project
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
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.CheckBoxPreference;
import android.preference.ColorPickerPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Window;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.baked.SeekBarPreference;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

public class LockscreenInterface extends SettingsPreferenceFragment implements
            Preference.OnPreferenceChangeListener {

    private static final String LOCKSCREEN_WIDGETS_CATEGORY = "lockscreen_widgets_category";
    private static final String LOCKSCREEN_BACKGROUND_CATEGORY = "lockscreen_background_category";
    private static final String KEY_BATTERY_STATUS = "lockscreen_battery_status";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";
    private static final String KEY_ENABLE_WIDGETS = "keyguard_enable_widgets";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_ENABLE_CAMERA = "keyguard_enable_camera";
    private static final String LOCKSCREEN_BACKGROUND_STYLE = "lockscreen_background_style";
    private static final String LOCKSCREEN_BACKGROUND_COLOR_FILL = "lockscreen_background_color_fill";
    private static final String LOCKSCREEN_WALLPAPER_ALPHA = "lockscreen_wallpaper_alpha";
    private static final String LOCKSCREEN_WALLPAPER_FILE_NAME = "lockwallpaper";

    private static final int REQUEST_PICK_WALLPAPER = 201;
    private static final int COLOR_FILL = 0;
    private static final int CUSTOM_IMAGE = 1;
    private static final int DEFAULT = 2;

    private CheckBoxPreference mEnableKeyguardWidgets;
    private CheckBoxPreference mEnableCameraWidget;
    private ColorPickerPreference mLockColorFill;
    private ListPreference mLockBackground;
    private ListPreference mBatteryStatus;
    private LockPatternUtils mLockUtils;
    private SeekBarPreference mWallpaperAlpha;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private DevicePolicyManager mDPM;

    private PreferenceCategory mLockscreenBackground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lockscreen_interface_settings);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        mLockUtils = mChooseLockSettingsHelper.utils();
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Find categories
        PreferenceCategory widgetsCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_WIDGETS_CATEGORY);
        // Find preferences
        mEnableKeyguardWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_WIDGETS);
        mEnableCameraWidget = (CheckBoxPreference) findPreference(KEY_ENABLE_CAMERA);
        mBatteryStatus = (ListPreference) findPreference(KEY_BATTERY_STATUS);

        // Enable or disable lockscreen widgets based on policy
        checkDisabledByPolicy(mEnableKeyguardWidgets,
                DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL);

        // Enable or disable camera widget based on device and policy
        if (Camera.getNumberOfCameras() == 0) {
            widgetsCategory.removePreference(mEnableCameraWidget);
            mEnableCameraWidget = null;
        } else if (mLockUtils.isSecure()) {
            checkDisabledByPolicy(mEnableCameraWidget,
                    DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA);
        }

        // Remove cLock settings item if not installed
        if (!isPackageInstalled("com.cyanogenmod.lockclock")) {
            widgetsCategory.removePreference(findPreference(KEY_LOCK_CLOCK));
        }

        // Remove maximize widgets on tablets
        if (!Utils.isPhone(getActivity())) {
            widgetsCategory.removePreference(
                    findPreference(Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS));
        }

        mLockscreenBackground = (PreferenceCategory) findPreference(LOCKSCREEN_BACKGROUND_CATEGORY);
        mLockBackground = (ListPreference) findPreference(LOCKSCREEN_BACKGROUND_STYLE);
        mWallpaperAlpha = (SeekBarPreference) findPreference(LOCKSCREEN_WALLPAPER_ALPHA);
        mLockColorFill = (ColorPickerPreference) findPreference(LOCKSCREEN_BACKGROUND_COLOR_FILL);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update custom widgets and camera
        if (mEnableKeyguardWidgets != null) {
            mEnableKeyguardWidgets.setChecked(mLockUtils.getWidgetsEnabled());
        }

        if (mEnableCameraWidget != null) {
            mEnableCameraWidget.setChecked(mLockUtils.getCameraEnabled());
        }

        setListeners();
        setDefaultValues();
        updateSummaries();
        updateVisiblePreferences();
    }

    protected void setListeners() {
        mLockBackground.setOnPreferenceChangeListener(this);
        mWallpaperAlpha.setOnPreferenceChangeListener(this);
        mLockColorFill.setOnPreferenceChangeListener(this);
        mBatteryStatus.setOnPreferenceChangeListener(this);
    }

    protected void setDefaultValues() {
        float mWallpaperAlphaTransparency = 1.0f;
        try {
            mWallpaperAlphaTransparency = Settings.System.getFloat(getContentResolver(),
                    Settings.System.LOCKSCREEN_WALLPAPER_ALPHA);
        } catch (Exception e) {
            mWallpaperAlphaTransparency = 1.0f;
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.LOCKSCREEN_WALLPAPER_ALPHA, 1.0f);
        }
        mWallpaperAlpha.setInitValue((int) (mWallpaperAlphaTransparency * 100));
        mLockBackground.setValue(Integer.toString(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 2)));
         mBatteryStatus.setValue(Integer.toString(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, 0)));
    }

    protected void updateSummaries() {
        mLockBackground.setSummary(mLockBackground.getEntry());
        mLockColorFill.setSummary(ColorPickerPreference.convertToARGB(
                Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND_COLOR, 0x00000000)));
        mBatteryStatus.setSummary(mBatteryStatus.getEntry());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        if (KEY_ENABLE_WIDGETS.equals(key)) {
            mLockUtils.setWidgetsEnabled(mEnableKeyguardWidgets.isChecked());
            return true;
        } else if (KEY_ENABLE_CAMERA.equals(key)) {
            mLockUtils.setCameraEnabled(mEnableCameraWidget.isChecked());
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mLockBackground) {
            int index = mLockBackground.findIndexOfValue(objValue.toString());
            preference.setSummary(mLockBackground.getEntries()[index]);
            return handleBackgroundSelection(index);
        } else if (preference == mWallpaperAlpha) {
            float value = Float.parseFloat((String) objValue);
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.LOCKSCREEN_WALLPAPER_ALPHA, value / 100);
            return true;
        } else if (preference == mLockColorFill) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int value = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND_COLOR, value);
            return true;
        } else if (preference == mBatteryStatus) {
            int value = Integer.valueOf((String) objValue);
            int index = mBatteryStatus.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, value);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[index]);
            return true;
        }
        return false;
    }

    /**
     * Checks if a specific policy is disabled by a device administrator, and disables the
     * provided preference if so.
     * @param preference Preference
     * @param feature Feature
     */
    private void checkDisabledByPolicy(Preference preference, int feature) {
        boolean disabled = featureIsDisabled(feature);

        if (disabled) {
            preference.setSummary(R.string.security_enable_widgets_disabled_summary);
        }

        preference.setEnabled(!disabled);
    }

    /**
     * Checks if a specific policy is disabled by a device administrator.
     * @param feature Feature
     * @return Is disabled
     */
    private boolean featureIsDisabled(int feature) {
        return (mDPM.getKeyguardDisabledFeatures(null) & feature) != 0;
    }


    private void updateVisiblePreferences() {
        int visible = Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 2);
        if (visible == 1) {
            mLockscreenBackground.addPreference(mWallpaperAlpha);
        } else {
            mLockscreenBackground.removePreference(mWallpaperAlpha);
        }
        if (visible == 0) {
            mLockscreenBackground.addPreference(mLockColorFill);
        } else {
            mLockscreenBackground.removePreference(mLockColorFill);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_WALLPAPER) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = null;
                if (data != null) {
                    uri = data.getData();
                    FileOutputStream out = null;
                    try {
                        Bitmap wallpaper = getBitmapFromUri(uri);
                        out = getActivity().openFileOutput(LOCKSCREEN_WALLPAPER_FILE_NAME,
                                Context.MODE_WORLD_READABLE);
                        wallpaper.compress(Bitmap.CompressFormat.JPEG, 85, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if(out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                    Toast.makeText(getActivity(), getResources().getString(R.string.
                            background_result_successful), Toast.LENGTH_LONG).show();
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 1);
                    updateVisiblePreferences();
                }
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.
                        background_result_not_successful), Toast.LENGTH_LONG).show();
            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    private boolean handleBackgroundSelection(int index) {
        if (index == COLOR_FILL) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 0);
            updateVisiblePreferences();
            return true;
        } else if (index == CUSTOM_IMAGE) {
            // Used to reset the image when already set
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 2);
            // Launches intent for user to select an image/crop it to set as background

            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            intent.putExtra("scaleType", 6);
            intent.putExtra("layout_width", -1);
            intent.putExtra("layout_height", -2);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

            final Display display = getActivity().getWindowManager().getDefaultDisplay();
            boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;

            int width = display.getWidth();
            int height = display.getHeight();

            intent.putExtra("aspectX", isPortrait ? width : height);
            intent.putExtra("aspectY", isPortrait ? height : width);

            try {
                intent.putExtra("return-data", false);
                getActivity().startActivityFromFragment(this, intent, REQUEST_PICK_WALLPAPER);
            } catch (ActivityNotFoundException e) {
            }
        } else if (index == DEFAULT) {
            // Sets background to default
            Settings.System.putInt(getContentResolver(),
                            Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 2);
            updateVisiblePreferences();
            return true;
        }
        return false;
    }
}
