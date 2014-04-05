/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.util.cm.QSConstants;
import com.android.internal.util.cm.QSUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.quicksettings.QuickSettingsUtil.TileInfo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class QuickSettingsTiles extends Fragment {

    private static final int MENU_RESET = Menu.FIRST;

    private DraggableGridView mDragView;
    private ViewGroup mContainer;
    private LayoutInflater mInflater;
    private static Resources mSystemUiResources;
    private TileAdapter mTileAdapter;
    private boolean mConfigRibbon;
    private int mTileTextSize = 12;
    private Context mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDragView = new DraggableGridView(getActivity());
        mContainer = container;
        mContainer.setClipChildren(false);
        mContainer.setClipToPadding(false);
        mInflater = inflater;

        QuickSettingsUtil.removeUnsupportedTiles(getActivity());

        // We have both a panel and the ribbon config, see which one we are using
        Bundle args = getArguments();
        if (args != null) {
            mConfigRibbon = args.getBoolean("config_ribbon");
        }

        PackageManager pm = getActivity().getPackageManager();
        if (pm != null) {
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
                mSystemUiResources = null;
            }
        }
        int panelWidth = getItemFromSystemUi("notification_panel_width", "dimen");
        if (panelWidth > 0) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(panelWidth,
                    FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
            mDragView.setLayoutParams(params);
        }
        int cellGap = getItemFromSystemUi("quick_settings_cell_gap", "dimen");
        if (cellGap != 0) {
            mDragView.setCellGap(cellGap);
        }
        mTileAdapter = new TileAdapter(getActivity(), mConfigRibbon);
        return mDragView;
    }

    public static int getItemFromSystemUi(String name, String type) {
        if (mSystemUiResources != null) {
            int resId = (int) mSystemUiResources.getIdentifier(name, type, "com.android.systemui");
            if (resId > 0) {
                try {
                    if (type.equals("dimen")) {
                        return (int) mSystemUiResources.getDimension(resId);
                    } else {
                        return mSystemUiResources.getInteger(resId);
                    }
                } catch (NotFoundException e) {
                }
            }
        }
        return 0;
    }

    void genTiles() {
        mDragView.removeAllViews();
        updateTilesPerRow();
        ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
        for (String tileindex : tiles) {
            QuickSettingsUtil.TileInfo tile = QuickSettingsUtil.TILES.get(tileindex);
            if (tile != null) {
                addTile(tile.getTitleResId(), tile.getIcon(), 0, false);
            }
        }
        addTile(R.string.profiles_add, null, R.drawable.ic_menu_add, false);
    }

    /**
     * Adds a tile to the dragview
     * @param titleId - string id for tile text in systemui
     * @param iconSysId - resource id for icon in systemui
     * @param iconRegId - resource id for icon in local package
     * @param newTile - whether a new tile is being added by user
     */
    void addTile(int titleId, String iconSysId, int iconRegId, boolean newTile) {
        View tileView = null;
        if (iconRegId != 0) {
            tileView = (View) mInflater.inflate(R.layout.quick_settings_tile_generic, null, false);
            final TextView name = (TextView) tileView.findViewById(R.id.tile_textview);
            name.setText(titleId);
            name.setTextSize(1, mTileTextSize);
            name.setTextColor(QSUtils.getTileTextColor(mContext));
            name.setCompoundDrawablesRelativeWithIntrinsicBounds(0, iconRegId, 0, 0);
        } else {
            final boolean isUserTile = titleId == QuickSettingsUtil.TILES.get(QSConstants.TILE_USER).getTitleResId();
            if (mSystemUiResources != null && iconSysId != null) {
                int resId = mSystemUiResources.getIdentifier(iconSysId, null, null);
                if (resId > 0) {
                    try {
                        Drawable d = mSystemUiResources.getDrawable(resId);
                        tileView = null;
                        if (isUserTile) {
                            tileView = (View) mInflater.inflate(R.layout.quick_settings_tile_user, null, false);
                            ImageView iv = (ImageView) tileView.findViewById(R.id.user_imageview);
                            TextView tv = (TextView) tileView.findViewById(R.id.tile_textview);
                            tv.setText(titleId);
                            tv.setTextSize(1, mTileTextSize);
                            tv.setTextColor(QSUtils.getTileTextColor(mContext));
                            iv.setImageDrawable(d);
                        } else {
                            tileView = (View) mInflater.inflate(R.layout.quick_settings_tile_generic, null, false);
                            final TextView name = (TextView) tileView.findViewById(R.id.tile_textview);
                            name.setText(titleId);
                            name.setTextSize(1, mTileTextSize);
                            name.setTextColor(QSUtils.getTileTextColor(mContext));
                            name.setCompoundDrawablesRelativeWithIntrinsicBounds(null, d, null, null);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        setTileBackground(mContext, tileView);
        mDragView.addView(tileView, newTile ? mDragView.getChildCount() - 1 : mDragView.getChildCount());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity().getApplicationContext();
        genTiles();
        mDragView.setOnRearrangeListener(new DraggableGridView.OnRearrangeListener() {
            public void onRearrange(int oldIndex, int newIndex) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                        QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
                String oldTile = tiles.get(oldIndex);
                tiles.remove(oldIndex);
                tiles.add(newIndex, oldTile);
                QuickSettingsUtil.saveCurrentTiles(getActivity(),
                        QuickSettingsUtil.getTileStringFromList(tiles), mConfigRibbon);
            }
            @Override
            public void onDelete(int index) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                        QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
                tiles.remove(index);
                QuickSettingsUtil.saveCurrentTiles(getActivity(),
                        QuickSettingsUtil.getTileStringFromList(tiles), mConfigRibbon);
            }
        });
        mDragView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 != mDragView.getChildCount() - 1) return;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.tile_choose_title)
                .setAdapter(mTileAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, final int position) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<String> curr = QuickSettingsUtil.getTileListFromString(
                                        QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
                                curr.add(mTileAdapter.getTileId(position));
                                QuickSettingsUtil.saveCurrentTiles(getActivity(),
                                        QuickSettingsUtil.getTileStringFromList(curr), mConfigRibbon);
                            }
                        }).start();
                        TileInfo info = QuickSettingsUtil.TILES.get(mTileAdapter.getTileId(position));
                        addTile(info.getTitleResId(), info.getIcon(), 0, true);
                    }
                });
                builder.create().show();
            }
        });

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Utils.isPhone(getActivity())) {
            mContainer.setPadding(20, 0, 20, 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setAlphabeticShortcut('r')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetTiles();
                return true;
            default:
                return false;
        }
    }

    private void resetTiles() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.reset);
        alert.setMessage(R.string.tiles_reset_message);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                QuickSettingsUtil.resetTiles(getActivity(), mConfigRibbon);
                genTiles();
            }
        });
        alert.setNegativeButton(R.string.cancel, null);
        alert.create().show();
    }

    private static class TileAdapter extends ArrayAdapter<String> {
        private static class Entry {
            public final TileInfo tile;
            public final String tileTitle;
            public Entry(TileInfo tile, String tileTitle) {
                this.tile = tile;
                this.tileTitle = tileTitle;
            }
        }

        private Entry[] mTiles;
        private boolean mIsRibbon;

        public TileAdapter(Context context, boolean isRibbon) {
            super(context, android.R.layout.simple_list_item_1);
            mTiles = new Entry[getCount()];
            mIsRibbon = isRibbon;
            loadItems(context.getResources());
            sortItems();
        }

        private void loadItems(Resources resources) {
            int index = 0;
            for (TileInfo t : QuickSettingsUtil.TILES.values()) {
                mTiles[index++] = new Entry(t, resources.getString(t.getTitleResId()));
            }
        }

        private void sortItems() {
            final Collator collator = Collator.getInstance();
            collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            collator.setStrength(Collator.PRIMARY);
            Arrays.sort(mTiles, new Comparator<Entry>() {
                @Override
                public int compare(Entry e1, Entry e2) {
                    return collator.compare(e1.tileTitle, e2.tileTitle);
                }
            });
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            v.setEnabled(isEnabled(position));
            return v;
        }

        @Override
        public int getCount() {
            return QuickSettingsUtil.TILES.size();
        }

        @Override
        public String getItem(int position) {
            return mTiles[position].tileTitle;
        }

        public String getTileId(int position) {
            return mTiles[position].tile.getId();
        }

        @Override
        public boolean isEnabled(int position) {
            String usedTiles = QuickSettingsUtil.getCurrentTiles(
                    getContext(), mIsRibbon);
            return !(usedTiles.contains(mTiles[position].tile.getId()));
        }
    }

    void updateTileTextSize(int column) {
        // adjust Tile Text Size based on column count
        switch (column) {
            case 7:
                mTileTextSize = 8;
                break;
            case 6:
                mTileTextSize = 8;
                break;
            case 5:
                mTileTextSize = 9;
                break;
            case 4:
                mTileTextSize = 10;
                break;
            case 3:
            default:
                mTileTextSize = 12;
                break;
            case 2:
                mTileTextSize = 14;
                break;
            case 1:
                mTileTextSize = 16;
                break;
        }
    }

    private void updateTilesPerRow() {
        boolean mPortrait = mContext.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
        int columnCount = getItemFromSystemUi("quick_settings_num_columns", "integer");
        if (mPortrait) {
            columnCount = QSUtils.getMaxColumns(mContext, Configuration.ORIENTATION_PORTRAIT);
        } else {
            columnCount = QSUtils.getMaxColumns(mContext, Configuration.ORIENTATION_LANDSCAPE);
        }
        if (columnCount != 0) {
            mDragView.setColumnCount(columnCount);
        }
        updateTileTextSize(columnCount);
    }

    public static void setTileBackground(Context ctx, View v) {
        ContentResolver resolver = ctx.getContentResolver();
        int tileBg = Settings.System.getInt(resolver,
                Settings.System.QUICK_SETTINGS_BACKGROUND_STYLE, 2);
        int blue = Settings.System.getInt(resolver,
                Settings.System.RANDOM_COLOR_ONE, com.android.internal.R.color.holo_blue_dark);
        int green = Settings.System.getInt(resolver,
                Settings.System.RANDOM_COLOR_TWO, com.android.internal.R.color.holo_green_dark);
        int red = Settings.System.getInt(resolver,
                Settings.System.RANDOM_COLOR_THREE, com.android.internal.R.color.holo_red_dark);
        int orange = Settings.System.getInt(resolver,
                Settings.System.RANDOM_COLOR_FOUR, com.android.internal.R.color.holo_orange_dark);
        int purple = Settings.System.getInt(resolver,
                Settings.System.RANDOM_COLOR_FIVE, com.android.internal.R.color.holo_purple);
        int blueBright = Settings.System.getInt(resolver,
                Settings.System.RANDOM_COLOR_SIX, com.android.internal.R.color.holo_blue_bright);
        switch (tileBg) {
            case 0:
                int[] colors = new int[] {blue, green, red, orange, purple, blueBright};
                Random generator = new Random();
                v.setBackgroundColor(colors[generator.nextInt(colors.length)]);
                break;
            case 1:
                int tileBgColor = Settings.System.getInt(resolver,
                        Settings.System.QUICK_SETTINGS_BACKGROUND_COLOR, 0xFF000000);
                v.setBackgroundColor(tileBgColor);
                break;
            case 2:
            default:
                v.setBackgroundResource(R.drawable.qs_tile_background);
                break;
        }
    }
}
