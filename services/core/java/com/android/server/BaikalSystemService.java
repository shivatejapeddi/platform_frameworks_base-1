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


package com.android.server;

//import com.android.internal.baikalos;


import com.android.internal.baikalos.Actions;
import com.android.internal.baikalos.Bluetooth;
import com.android.internal.baikalos.Telephony;
import com.android.internal.baikalos.Torch;
import com.android.internal.baikalos.Sensors;
import com.android.internal.baikalos.Runtime;
import com.android.internal.baikalos.AppProfileManager;
import com.android.internal.baikalos.DevProfileManager;


import com.android.internal.baikalos.BaikalSettings;
import com.android.internal.baikalos.BaikalUtils;


import android.util.Slog;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;


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

import com.android.server.SystemService;

//import com.android.internal.custom.hardware.LineageHardwareManager;
//import com.android.internal.custom.hardware.DisplayMode;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

public class BaikalSystemService extends SystemService {

    private static final String TAG = "BaikalService";

    private static final boolean DEBUG = false;

    private boolean mSystemReady = false;

    private final Context mContext;

    //private LineageHardwareManager mHardware;


    final MyHandler mHandler;
    final MyHandlerThread mHandlerThread;

    final com.android.internal.baikalos.Actions mBaikalActions;

    final com.android.internal.baikalos.Bluetooth mBaikalBluetooth;

    final com.android.internal.baikalos.Telephony mBaikalTelephony;
    final com.android.internal.baikalos.Torch mBaikalTorch;
    final com.android.internal.baikalos.Sensors mBaikalSensors;
    final com.android.internal.baikalos.AppProfileManager mBaikalAppProfileManager;
    final com.android.internal.baikalos.DevProfileManager mBaikalDevProfileManager;

    com.android.internal.baikalos.BaikalSettings mBaikalSettings;

    public BaikalSystemService(Context context) {
        super(context);
        mContext = context;
        if( DEBUG ) {
            Slog.i(TAG,"BaikalSystemService()");
        }

        mHandlerThread = new MyHandlerThread();
        mHandlerThread.start();
        mHandler = new MyHandler(mHandlerThread.getLooper());


	    mBaikalActions = new Actions(mContext,mHandler);
	    mBaikalBluetooth = new Bluetooth();
	    mBaikalTelephony = new Telephony();
	    mBaikalTorch = new Torch();
	    mBaikalSensors = new Sensors();
        mBaikalAppProfileManager = new AppProfileManager();
        mBaikalDevProfileManager = new DevProfileManager();
   }





    @Override
    public void onStart() {
        if( DEBUG ) {
            Slog.i(TAG,"onStart()");
        }
/*
        mBinderService = new BinderService();
        publishBinderService(Context.BAIKAL_SERVICE_CONTROLLER, mBinderService);
        publishLocalService(BaikalService.class, this);

        readConfigFileLocked();*/
    }

    @Override
    public void onBootPhase(int phase) {
        if( DEBUG ) {
            Slog.i(TAG,"onBootPhase(" + phase + ")");
        }

        if (phase == PHASE_SYSTEM_SERVICES_READY) {
            synchronized(this) {
                Slog.i(TAG,"onBootPhase(" + phase + "): Core BaikalOS componenets init");
                mBaikalSettings.loadStaticConstants(mContext);
                updateDolbyService();
	        }
    	} else if( phase == PHASE_BOOT_COMPLETED) {

            if( DEBUG ) {
                Slog.i(TAG,"onBootPhase(PHASE_BOOT_COMPLETED)");
            }

            synchronized(this) {

    		    mBaikalActions.initialize(mContext,mHandler);	
    		    mBaikalBluetooth.initialize(mContext,mHandler);	
    		    mBaikalTelephony.initialize(mContext,mHandler);	
    		    mBaikalTorch.initialize(mContext,mHandler);	
    		    mBaikalSensors.initialize(mContext,mHandler);
                mBaikalAppProfileManager.initialize(mContext,mHandler);
                mBaikalDevProfileManager.initialize(mContext,mHandler);

                mBaikalSettings = new BaikalSettings(mHandler,mContext);

                //mHardware = LineageHardwareManager.getInstance(mContext);

                IntentFilter topAppFilter = new IntentFilter();
                topAppFilter.addAction(Actions.ACTION_TOP_APP_CHANGED);
                getContext().registerReceiver(mTopAppReceiver, topAppFilter);


	        }

	        BaikalStaticService.Initialize(mContext,
    			mBaikalActions,
    			mBaikalBluetooth,
                mBaikalTelephony,
    			mBaikalTorch,
    			mBaikalSensors,
                mBaikalAppProfileManager,
                mBaikalDevProfileManager,
    			mBaikalSettings);


            synchronized(this) {

                int uid = getPackageUidLocked("com.google.android.gms");

                Runtime.setGmsUid(uid);
		        BaikalUtils.setGmsUid(uid);

                uid = getPackageUidLocked("com.android.vending");
                Runtime.setGpsUid(uid);

                uid = getPackageUidLocked("com.dolby.daxservice");
                BaikalUtils.setDolbyUid(uid);

                //mConstants.updateConstantsLocked();

            }
        }
    }

    private int getPackageUidLocked(String packageName) {
        final PackageManager pm = getContext().getPackageManager();

        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName,PackageManager.MATCH_ALL);
            if( ai != null ) {
                Slog.i(TAG,"getPackageUidLocked package=" + packageName + ", uid=" + ai.uid);
                return ai.uid;
            }
        } catch(Exception e) {
            Slog.i(TAG,"getPackageUidLocked package=" + packageName + " not found on this device.");
        }
        return -1;
    }



    final class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
            }

	    if( mBaikalActions.onMessage(msg) ) return; 
	    if( mBaikalSensors.onMessage(msg) ) return; 
	    if( mBaikalTelephony.onMessage(msg) ) return; 
	    if( mBaikalBluetooth.onMessage(msg) ) return; 
	    if( mBaikalTorch.onMessage(msg) ) return; 
        }
    }

    private class MyHandlerThread extends HandlerThread {

        Handler handler;

        public MyHandlerThread() {
            super("baikal.handler", android.os.Process.THREAD_PRIORITY_FOREGROUND);
        }
    }

    private void updateDolbyService() {
        Boolean isDolbyAvail = SystemProperties.getBoolean("persist.baikal.dolby.enable",false);
        if( DEBUG ) Slog.i(TAG,"updateDolbyService isDolbyAvail=" + isDolbyAvail);
        updateDolbyConfiguration(isDolbyAvail);
    }

    private void updateDolbyConfiguration(boolean enabled) {
        if( !enabled ) {
            setPackageEnabled("com.motorola.dolby.dolbyui",false);
            setPackageEnabled("com.dolby.daxservice",false);
        } else {
            setPackageEnabled("com.motorola.dolby.dolbyui",true);
            setPackageEnabled("com.dolby.daxservice",true);
        }
    }

    private void setPackageEnabled(String packageName, boolean enabled) {
        int state = enabled ? COMPONENT_ENABLED_STATE_ENABLED : COMPONENT_ENABLED_STATE_DISABLED;
        try {
            mContext.getPackageManager().setApplicationEnabledSetting(packageName,state,0);
        } catch(Exception e1) {
            Slog.e(TAG, "setPackageEnabled: exception=", e1);
        }
    }

    


    private final BroadcastReceiver mTopAppReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (BaikalSystemService.this) {
                /*  Require Lineage Hardware Manager. Postpone.
                String action = intent.getAction();
                String packageName = (String)intent.getExtra(Actions.EXTRA_PACKAGENAME);
                int uid = (int)intent.getExtra(Actions.EXTRA_UID);
                if( mHardware != null ) {
                    DisplayMode current = mHardware.getCurrentDisplayMode();
                    if( current != null && current.name.contains("HDR") ) {
                        if( DEBUG ) Slog.i(TAG,"setDisplayMode current=(" + current.id + "," + current.name + ")");
                        DisplayMode mode = mHardware.getDefaultDisplayMode();
                        if( mode != null ) {
                            if( DEBUG ) Slog.i(TAG,"setDisplayMode default=(" + mode.id + "," + mode.name + ")");
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mHardware.setDisplayMode(mode,false);
                                }
                            }, 1000);
                        }
                    }
                }*/
            }
        }
    };


}
