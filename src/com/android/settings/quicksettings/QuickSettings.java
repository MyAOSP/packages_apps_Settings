/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.quicksettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.ColorPickerPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.util.cm.QSConstants;
import com.android.internal.util.cm.QSUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class QuickSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "QuickSettingsPanel";

    private static final String SEPARATOR = "OV=I=XseparatorX=I=VO";
    private static final String EXP_RING_MODE = "pref_ring_mode";
    private static final String EXP_NETWORK_MODE = "pref_network_mode";
    private static final String EXP_SCREENTIMEOUT_MODE = "pref_screentimeout_mode";
    private static final String QUICK_PULLDOWN = "quick_pulldown";
    private static final String GENERAL_SETTINGS = "pref_general_settings";
    private static final String STATIC_TILES = "static_tiles";
    private static final String DYNAMIC_TILES = "pref_dynamic_tiles";
    private static final String NUM_COLUMNS_PORT = "num_columns_port";
    private static final String NUM_COLUMNS_LAND = "num_columns_land";
    private static final String TILE_BACKGROUND_STYLE = "tile_background_style";
    private static final String TILE_BACKGROUND_COLOR = "tile_background_color";
    private static final String RANDOM_COLORS = "random_colors";
    private static final String TILE_TEXT_COLOR = "tile_text_color";
    private static final String QS_ANIM_SET = "qs_anim_set";

    private ColorPickerPreference mTileTextColor;
    private ColorPickerPreference mTileBgColor;
    private ListPreference mNumColumnsPort;
    private ListPreference mNumColumnsLand;
    private ListPreference mNetworkMode;
    private ListPreference mScreenTimeoutMode;
    private ListPreference mQuickPulldown;
    private ListPreference mTileBgStyle;
    private ListPreference mAnimSet;
    private MultiSelectListPreference mRingMode;
    private PreferenceScreen mRandomColors;
    private PreferenceCategory mGeneralSettings;
    private PreferenceCategory mStaticTiles;
    private PreferenceCategory mDynamicTiles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.quick_settings_panel);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        mGeneralSettings = (PreferenceCategory) prefSet.findPreference(GENERAL_SETTINGS);
        mStaticTiles = (PreferenceCategory) prefSet.findPreference(STATIC_TILES);
        mDynamicTiles = (PreferenceCategory) prefSet.findPreference(DYNAMIC_TILES);
        mQuickPulldown = (ListPreference) prefSet.findPreference(QUICK_PULLDOWN);
        mAnimSet = (ListPreference) prefSet.findPreference(QS_ANIM_SET);

        if (!Utils.isPhone(getActivity())) {
            if (mQuickPulldown != null) {
                mGeneralSettings.removePreference(mQuickPulldown);
            }
        }

        mTileTextColor = (ColorPickerPreference) findPreference(TILE_TEXT_COLOR);
        mNumColumnsPort = (ListPreference) prefSet.findPreference(NUM_COLUMNS_PORT);
        mNumColumnsLand = (ListPreference) prefSet.findPreference(NUM_COLUMNS_LAND);
        mTileBgStyle = (ListPreference) findPreference(TILE_BACKGROUND_STYLE);
        mTileBgColor = (ColorPickerPreference) findPreference(TILE_BACKGROUND_COLOR);
        mRandomColors = (PreferenceScreen) findPreference(RANDOM_COLORS);
        // Add the sound mode
        mRingMode = (MultiSelectListPreference) prefSet.findPreference(EXP_RING_MODE);
        // Add the network mode preference
        mNetworkMode = (ListPreference) prefSet.findPreference(EXP_NETWORK_MODE);
        // Screen timeout mode
        mScreenTimeoutMode = (ListPreference) prefSet.findPreference(EXP_SCREENTIMEOUT_MODE);

        // Remove unsupported options
        /* if (!QSUtils.deviceSupportsDockBattery(getActivity())) {
            Preference pref = findPreference(Settings.System.QS_DYNAMIC_DOCK_BATTERY);
            if (pref != null) {
                mDynamicTiles.removePreference(pref);
            }
        }*/
        if (!QSUtils.deviceSupportsImeSwitcher(getActivity())) {
            Preference pref = findPreference(Settings.System.QS_DYNAMIC_IME);
            if (pref != null) {
                mDynamicTiles.removePreference(pref);
            }
        }
        if (!QSUtils.deviceSupportsUsbTether(getActivity())) {
            Preference pref = findPreference(Settings.System.QS_DYNAMIC_USBTETHER);
            if (pref != null) {
                mDynamicTiles.removePreference(pref);
            }
        }
        if (!QSUtils.deviceSupportsWifiDisplay(getActivity())) {
            Preference pref = findPreference(Settings.System.QS_DYNAMIC_WIFI);
            if (pref != null) {
                mDynamicTiles.removePreference(pref);
            }
        }
    }

    private void registerListeners() {
        if (Utils.isPhone(getActivity())) {
            mQuickPulldown.setOnPreferenceChangeListener(this);
        }
        mTileTextColor.setOnPreferenceChangeListener(this);
        mNumColumnsPort.setOnPreferenceChangeListener(this);
        mNumColumnsLand.setOnPreferenceChangeListener(this);
        mRingMode.setOnPreferenceChangeListener(this);
        mScreenTimeoutMode.setOnPreferenceChangeListener(this);
        if (mNetworkMode != null) {
            mNetworkMode.setOnPreferenceChangeListener(this);
        }
        mTileBgStyle.setOnPreferenceChangeListener(this);
        mTileBgColor.setOnPreferenceChangeListener(this);
        mAnimSet.setOnPreferenceChangeListener(this);
    }

    private void setDefaultValues() {
        ContentResolver resolver = getContentResolver();
        mQuickPulldown.setValue(Integer.toString(Settings.System.getInt(
                resolver, Settings.System.QS_QUICK_PULLDOWN, 0)));
        mNumColumnsPort.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.QUICK_SETTINGS_NUM_COLUMNS_PORT, 3)));
        mNumColumnsLand.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.QUICK_SETTINGS_NUM_COLUMNS_LAND, 5)));
        mTileBgStyle.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.QUICK_SETTINGS_BACKGROUND_STYLE, 2)));
        mScreenTimeoutMode.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.EXPANDED_SCREENTIMEOUT_MODE, 1)));
        mNetworkMode.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.EXPANDED_NETWORK_MODE, 0)));
        mAnimSet.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.QS_ANIMATION_SET, 0)));
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            String storedRingMode = Settings.System.getString(resolver,
                    Settings.System.EXPANDED_RING_MODE);
            if (storedRingMode != null) {
                String[] ringModeArray = TextUtils.split(storedRingMode, SEPARATOR);
                mRingMode.setValues(new HashSet<String>(Arrays.asList(ringModeArray)));
            }
        }
    }

    private void updateSummaries() {
        ContentResolver resolver = getContentResolver();
        updatePulldownSummary(Settings.System.getInt(
                resolver, Settings.System.QS_QUICK_PULLDOWN, 0));
        mNumColumnsPort.setSummary(mNumColumnsPort.getEntry());
        mNumColumnsLand.setSummary(mNumColumnsLand.getEntry());
        mTileBgStyle.setSummary(mTileBgStyle.getEntry());
        mScreenTimeoutMode.setSummary(mScreenTimeoutMode.getEntry());
        mTileBgColor.setSummary(ColorPickerPreference.convertToARGB(Settings.System.getInt(
                resolver, Settings.System.QUICK_SETTINGS_BACKGROUND_COLOR, 0xFF1F1F1F)));
        mTileTextColor.setSummary(ColorPickerPreference.convertToARGB(Settings.System.getInt(
                resolver, Settings.System.QUICK_SETTINGS_TEXT_COLOR, 0xFFFFFFFF)));
        if (mNetworkMode != null) {
            mNetworkMode.setSummary(mNetworkMode.getEntry());
        }
        mAnimSet.setSummary(mAnimSet.getEntry());
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            String storedRingMode = Settings.System.getString(resolver,
                    Settings.System.EXPANDED_RING_MODE);
            if (storedRingMode != null) {
                updateSummary(storedRingMode, mRingMode, R.string.pref_ring_mode_summary);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        QuickSettingsUtil.updateAvailableTiles(getActivity());

        if (mNetworkMode != null) {
            if (QuickSettingsUtil.isTileAvailable(QSConstants.TILE_NETWORKMODE)) {
                mStaticTiles.addPreference(mNetworkMode);
            } else {
                mStaticTiles.removePreference(mNetworkMode);
            }
        }
        registerListeners();
        setDefaultValues();
        updateSummaries();
        updateVisibility();
    }

    private class MultiSelectListPreferenceComparator implements Comparator<String> {
        private MultiSelectListPreference pref;

        MultiSelectListPreferenceComparator(MultiSelectListPreference p) {
            pref = p;
        }

        @Override
        public int compare(String lhs, String rhs) {
            return Integer.compare(pref.findIndexOfValue(lhs),
                    pref.findIndexOfValue(rhs));
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getContentResolver();
        if (preference == mRingMode) {
            ArrayList<String> arrValue = new ArrayList<String>((Set<String>) newValue);
            Collections.sort(arrValue, new MultiSelectListPreferenceComparator(mRingMode));
            String value = TextUtils.join(SEPARATOR, arrValue);
            Settings.System.putString(resolver, Settings.System.EXPANDED_RING_MODE, value);
            updateSummary(value, mRingMode, R.string.pref_ring_mode_summary);
            return true;
        } else if (preference == mNetworkMode) {
            int value = Integer.valueOf((String) newValue);
            int index = mNetworkMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver, Settings.System.EXPANDED_NETWORK_MODE, value);
            mNetworkMode.setSummary(mNetworkMode.getEntries()[index]);
            return true;
        } else if (preference == mQuickPulldown) {
            int quickPulldownValue = Integer.valueOf((String) newValue);
            Settings.System.putInt(resolver, Settings.System.QS_QUICK_PULLDOWN,
                    quickPulldownValue);
            updatePulldownSummary(quickPulldownValue);
            return true;
        } else if (preference == mScreenTimeoutMode) {
            int value = Integer.valueOf((String) newValue);
            int index = mScreenTimeoutMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver, Settings.System.EXPANDED_SCREENTIMEOUT_MODE, value);
            mScreenTimeoutMode.setSummary(mScreenTimeoutMode.getEntries()[index]);
            return true;
        } else if (preference == mNumColumnsPort) {
            int value = Integer.parseInt((String) newValue);
            int index = mNumColumnsPort.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.QUICK_SETTINGS_NUM_COLUMNS_PORT, value);
            preference.setSummary(mNumColumnsPort.getEntries()[index]);
            return true;
        } else if (preference == mNumColumnsLand) {
            int value = Integer.parseInt((String) newValue);
            int index = mNumColumnsLand.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.QUICK_SETTINGS_NUM_COLUMNS_LAND, value);
            preference.setSummary(mNumColumnsLand.getEntries()[index]);
            return true;
        } else if (preference == mTileTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int value = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.QUICK_SETTINGS_TEXT_COLOR, value);
            return true;
        } else if (preference == mTileBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int value = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.QUICK_SETTINGS_BACKGROUND_COLOR, value);
            return true;
        } else if (preference == mTileBgStyle) {
            int value = Integer.valueOf((String) newValue);
            int index = mTileBgStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.QUICK_SETTINGS_BACKGROUND_STYLE, value);
            preference.setSummary(mTileBgStyle.getEntries()[index]);
            updateVisibility();
            return true;
        } else if (preference == mAnimSet) {
            int value = Integer.valueOf((String) newValue);
            int index = mAnimSet.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.QS_ANIMATION_SET, value);
            preference.setSummary(mAnimSet.getEntries()[index]);
            updateVisibility();
            return true;
        }
        return false;
    }

    private void updateVisibility() {
        ContentResolver resolver = getContentResolver();
        int visible = Settings.System.getInt(resolver,
                    Settings.System.QUICK_SETTINGS_BACKGROUND_STYLE, 2);
        switch (visible) {
            case 2:
            default:
                mGeneralSettings.removePreference(mRandomColors);
                mGeneralSettings.removePreference(mTileBgColor);
                break;
            case 1:
                mGeneralSettings.removePreference(mRandomColors);
                mGeneralSettings.addPreference(mTileBgColor);
                break;
            case 0:
                mGeneralSettings.addPreference(mRandomColors);
                mGeneralSettings.removePreference(mTileBgColor);
                break;
        }
    }

    private void updateSummary(String val, MultiSelectListPreference pref, int defSummary) {
        // Update summary message with current values
        final String[] values = parseStoredValue(val);
        if (values != null) {
            final int length = values.length;
            final CharSequence[] entries = pref.getEntries();
            StringBuilder summary = new StringBuilder();
            for (int i = 0; i < length; i++) {
                CharSequence entry = entries[Integer.parseInt(values[i])];
                if (i != 0) {
                    summary.append(" | ");
                }
                summary.append(entry);
            }
            pref.setSummary(summary);
        } else {
            pref.setSummary(defSummary);
        }
    }

    private void updatePulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // quick pulldown deactivated
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_off));
        } else {
            String direction = res.getString(value == 2
                    ? R.string.quick_pulldown_summary_left
                    : R.string.quick_pulldown_summary_right);
            mQuickPulldown.setSummary(res.getString(R.string.summary_quick_pulldown, direction));
        }
    }

    public static String[] parseStoredValue(CharSequence val) {
        if (TextUtils.isEmpty(val)) {
            return null;
        } else {
            return val.toString().split(SEPARATOR);
        }
    }
}