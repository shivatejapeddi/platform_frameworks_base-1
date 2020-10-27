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

package com.android.internal.baikalos;

import android.util.Slog;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;

public class Actions extends MessageHandler { 

    private static final String TAG = "Baikal.Actions";

    public static final String ACTION_IDLE_MODE_CHANGED = "com.android.internal.baikalos.Actions.ACTION_IDLE_MODE_CHANGED";
    public static final String ACTION_POWER_MODE_CHANGED = "com.android.internal.baikalos.Actions.ACTION_POWER_MODE_CHANGED";
    public static final String ACTION_CHARGER_MODE_CHANGED = "com.android.internal.baikalos.Actions.ACTION_CHARGER_MODE_CHANGED";
    public static final String ACTION_SCREEN_MODE_CHANGED = "com.android.internal.baikalos.Actions.ACTION_SCREEN_MODE_CHANGED";
    public static final String ACTION_STAMINA_CHANGED = "com.android.internal.baikalos.Actions.ACTION_STAMINA_CHANGED";

    public static final String ACTION_READER_MODE_CHANGED = "com.android.internal.baikalos.Actions.ACTION_READER_MODE_CHANGED";

    public static final String ACTION_TOP_APP_CHANGED = "com.android.internal.baikalos.Actions.ACTION_TOP_APP_CHANGED";
    public static final String ACTION_BRIGHTNESS_OVERRIDE = "com.android.internal.baikalos.Actions.ACTION_BRIGHTNESS_OVERRIDE";
    public static final String ACTION_BLUETOOTH_DEVICE_CHANGED = "com.android.internal.baikalos.Actions.ACTION_BLUETOOTH_DEVICE_CHANGED";
    public static final String ACTION_NET_WIFI_CHANGED = "com.android.internal.baikalos.Actions.ACTION_NET_WIFI_CHANGED";
    public static final String ACTION_NET_MOBILE_DATA_CHANGED = "com.android.internal.baikalos.Actions.ACTION_NET_MOBILE_DATA_CHANGED";

    public static final String ACTION_PROFILE_CHANGED = "com.android.internal.baikalos.Actions.ACTION_PROFILE_CHANGED";

    public static final String ACTION_SET_PROFILE = "com.android.internal.baikalos.Actions.ACTION_SET_PROFILE";

    public static final String EXTRA_BOOL_MODE = "com.android.internal.baikalos.Actions.EXTRA_BOOL_MODE";
    public static final String EXTRA_INT_MODE = "com.android.internal.baikalos.Actions.EXTRA_INT_MODE";
    public static final String EXTRA_INT_BRIGHTNESS = "com.android.internal.baikalos.Actions.EXTRA_INT_BRIGHTNESS";
    public static final String EXTRA_UID = "com.android.internal.baikalos.Actions.EXTRA_UID";
    public static final String EXTRA_PACKAGENAME = "com.android.internal.baikalos.Actions.EXTRA_PACKAGENAME";

    public static final String EXTRA_BT_DEVICE = "com.android.internal.baikalos.Actions.EXTRA_BT_DEVICE";
    public static final String EXTRA_WIFI_DEVICE = "com.android.internal.baikalos.Actions.EXTRA_WIFI_DEVICE";

    public static final String EXTRA_PROFILE_THERMAL = "com.android.internal.baikalos.Actions.EXTRA_PROFILE_THERMAL";
    public static final String EXTRA_PROFILE_POWER = "com.android.internal.baikalos.Actions.EXTRA_PROFILE_POWER";
    public static final String EXTRA_PROFILE_READER = "com.android.internal.baikalos.Actions.EXTRA_PROFILE_READER";
    public static final String EXTRA_PROFILE_BRIGHTNESS = "com.android.internal.baikalos.Actions.EXTRA_PROFILE_READER";

    private static Context mStaticContext;
    private static Handler mStaticHandler;

    public Actions(Context context, Handler handler) {
    	mStaticContext = context;
	    mStaticHandler = handler;
    }

    @Override
    protected void initialize() {
    	if( Constants.DEBUG_ACTIONS ) Slog.i(TAG,"initialize()");                
    }

    @Override
    public boolean onMessage(Message msg) {
    	switch(msg.what) {
    	    case Messages.MESSAGE_SEND_INTENT:
    		sendIntent((Intent)msg.obj);
    		return true;
    	}
    	return false;
    }

    public static void sendIdleModeChanged(boolean on) {
        Intent intent = new Intent(ACTION_IDLE_MODE_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
    	intent.putExtra(EXTRA_BOOL_MODE,on);
    	enqueueIntent(intent);
    }

    public static void sendPowerModeChanged(boolean on) {
        Intent intent = new Intent(ACTION_POWER_MODE_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
    	intent.putExtra(EXTRA_BOOL_MODE,on);
    	enqueueIntent(intent);
    }

    public static void sendStaminaChanged(boolean on) {
        Intent intent = new Intent(ACTION_STAMINA_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
    	intent.putExtra(EXTRA_BOOL_MODE,on);
    	enqueueIntent(intent);
    }

    public static void sendChargerModeChanged(boolean on) {
        Intent intent = new Intent(ACTION_CHARGER_MODE_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
    	intent.putExtra(EXTRA_BOOL_MODE,on);
    	enqueueIntent(intent);
    }

    public static void sendScreenModeChanged(boolean on) {
        Intent intent = new Intent(ACTION_SCREEN_MODE_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
    	intent.putExtra(EXTRA_BOOL_MODE,on);
    	enqueueIntent(intent);
    }

    public static void sendReaderModeChanged(boolean on) {
        Intent intent = new Intent(ACTION_READER_MODE_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
    	intent.putExtra(EXTRA_BOOL_MODE,on);
    	enqueueIntent(intent);
    }

    public static void sendBrightnessOverrideChanged(int brightness) {
        Intent intent = new Intent(ACTION_BRIGHTNESS_OVERRIDE);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
    	intent.putExtra(EXTRA_INT_BRIGHTNESS,brightness);
    	enqueueIntent(intent);
    }

    public static void sendTopAppChanged(int uid,String packageName) {
        Intent intent = new Intent(ACTION_TOP_APP_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
    	intent.putExtra(EXTRA_UID,uid);
    	intent.putExtra(EXTRA_PACKAGENAME,packageName);
    	enqueueIntent(intent);
    }

    public static void sendSetProfile(String profileName) {
        Intent intent = new Intent(ACTION_SET_PROFILE);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
    	intent.putExtra("profile",profileName);
    	enqueueIntent(intent);
    }

    public static void enqueueIntent(Intent intent) {
    	Message msg = mStaticHandler.obtainMessage(Messages.MESSAGE_SEND_INTENT);
    	msg.obj = intent;
    	mStaticHandler.sendMessage(msg);
    }

    private static void sendIntent(Intent intent) {
    	mStaticContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }
}
