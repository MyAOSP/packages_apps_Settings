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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.ColorPickerPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class StatusBarBattery extends SettingsPreferenceFragment implements
            OnPreferenceChangeListener {

    private static final String TAG = "StatusBarBattery";

    private static final String BATTERY_STYLE = "battery_style";
    private static final String BATTERY_COLOR = "battery_color";
    private static final String BATTERY_TEXT_COLOR = "battery_text_color";
    private static final String BATTERY_TEXT_CHARGING_COLOR = "battery_text_charging_color";
    private static final String CIRCLE_BATTERY_ANIMATIONSPEED = "circle_battery_animation_speed";

    private static final int MENU_RESET = Menu.FIRST;

    private ColorPickerPreference mBatteryColor;
    private ColorPickerPreference mBatteryTextColor;
    private ColorPickerPreference mBatteryTextChargingColor;
    private ListPreference mStatusBarBattery;
    private ListPreference mCircleAnimSpeed;

    private PackageManager pm;
    private PreferenceScreen mPrefs;
    private Resources systemUiResources;
    private boolean mCheckPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCustomView();
    }

    private PreferenceScreen createCustomView() {
        mCheckPreferences = false;

        addPreferencesFromResource(R.xml.status_bar_battery);
        mPrefs = getPreferenceScreen();

        mStatusBarBattery = (ListPreference) findPreference(BATTERY_STYLE);
        mBatteryColor = (ColorPickerPreference) findPreference(BATTERY_COLOR);
        mBatteryTextColor = (ColorPickerPreference) findPreference(BATTERY_TEXT_COLOR);
        mBatteryTextChargingColor = (ColorPickerPreference) findPreference(
                BATTERY_TEXT_CHARGING_COLOR);
        mCircleAnimSpeed = (ListPreference) findPreference(CIRCLE_BATTERY_ANIMATIONSPEED);

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return mPrefs;
    }

    @Override
    public void onResume() {
        super.onResume();
        setListeners();
        setDefaultValues();
        updateSummaries();
        updateBatteryStyleOptions();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.reset);
        alertDialog.setMessage(R.string.battery_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                iconColorReset();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void iconColorReset() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_COLOR, Color.WHITE);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR, Color.WHITE);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR, Color.RED);
    }

    protected void setListeners() {
        mStatusBarBattery.setOnPreferenceChangeListener(this);
        mBatteryColor.setOnPreferenceChangeListener(this);
        mBatteryTextColor.setOnPreferenceChangeListener(this);
        mBatteryTextChargingColor.setOnPreferenceChangeListener(this);
        mCircleAnimSpeed.setOnPreferenceChangeListener(this);
    }

    protected void setDefaultValues() {
        mStatusBarBattery.setValue(Integer.toString(Settings.System.getInt(
                getActivity().getContentResolver(), Settings.System.STATUS_BAR_BATTERY, 0)));
        mCircleAnimSpeed.setValue(Integer.toString(Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED, 3)));
    }

    protected void updateSummaries() {
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mBatteryColor.setSummary(ColorPickerPreference.convertToARGB(
                Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_COLOR, Color.WHITE)));
        mBatteryTextColor.setSummary(ColorPickerPreference.convertToARGB(
                Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR, Color.WHITE)));
        mBatteryTextChargingColor.setSummary(ColorPickerPreference.convertToARGB(
                Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR, Color.RED)));
        mCircleAnimSpeed.setSummary(mCircleAnimSpeed.getEntry());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mStatusBarBattery) {
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, value);
            updateBatteryStyleOptions();
            return true;
        } else if (preference == mBatteryColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int value = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_COLOR, value);
            return true;
        } else if (preference == mBatteryTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int value = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR, value);
            return true;
        } else if (preference == mBatteryTextChargingColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int value = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR, value);
            return true;
        } else if (preference == mCircleAnimSpeed) {
            int index = Integer.parseInt((String) newValue);
            mCircleAnimSpeed.setSummary(mCircleAnimSpeed.getEntries()[index]);
            int value = mCircleAnimSpeed.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED, value);
            return true;
        }
        return false;
    }

    private void updateBatteryStyleOptions() {
        mBatteryTextChargingColor.setTitle(R.string.battery_text_charging_color);
        int style = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY, 0);
        switch (style) {
            case 0: // Stock icon
            default:
                mPrefs.addPreference(mBatteryColor);
                mPrefs.addPreference(mBatteryTextChargingColor);
                mPrefs.removePreference(mBatteryTextColor);
                mPrefs.removePreference(mCircleAnimSpeed);
                mBatteryTextChargingColor.setTitle(R.string.battery_bolt_color);
                break;
            case 1: // Text % only
                mPrefs.removePreference(mBatteryColor);
                mPrefs.addPreference(mBatteryTextColor);
                mPrefs.addPreference(mBatteryTextChargingColor);
                mPrefs.removePreference(mCircleAnimSpeed);
                break;
            case 2: // Stock icon with text
                mPrefs.addPreference(mBatteryColor);
                mPrefs.addPreference(mBatteryTextColor);
                mPrefs.addPreference(mBatteryTextChargingColor);
                mPrefs.removePreference(mCircleAnimSpeed);
                break;
            case 3: // Circle battery
                mPrefs.addPreference(mBatteryColor);
                mPrefs.removePreference(mBatteryTextColor);
                mPrefs.removePreference(mBatteryTextChargingColor);
                mPrefs.addPreference(mCircleAnimSpeed);
                break;
            case 4: // Circle battery with percent
                mPrefs.addPreference(mBatteryColor);
                mPrefs.addPreference(mBatteryTextColor);
                mPrefs.addPreference(mBatteryTextChargingColor);
                mPrefs.addPreference(mCircleAnimSpeed);
                break;
            case 5: // Dotted circle
                mPrefs.addPreference(mBatteryColor);
                mPrefs.removePreference(mBatteryTextColor);
                mPrefs.removePreference(mBatteryTextChargingColor);
                mPrefs.addPreference(mCircleAnimSpeed);
                break;
            case 6:  // Dotted circle with percent
                mPrefs.addPreference(mBatteryColor);
                mPrefs.addPreference(mBatteryTextColor);
                mPrefs.addPreference(mBatteryTextChargingColor);
                mPrefs.addPreference(mCircleAnimSpeed);
                break;
            case 7:  // Hidden only in the statusbar
                mPrefs.addPreference(mBatteryColor);
                mPrefs.addPreference(mBatteryTextColor);
                mPrefs.removePreference(mBatteryTextChargingColor);
                mPrefs.removePreference(mCircleAnimSpeed);
                break;

        }
    }
}
