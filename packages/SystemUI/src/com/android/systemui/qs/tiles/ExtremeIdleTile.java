/*
 * Copyright (C) 2018 The OmniROM Project
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

package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;

import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.R;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class ExtremeIdleTile extends QSTileImpl<BooleanState> {
    private boolean mExtremeIdleDisabled;
    private boolean mAggressiveIdleDisabled;
    private boolean mListening;
    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_qs_extreme_idle);

    public ExtremeIdleTile(QSHost host) {
        super(host);
        mAggressiveIdleDisabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.BAIKALOS_AGGRESSIVE_IDLE, 0) == 0;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleClick() {
        mExtremeIdleDisabled = !mExtremeIdleDisabled;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BAIKALOS_EXTREME_IDLE, mExtremeIdleDisabled ? 0 : 1);


	if( mExtremeIdleDisabled ) {
        	Settings.Global.putInt(mContext.getContentResolver(),
                	Settings.Global.BAIKALOS_AGGRESSIVE_IDLE, mAggressiveIdleDisabled? 0:1);
	} else {
        	mAggressiveIdleDisabled = Settings.Global.getInt(mContext.getContentResolver(),
                	Settings.Global.BAIKALOS_AGGRESSIVE_IDLE, 0) == 0;
        	Settings.Global.putInt(mContext.getContentResolver(),
                	Settings.Global.BAIKALOS_AGGRESSIVE_IDLE, 0);
	}

        Settings.Global.putInt(mContext.getContentResolver(),
		Settings.Global.FORCED_APP_STANDBY_ENABLED, mExtremeIdleDisabled ? 0:1);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.FORCED_APP_STANDBY_FOR_SMALL_BATTERY_ENABLED, mExtremeIdleDisabled ? 0:1);

        refreshState();
    }


    @Override
    public Intent getLongClickIntent() {
        /*return new Intent().setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$AmbientDisplaySettingsActivity"));*/
	return null;
    }


    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_extreme_idle_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (state.slash == null) {
            state.slash = new SlashState();
        }
        mExtremeIdleDisabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.BAIKALOS_EXTREME_IDLE, 0) == 0;
        state.icon = mIcon;
        state.value = mExtremeIdleDisabled;
        state.slash.isSlashed = state.value;
        state.label = mContext.getString(R.string.quick_settings_extreme_idle_label);
        if (mExtremeIdleDisabled) {
            state.state = Tile.STATE_INACTIVE;
        } else {
            state.state = Tile.STATE_ACTIVE;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EXTENSIONS;
    }

    private ContentObserver mObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            refreshState();
        }
    };

    @Override
    public void handleSetListening(boolean listening) {
        if (mObserver == null) {
            return;
        }
        if (mListening != listening) {
            mListening = listening;
            if (listening) {
                mContext.getContentResolver().registerContentObserver(
                        Settings.Global.getUriFor(Settings.Global.BAIKALOS_EXTREME_IDLE), false, mObserver);
            } else {
                mContext.getContentResolver().unregisterContentObserver(mObserver);
            }
        }
    }
}
