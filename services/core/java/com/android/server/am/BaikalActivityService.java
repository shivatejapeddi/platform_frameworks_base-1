/*
 * Copyright (C) 2019 BaikalOS
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


package com.android.server.am;

import android.provider.Settings;

import com.android.internal.baikalos.Actions;
import com.android.internal.baikalos.Bluetooth;
import com.android.internal.baikalos.Telephony;
import com.android.internal.baikalos.Torch;
import com.android.internal.baikalos.Sensors;
import com.android.internal.baikalos.Runtime;
import com.android.internal.baikalos.AppProfileManager;
import com.android.internal.baikalos.DevProfileManager;


import com.android.internal.baikalos.BaikalSettings;

import android.util.Slog;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;


import android.os.Binder;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.UserHandle;
import android.os.Process;
import android.os.SystemClock;

import com.android.internal.baikalos.AppProfileSettings;
import com.android.internal.baikalos.AppProfileSettings.IAppProfileSettingsNotifier;
import com.android.internal.baikalos.AppProfile;

public class BaikalActivityService implements com.android.internal.baikalos.AppProfileSettings.IAppProfileSettingsNotifier {

    private static final String TAG = "BaikalActivityService";

    private static final boolean DEBUG = false;

    private boolean mSystemReady = false;

    private Context mContext;
    private Handler mHandler;

    public AppProfileSettings mAppSettings;

    BaikalActivityService(Handler handler, Context context) {
        mContext = context;
        mHandler = handler;
        if( DEBUG ) {
            Slog.i(TAG,"BaikalActivityService()");
        }


    }

    public void setSystemReady(boolean ready) {
        if( DEBUG ) {
            Slog.i(TAG,"setSystemReady(" + ready + ")");
        }

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BAIKALOS_STAMINA_ENABLED, 0);

	    mSystemReady = ready;
        mAppSettings = AppProfileSettings.getInstance(mHandler, mContext, mContext.getContentResolver(), this);
    }

    @Override
    public void onAppProfileSettingsChanged() {
    }

}
