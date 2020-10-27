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

import com.android.server.BaikalStaticService;


import android.os.SystemClock;

import android.util.Slog;
import android.util.ArrayMap;

import android.content.ComponentName;
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

import android.app.ActivityManager;

import java.util.Arrays;
import java.util.ArrayList;

import com.android.internal.baikalos.AppProfile;
import com.android.internal.baikalos.AppProfileSettings;

public class BaikalActivityServiceStatic {

    private static final String TAG = "BaikalActivityServiceStatic";

    private static final boolean DEBUG = false;
    private static final boolean DEBUG_STAMINA = false;

    private static final String [] mGoogleServicesIdleBlackListed = {
        "com.google.android.location.geocode.GeocodeService",
        "com.google.android.location.geofencer.service.GeofenceProviderService",
        //"com.google.android.location.network.NetworkLocationService",
        //"com.google.android.location.internal.GoogleLocationManagerService",
        "com.google.android.location.reporting.service.ReportingAndroidService",
        //"com.google.android.location.internal.server.GoogleLocationService",
        //"com.google.android.location.fused.FusedLocationService",
        //"com.google.android.location.internal.server.HardwareArProviderService",
        "com.google.android.location.places.service.PlaceDetectionAsyncService",
        "com.google.android.gms.tron.CollectionService",
        //"com.google.location.nearby.direct.service.NearbyDirectService",
        ".lockbox.service.LockboxBrokerService",
        ".usagereporting.service.UsageReportingService"
     
    };


    public static int applyOomAdjLocked(ActivityManagerService mAm, ProcessRecord app,ProcessRecord top_app) {

        if( DEBUG ) {
            /*Slog.d(TAG,"applyOomAdjLocked:" + 
		        app.info.packageName + "/" + app.info.uid +
    		    ",ag=" + BaikalSettings.getAggressiveIdleEnabled() +
    		    ",ex=" + BaikalSettings.getExtremeIdleActive() + 
    		    ",id=" + Runtime.isIdleMode()
	            );*/
        }

	    if( app == top_app ) return 1;


        if( !BaikalSettings.getStaminaMode() &&
            !BaikalSettings.getExtremeIdleActive() &&
            !BaikalSettings.getAggressiveIdleEnabled() ) {
            return 0;
        }

        if( BaikalSettings.getAppBlocked(app.info.uid, app.info.packageName) ) {
            Slog.i(TAG,"applyOomAdjLocked: killing blocked app: " + app.info.packageName + "/" + app.info.uid);
            try {
                for (int is = app.mServices.size()-1;is >= 0; is--) {
                    ServiceRecord s = app.mServices.valueAt(is);
                    s.delayed = false;
                    s.stopIfKilled = true;
                } 
            } catch (Exception e) {
            }
            return 2;
        }


        final int appId = UserHandle.getAppId(app.info.uid);

        if( BaikalSettings.getStaminaMode() )  {
            if( app.curAdj <= 50 ) return 0;
        } else if( BaikalSettings.getExtremeIdleActive() ) {
            if( app.curAdj <= 300 ) return 0;
        } else if( BaikalSettings.getAggressiveIdleEnabled() ) {
            if( app.curAdj <= 600 ) return 0;
        } else {
            if( app.curAdj < 900 ) return 0;
        }

        final long now = SystemClock.uptimeMillis();
        final long oldTime = now - 15 * 1000;
        final long oldTimeStamina = now - 5 * 1000;

        AppProfile profile = AppProfileSettings.getProfileStatic(app.info.packageName);

        if( profile != null && profile.mBackground < 0 ) return 0;

        switch (app.getCurProcState()) {
            case ActivityManager.PROCESS_STATE_BOUND_TOP:
            case ActivityManager.PROCESS_STATE_BOUND_FOREGROUND_SERVICE:
            case ActivityManager.PROCESS_STATE_FOREGROUND_SERVICE:
            case ActivityManager.PROCESS_STATE_IMPORTANT_FOREGROUND:
            case ActivityManager.PROCESS_STATE_IMPORTANT_BACKGROUND:
            case ActivityManager.PROCESS_STATE_TRANSIENT_BACKGROUND:

                if( !BaikalSettings.getStaminaMode() ) return 0;
            
            case ActivityManager.PROCESS_STATE_BACKUP:
            case ActivityManager.PROCESS_STATE_SERVICE:
            case ActivityManager.PROCESS_STATE_RECEIVER:
            case ActivityManager.PROCESS_STATE_HEAVY_WEIGHT:
            case ActivityManager.PROCESS_STATE_LAST_ACTIVITY:
            case ActivityManager.PROCESS_STATE_CACHED_ACTIVITY:
            case ActivityManager.PROCESS_STATE_CACHED_ACTIVITY_CLIENT:
            case ActivityManager.PROCESS_STATE_CACHED_RECENT:

                if( BaikalSettings.getAppRestricted(appId,app.info.packageName) ) {
                    Slog.i(TAG,"applyOomAdjLocked: killing restricted app: " + app.info.packageName + "/" + app.info.uid);
                    try {
                        for (int is = app.mServices.size()-1;is >= 0; is--) {
                            ServiceRecord s = app.mServices.valueAt(is);
        	                s.delayed = false;
                            s.stopIfKilled = true;
                        } 
                    } catch (Exception e) {
                    }
                    return 2;
                }

                if( profile != null && profile.mBackground < 1 ) return 0;
        
                if( app.info.uid < 10000 &&  app.curAdj < (ProcessList.CACHED_APP_MIN_ADJ) ) return 0;
 
                if( BaikalSettings.getStaminaMode() || 
                    BaikalSettings.getExtremeIdleActive() ){
                } else {
                    return 0;
                }

                if(  BaikalSettings.getStaminaMode() && profile != null && profile.mStamina ) {
                    return 0;
                }
                    
                if( (!BaikalSettings.getExtremeIdleActive() && !BaikalSettings.getStaminaMode()) || app.curAdj < (ProcessList.CACHED_APP_MIN_ADJ) ) {
                    if( !BaikalSettings.getAppRestricted(appId,app.info.packageName) && Arrays.binarySearch(mAm.mDeviceIdleWhitelist, appId) >= 0 ) {
                        if( DEBUG_STAMINA ) Slog.i(TAG,"applyOomAdjLocked: BFGS stamina: sys wl " + app.info.packageName + "/" + app.info.uid);
                        return 0;
                    }
                    if( (Arrays.binarySearch(mAm.mDeviceIdleTempWhitelist, appId) >= 0)
                        || (mAm.mPendingTempWhitelist.indexOfKey(app.info.uid) >= 0)  )  {
                        if( DEBUG_STAMINA ) Slog.i(TAG,"applyOomAdjLocked: BFGS stamina: temp wl " + app.info.packageName + "/" + app.info.uid);
                        return 1;
                    }
                }

                if ( profile != null && profile.mBackground > 0 && (app.lastActivityTime < oldTimeStamina) ) {
                    Slog.i(TAG,"applyOomAdjLocked: BFGS stamina: restricted " + app.info.packageName + "/" + app.info.uid);
                } else {

                    if ( BaikalSettings.getStaminaMode() && (app.lastActivityTime > oldTimeStamina) ) {
                        if( DEBUG_STAMINA ) Slog.i(TAG,"applyOomAdjLocked: BFGS stamina: active " + app.info.packageName + "/" + app.info.uid);
                        return 0;
                    }
                
                    if ( !BaikalSettings.getStaminaMode() && (app.lastActivityTime > oldTime) ) 
                    { 
                        if( DEBUG_STAMINA ) Slog.i(TAG,"applyOomAdjLocked: BFGS extreme: active " + app.info.packageName + "/" + app.info.uid);
                        return 0;
                    }
                }

                /*if( DEBUG )*/ Slog.i(TAG,"applyOomAdjLocked: killing active app: " + app.info.packageName + "/" + app.info.uid);

                try {
                    for (int is = app.mServices.size()-1;is >= 0; is--) {
                        ServiceRecord s = app.mServices.valueAt(is);
    	                s.delayed = false;
                        s.stopIfKilled = true;
                    } 
                } catch (Exception e) {
                }
                return 2;

            case ActivityManager.PROCESS_STATE_CACHED_EMPTY: {

                if( BaikalSettings.getAppRestricted(appId,app.info.packageName) ) {
                    Slog.i(TAG,"applyOomAdjLocked: killing restricted cached app: " + app.info.packageName + "/" + app.info.uid);
                    try {
                        for (int is = app.mServices.size()-1;is >= 0; is--) {
                            ServiceRecord s = app.mServices.valueAt(is);
        	                s.delayed = false;
                            s.stopIfKilled = true;
                        } 
                    } catch (Exception e) {
                    }
                    return 2;
                }


                if ( BaikalSettings.getStaminaMode() && (app.lastActivityTime > oldTimeStamina) ) {
                    if( DEBUG_STAMINA ) Slog.i(TAG,"applyOomAdjLocked: CEM stamina: active " + app.info.packageName + "/" + app.info.uid);
                    return 0;
                }
                if ( !BaikalSettings.getStaminaMode() && (app.lastActivityTime > oldTime) ) {
                    if( DEBUG_STAMINA ) Slog.i(TAG,"applyOomAdjLocked: CEM extreme: active " + app.info.packageName + "/" + app.info.uid);
                    return 0;
                }

                if( BaikalSettings.getStaminaMode() ) {
                    if( DEBUG_STAMINA ) Slog.i(TAG,"applyOomAdjLocked: CEM stamina: " + app.info.packageName + "/" + app.info.uid);
                } else if( !Runtime.isIdleMode() ) {
                    if( !(BaikalSettings.getExtremeIdleActive() ) ) {
                        return 0;
                    }
                    if( app.curAdj < (ProcessList.CACHED_APP_MIN_ADJ + 20) ) {
                        if( DEBUG_STAMINA ) Slog.i(TAG,"applyOomAdjLocked: CEM extreme: low adjustment" + app.info.packageName + "/" + app.info.uid);
                        return 0;
                    }
                } else {
                    if( BaikalSettings.getExtremeIdleActive() ) {
                        if( app.curAdj < (ProcessList.CACHED_APP_MIN_ADJ) ) {
                            if( DEBUG_STAMINA ) Slog.i(TAG,"applyOomAdjLocked: CEM idle extreme: low adjustment " + app.info.packageName + "/" + app.info.uid);
                            return 0;
                        }
                    } else if( BaikalSettings.getAggressiveIdleEnabled() ) {
                        if( app.curAdj < (ProcessList.CACHED_APP_MIN_ADJ + 15) ) {
                            if( DEBUG_STAMINA ) Slog.i(TAG,"applyOomAdjLocked: CEM idle aggressive: low adjustment " + app.info.packageName + "/" + app.info.uid);
                            return 0;
                        }
                    } else {
                        if( app.curAdj < (ProcessList.CACHED_APP_MIN_ADJ + 30) ) {
                            if( DEBUG_STAMINA ) Slog.i(TAG,"applyOomAdjLocked: CEM idle normal: low adjustment " + app.info.packageName + "/" + app.info.uid);
                            return 0;
                        }
                    }
                } 

                /*if( DEBUG ) */ Slog.i(TAG,"applyOomAdjLocked: killing cached app: " + app.info.packageName + "/" + app.info.uid);
                try {
                    for (int is = app.mServices.size()-1;is >= 0; is--) {
                        ServiceRecord s = app.mServices.valueAt(is);
    	                s.delayed = false;
                        s.stopIfKilled = true;
                    } 
                } catch (Exception e) {
                }
                return 2;
             }
        }
	    return 0;
    }

    public static boolean isServiceWhitelisted(ActivityManagerService mAm, ServiceRecord service, int callingUid, int callingPid, String callingPackageName, boolean isStarting) {
        if( DEBUG ) Slog.i(TAG,"isServiceWhitelisted: from " + callingPackageName + "/" + callingUid + "/" + callingPid + " to " + service);

        /*

        if( service.name.getClassName().startsWith("android.telecom") ) return true;
        if( service.name.getClassName().startsWith("android.bluetooth") ) return true;
        if( service.name.getClassName().endsWith(".chimera.GmsBoundBrokerService") ) return true;
        if( service.name.getClassName().endsWith(".chimera.GmsIntentOperationService") ) return true;
        if( service.name.getClassName().endsWith(".chimera.PersistentIntentOperationService") ) return true;
        if( service.name.getClassName().endsWith(".chimera.GmsApiService") ) return true;
        if( service.name.getClassName().endsWith(".gcm.GcmService") ) return true;

        //if( service.name.getClassName().endsWith("com.google.android.location.internal.GoogleLocationManagerService") ) return true;
        if( service.name.getClassName().endsWith("com.google.android.location.internal.server.GoogleLocationService") ) return true;
        if( service.name.getClassName().endsWith("com.google.android.location.network.NetworkLocationService") ) return true;
        if( service.name.getClassName().endsWith("com.google.android.location.reporting.service.DispatchingService") ) return true;
        if( service.name.getClassName().endsWith("com.google.android.location.internal.PendingIntentCallbackService") ) return true;
        if( service.name.getClassName().endsWith("com.google.android.contextmanager.service.ContextManagerService") ) return true;
        if( service.name.getClassName().endsWith(".tapandpay.security.StorageKeyCacheService") ) return true;
        if( service.name.getClassName().endsWith("com.google.android.gms.wallet.service.PaymentService") ) return true;


        if( !Runtime.isIdleMode() ) {
            if( service.name.getClassName().endsWith("com.google.android.location.internal.GoogleLocationManagerService") ) return true;
            //if( service.name.getClassName().endsWith("com.google.android.location.internal.server.GoogleLocationService") ) return true;
            //if( service.name.getClassName().endsWith("com.google.android.location.network.NetworkLocationService") ) return true;
            //if( service.name.getClassName().endsWith("com.google.android.location.reporting.service.DispatchingService") ) return true;
            //if( service.name.getClassName().endsWith("com.google.android.location.internal.PendingIntentCallbackService") ) return true;
        }
        */
        return false;
    }

    public static boolean isServiceBlacklisted(ActivityManagerService mAm, ServiceRecord service, int callingUid, int callingPid, String callingPackageName, boolean isStarting) {
        return false;
    }

    public static boolean isBroadcastBlacklisted(ActivityManagerService mAm,BroadcastRecord r, ResolveInfo info, boolean background) {

        if( !background ) return false;

        String act = r.intent.getAction();
        //if( !act.startsWith("android.intent.action.BOOT_COMPLETED") ) return false;

        AppProfile profile = AppProfileSettings.getProfileStatic(info.activityInfo.packageName);
        if( profile == null ) return false;

        if( !getBackgroundMode(profile) ) return true;

        return false;
    }

    public static boolean isBroadcastWhitelisted(ActivityManagerService mAm,BroadcastRecord r, ResolveInfo info, boolean background) {

        if( DEBUG )  Slog.i(TAG,"isBroadcastWhitelisted: from " + r.callerPackage + "/" + r.callingUid + "/" + r.callingPid + " to " + r.intent + " on [" + background + "]");

        if( !background ) return false;
        
	    ComponentName cmp = r.intent.getComponent();
	    String act = r.intent.getAction();
        if( act == null && cmp == null ) return false; // Invalid Intent received;
        if( !BaikalUtils.isGmsUid(info.activityInfo.applicationInfo.uid) ) return false;

        if( act != null ) {
        	//if( act.startsWith("android.") ) return true;
        	if( act.startsWith("com.google.android.gms.auth") ) return true;
        	if( act.startsWith("com.google.android.gms.gcm") ) return true;
    	    if( act.startsWith("com.google.android.c2dm") ) return true;
    	    if( act.startsWith("com.android.vending.INTENT_PACKAGE") ) return true;
        }
        if( cmp != null ) {
            String cmps = cmp.toString();
        	if( cmps.endsWith(".chimera.GmsBoundBrokerService") ) return true;
           	if( cmps.endsWith(".chimera.GmsIntentOperationService") ) return true;
           	if( cmps.endsWith(".gcm.GcmService") ) return true;
        }

        if( info.activityInfo.name.endsWith(".chimera.GmsIntentOperationService$PersistentInternalReceiver") ) return true;
	    return false;
    }

    public static int getAppStartModeLocked(int uid, String packageName, int packageTargetSdk,
            int callingPid, boolean alwaysRestrict, boolean disabledOnly, boolean forcedStandby) {

        if( BaikalSettings.getAppBlocked(uid, packageName) ) {
            return ActivityManager.APP_START_MODE_DISABLED;
        }


        if( BaikalUtils.isDolbyUid(uid) ) {
            if( !SystemProperties.getBoolean("persist.baikal.dolby.enable",false) ) {
                return ActivityManager.APP_START_MODE_DISABLED;
            }
        }
        return -1;
    }

    public static boolean allowBackgroundStart(int uid, String packageName) {
        AppProfile profile = AppProfileSettings.getProfileStatic(packageName);
        if( profile == null ) return true;

        if( !getBackgroundMode(profile) ) return false;
        return true;
    }

    private static boolean getBackgroundMode(AppProfile profile) {
        if( Runtime.isIdleMode()  ) {
            if( profile.mBackground > 1 && BaikalSettings.getExtremeIdleEnabled() ) return false;
            if( profile.mBackground > 0 && BaikalSettings.getAggressiveIdleEnabled() ) return false;
        } else {
            if( profile.mBackground > 2 && BaikalSettings.getAggressiveIdleEnabled() ) return false;
            if( profile.mBackground > 1 && BaikalSettings.getExtremeIdleEnabled() ) return false;
        }
        return true;
    }
}
