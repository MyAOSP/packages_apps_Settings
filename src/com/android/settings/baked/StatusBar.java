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
import android.content.DialogInterface.OnCancelListener;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ColorPickerPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.Date;

public class StatusBar extends SettingsPreferenceFragment implements
            OnPreferenceChangeListener {

    private static final String TAG = "StatusBar";
    // Preference category
    private static final String STATUS_BAR_BATTERY_SETTINGS = "status_bar_battery_settings";
    private static final String STATUS_BAR_SIGNAL_SETTINGS = "status_bar_signal_settings";
    private static final String STATUS_BAR_MISC_SETTINGS = "status_bar_misc_settings";

    // Battery
    private static final String BATTERY_STYLE = "battery_style";
    private static final String BATTERY_COLOR = "battery_color";
    private static final String BATTERY_TEXT_COLOR = "battery_text_color";
    private static final String BATTERY_TEXT_CHARGING_COLOR = "battery_text_charging_color";
    private static final String CIRCLE_BATTERY_ANIMATIONSPEED = "circle_battery_animation_speed";
    // Clock
    private static final String PREF_ENABLE = "clock_style";
    private static final String PREF_AM_PM_STYLE = "status_bar_am_pm";
    private static final String PREF_COLOR_PICKER = "clock_color";
    private static final String PREF_CLOCK_DATE_DISPLAY = "clock_date_display";
    private static final String PREF_CLOCK_DATE_STYLE = "clock_date_style";
    private static final String PREF_CLOCK_DATE_FORMAT = "clock_date_format";
    // Signal
    private static final String STATUS_BAR_SIGNAL = "status_bar_signal";

    private static final int MENU_RESET = Menu.FIRST;

    public static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    public static final int CLOCK_DATE_STYLE_UPPERCASE = 2;
    private static final int CUSTOM_CLOCK_DATE_FORMAT_INDEX = 18;

    private CheckBoxPreference mStatusBarBrightnessControl;
    private ColorPickerPreference mBatteryColor;
    private ColorPickerPreference mBatteryTextColor;
    private ColorPickerPreference mBatteryTextChargingColor;
    private ColorPickerPreference mColorPicker;
    private ListPreference mStatusBarBattery;
    private ListPreference mCircleAnimSpeed;
    private ListPreference mClockStyle;
    private ListPreference mClockAmPmStyle;
    private ListPreference mClockDateDisplay;
    private ListPreference mClockDateStyle;
    private ListPreference mClockDateFormat;
    private ListPreference mStatusBarSignal;

    private PackageManager pm;
    private PreferenceScreen mPrefs;
    private Resources systemUiResources;
    private boolean mCheckPreferences;

    private PreferenceCategory mBatteryCategory;
    private PreferenceCategory mSignalCategory;
    private PreferenceCategory mMiscCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCustomView();
    }

    private PreferenceScreen createCustomView() {
        mCheckPreferences = false;

        addPreferencesFromResource(R.xml.status_bar);
        mPrefs = getPreferenceScreen();
        mBatteryCategory = (PreferenceCategory) findPreference(STATUS_BAR_BATTERY_SETTINGS);
        mSignalCategory = (PreferenceCategory) findPreference(STATUS_BAR_SIGNAL_SETTINGS);
        mMiscCategory = (PreferenceCategory) findPreference(STATUS_BAR_MISC_SETTINGS);

        mStatusBarBattery = (ListPreference) findPreference(BATTERY_STYLE);
        mBatteryColor = (ColorPickerPreference) findPreference(BATTERY_COLOR);
        mBatteryTextColor = (ColorPickerPreference) findPreference(BATTERY_TEXT_COLOR);
        mBatteryTextChargingColor = (ColorPickerPreference) findPreference(
                BATTERY_TEXT_CHARGING_COLOR);
        mCircleAnimSpeed = (ListPreference) findPreference(CIRCLE_BATTERY_ANIMATIONSPEED);
        mClockStyle = (ListPreference) findPreference(PREF_ENABLE);
        mClockAmPmStyle = (ListPreference) findPreference(PREF_AM_PM_STYLE);
        mColorPicker = (ColorPickerPreference) findPreference(PREF_COLOR_PICKER);
        mClockDateDisplay = (ListPreference) findPreference(PREF_CLOCK_DATE_DISPLAY);
        mClockDateStyle = (ListPreference) findPreference(PREF_CLOCK_DATE_STYLE);
        mClockDateFormat = (ListPreference) findPreference(PREF_CLOCK_DATE_FORMAT);
        mStatusBarSignal = (ListPreference) findPreference(STATUS_BAR_SIGNAL);
        mStatusBarBrightnessControl = (CheckBoxPreference)
                findPreference(Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL);

        if (mClockDateFormat.getValue() == null) {
            mClockDateFormat.setValue("EEE");
        }

        parseClockDateFormats();

        if (Utils.isWifiOnly(getActivity())) {
            mSignalCategory.removePreference(mStatusBarSignal);
            mPrefs.removePreference(mSignalCategory);
        }

        if (Utils.isTablet(getActivity())) {
            mMiscCategory.removePreference(mStatusBarBrightnessControl);
        }

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return mPrefs;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCheckPreferences) {
            setListeners();
            setDefaultValues();
            updateSummaries();
            updateBatteryStyleOptions();
            updateVisibility();
        }
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
                Settings.System.putInt(getContentResolver(),
                        Settings.System.STATUSBAR_CLOCK_COLOR, -2);
                updateSummaries();
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
        mClockStyle.setOnPreferenceChangeListener(this);
        mClockAmPmStyle.setOnPreferenceChangeListener(this);
        mColorPicker.setOnPreferenceChangeListener(this);
        mClockDateDisplay.setOnPreferenceChangeListener(this);
        mClockDateStyle.setOnPreferenceChangeListener(this);
        mClockDateFormat.setOnPreferenceChangeListener(this);
        mStatusBarSignal.setOnPreferenceChangeListener(this);
    }

    protected void setDefaultValues() {
        mStatusBarBattery.setValue(Integer.toString(Settings.System.getInt(
                getActivity().getContentResolver(), Settings.System.STATUS_BAR_BATTERY, 0)));
        mCircleAnimSpeed.setValue(Integer.toString(Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED, 3)));
        mClockStyle.setValue(Integer.toString(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUSBAR_CLOCK_STYLE, 0)));
        mClockAmPmStyle.setValue(Integer.toString(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUSBAR_CLOCK_AM_PM_STYLE, 0)));
        mClockDateDisplay.setValue(Integer.toString(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUSBAR_CLOCK_DATE_DISPLAY, 0)));
        mClockDateStyle.setValue(Integer.toString(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUSBAR_CLOCK_DATE_STYLE, 2)));
        mStatusBarSignal.setValue(Integer.toString(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_SIGNAL_TEXT, 0)));
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
        mClockStyle.setSummary(mClockStyle.getEntry());
        mColorPicker.setSummary(ColorPickerPreference.convertToARGB(
                Settings.System.getInt(getContentResolver(),
                Settings.System.STATUSBAR_CLOCK_COLOR, com.android.internal.R.color.white)));
        mClockDateDisplay.setSummary(mClockDateDisplay.getEntry());
        mClockDateStyle.setSummary(mClockDateStyle.getEntry());
        mStatusBarSignal.setSummary(mStatusBarSignal.getEntry());
        try {
            if (Settings.System.getInt(getContentResolver(),
                    Settings.System.TIME_12_24) == 24) {
                mClockAmPmStyle.setEnabled(false);
                mClockAmPmStyle.setSummary(R.string.status_bar_am_pm_info);
            } else {
                mClockAmPmStyle.setSummary(mClockAmPmStyle.getEntry());
            }
        } catch (SettingNotFoundException e ) {
        }
    }

    private void updateVisibility() {
        boolean mClockDateToggle = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUSBAR_CLOCK_DATE_DISPLAY, 0) != 0;
        if (!mClockDateToggle) {
            mClockDateStyle.setEnabled(false);
            mClockDateFormat.setEnabled(false);
        } else {
            mClockDateStyle.setEnabled(true);
            mClockDateFormat.setEnabled(true);
        }

        int style = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUSBAR_CLOCK_STYLE, 0);
        if (style == 2) {
            mClockAmPmStyle.setEnabled(false);
            mColorPicker.setEnabled(false);
            mClockDateDisplay.setEnabled(false);
            mClockDateStyle.setEnabled(false);
            mClockDateFormat.setEnabled(false);
        } else {
            mClockAmPmStyle.setEnabled(true);
            mColorPicker.setEnabled(true);
            mClockDateDisplay.setEnabled(true);
            if (mClockDateToggle) {
                mClockDateStyle.setEnabled(true);
                mClockDateFormat.setEnabled(true);
            }
        }

        try {
            if (Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE)
                    == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                mStatusBarBrightnessControl.setEnabled(false);
                mStatusBarBrightnessControl.setSummary(R.string.status_bar_toggle_info);
            }
        } catch (SettingNotFoundException e) {
            // Do nothing
        }
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
        } else if (preference == mClockAmPmStyle) {
            int index = mClockAmPmStyle.findIndexOfValue((String) newValue);
            mClockAmPmStyle.setSummary(mClockAmPmStyle.getEntries()[index]);
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_AM_PM_STYLE, value);
            return true;
        } else if (preference == mClockStyle) {
            int index = mClockStyle.findIndexOfValue((String) newValue);
            mClockStyle.setSummary(mClockStyle.getEntries()[index]);
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_STYLE, value);
            updateVisibility();
            return true;
        } else if (preference == mColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            preference.setSummary(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_COLOR, intHex);
            return true;
        } else if (preference == mClockDateDisplay) {
            int index = mClockDateDisplay.findIndexOfValue((String) newValue);
            mClockDateDisplay.setSummary(mClockDateDisplay.getEntries()[index]);
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_DATE_DISPLAY, value);
            updateVisibility();
            return true;
        } else if (preference == mStatusBarSignal) {
            int index = mStatusBarSignal.findIndexOfValue((String) newValue);
            mStatusBarSignal.setSummary(mStatusBarSignal.getEntries()[index]);
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_SIGNAL_TEXT, value);
            return true;
          } else if (preference == mClockDateStyle) {
            int index = mClockDateStyle.findIndexOfValue((String) newValue);
            mClockDateStyle.setSummary(mClockDateStyle.getEntries()[index]);
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_DATE_STYLE, value);
            parseClockDateFormats();
            return true;
        } else if (preference == mClockDateFormat) {
            AlertDialog dialog;
            int index = mClockDateFormat.findIndexOfValue((String) newValue);
            if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.clock_date_string_edittext_title);
                alert.setMessage(R.string.clock_date_string_edittext_summary);
                final EditText input = new EditText(getActivity());
                String oldText = Settings.System.getString(getContentResolver(),
                        Settings.System.STATUSBAR_CLOCK_DATE_FORMAT);
                if (oldText != null) {
                    input.setText(oldText);
                }
                alert.setView(input);
                alert.setPositiveButton(R.string.menu_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int whichButton) {
                        String value = input.getText().toString();
                        if (value.equals("")) {
                            return;
                        }
                        Settings.System.putString(getContentResolver(),
                            Settings.System.STATUSBAR_CLOCK_DATE_FORMAT, value);
                        return;
                    }
                });
                alert.setNegativeButton(R.string.menu_cancel,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int which) {
                        return;
                    }
                });
                dialog = alert.create();
                dialog.show();
            } else {
                if ((String) newValue != null) {
                    Settings.System.putString(getContentResolver(),
                        Settings.System.STATUSBAR_CLOCK_DATE_FORMAT, (String) newValue);
                }
            }
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
                mBatteryCategory.addPreference(mBatteryColor);
                mBatteryCategory.addPreference(mBatteryTextChargingColor);
                mBatteryCategory.removePreference(mBatteryTextColor);
                mBatteryCategory.removePreference(mCircleAnimSpeed);
                mBatteryTextChargingColor.setTitle(R.string.battery_bolt_color);
                break;
            case 1: // Text % only
                mBatteryCategory.removePreference(mBatteryColor);
                mBatteryCategory.addPreference(mBatteryTextColor);
                mBatteryCategory.addPreference(mBatteryTextChargingColor);
                mBatteryCategory.removePreference(mCircleAnimSpeed);
                break;
            case 2: // Stock icon with text
                mBatteryCategory.addPreference(mBatteryColor);
                mBatteryCategory.addPreference(mBatteryTextColor);
                mBatteryCategory.addPreference(mBatteryTextChargingColor);
                mBatteryCategory.removePreference(mCircleAnimSpeed);
                break;
            case 3: // Circle battery
                mBatteryCategory.addPreference(mBatteryColor);
                mBatteryCategory.removePreference(mBatteryTextColor);
                mBatteryCategory.removePreference(mBatteryTextChargingColor);
                mBatteryCategory.addPreference(mCircleAnimSpeed);
                break;
            case 4: // Circle battery with percent
                mBatteryCategory.addPreference(mBatteryColor);
                mBatteryCategory.addPreference(mBatteryTextColor);
                mBatteryCategory.addPreference(mBatteryTextChargingColor);
                mBatteryCategory.addPreference(mCircleAnimSpeed);
                break;
            case 5: // Dotted circle
                mBatteryCategory.addPreference(mBatteryColor);
                mBatteryCategory.removePreference(mBatteryTextColor);
                mBatteryCategory.removePreference(mBatteryTextChargingColor);
                mBatteryCategory.addPreference(mCircleAnimSpeed);
                break;
            case 6:  // Dotted circle with percent
                mBatteryCategory.addPreference(mBatteryColor);
                mBatteryCategory.addPreference(mBatteryTextColor);
                mBatteryCategory.addPreference(mBatteryTextChargingColor);
                mBatteryCategory.addPreference(mCircleAnimSpeed);
                break;
            case 7:  // Hidden only in the statusbar
                mBatteryCategory.addPreference(mBatteryColor);
                mBatteryCategory.addPreference(mBatteryTextColor);
                mBatteryCategory.removePreference(mBatteryTextChargingColor);
                mBatteryCategory.removePreference(mCircleAnimSpeed);
                break;

        }
    }

    private void parseClockDateFormats() {
        // Parse and repopulate mClockDateFormats's entries based on current date.
        String[] dateEntries = getResources().getStringArray(R.array.clock_date_format_entries_values);
        CharSequence parsedDateEntries[];
        parsedDateEntries = new String[dateEntries.length];
        Date now = new Date();

        int lastEntry = dateEntries.length - 1;
        int dateFormat = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUSBAR_CLOCK_DATE_STYLE, 2);
        for (int i = 0; i < dateEntries.length; i++) {
            if (i == lastEntry) {
                parsedDateEntries[i] = dateEntries[i];
            } else {
                String newDate;
                CharSequence dateString = DateFormat.format(dateEntries[i], now);
                if (dateFormat == CLOCK_DATE_STYLE_LOWERCASE) {
                    newDate = dateString.toString().toLowerCase();
                } else if (dateFormat == CLOCK_DATE_STYLE_UPPERCASE) {
                    newDate = dateString.toString().toUpperCase();
                } else {
                    newDate = dateString.toString();
                }

                parsedDateEntries[i] = newDate;
            }
        }
        mClockDateFormat.setEntries(parsedDateEntries);
    }
}
