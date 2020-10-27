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

import android.app.AlarmManager;


public class BaikalStaticService {

    private static final String TAG = "BaikalService";

    private static final boolean DEBUG = true;

    private static boolean mSystemReady = false;

    private static Context mContext;

    static com.android.internal.baikalos.Actions mBaikalActions;
    static com.android.internal.baikalos.Bluetooth mBaikalBluetooth;
    static com.android.internal.baikalos.Telephony mBaikalTelephony;
    static com.android.internal.baikalos.Torch mBaikalTorch;
    static com.android.internal.baikalos.Sensors mBaikalSensors;
    static com.android.internal.baikalos.AppProfileManager mBaikalAppProfileManager;
    static com.android.internal.baikalos.DevProfileManager mBaikalDevProfileManager;

    static com.android.internal.baikalos.BaikalSettings mBaikalSettings;


    BaikalStaticService() {
    }

    static void Initialize(Context context,
	    com.android.internal.baikalos.Actions baikalActions,
	    com.android.internal.baikalos.Bluetooth baikalBluetooth,
	    com.android.internal.baikalos.Telephony baikalTelephony,
	    com.android.internal.baikalos.Torch baikalTorch,
	    com.android.internal.baikalos.Sensors baikalSensors,
	    com.android.internal.baikalos.AppProfileManager baikalAppProfileManager,
	    com.android.internal.baikalos.DevProfileManager baikalDevProfileManager,
	    com.android.internal.baikalos.BaikalSettings baikalSettings) {

        mContext = context;
        if( DEBUG ) {
            Slog.i(TAG,"BaikalStatic()");
        }

	    mBaikalActions = baikalActions;
	    mBaikalBluetooth = baikalBluetooth;
	    mBaikalTelephony = baikalTelephony;
	    mBaikalTorch = baikalTorch;
	    mBaikalSensors = baikalSensors;
        mBaikalAppProfileManager = baikalAppProfileManager;
        mBaikalDevProfileManager = baikalDevProfileManager;
	    mBaikalSettings = baikalSettings;
    }

    static void setSystemReady(boolean ready) {
        if( DEBUG ) {
            Slog.i(TAG,"setSystemReady(" + ready + ")");
        }
	    mSystemReady = ready;
    }

    private static Object mStaticMembersLock = new Object();

    private static boolean mHideIdleFromGms;
    private static boolean mUnrestrictedNetwork;
	

    public static boolean isEnergySaveMode() {
	    return BaikalSettings.getAggressiveIdleEnabled() ||
		   BaikalSettings.getExtremeIdleEnabled();
    }

    public static boolean processAlarmLocked(AlarmManagerService.Alarm a, AlarmManagerService.Alarm pendingUntil) {

        if ( a == pendingUntil ) {
            if( DEBUG ) {
                Slog.i(TAG,"DeviceIdleAlarm: unrestricted:" + a.statsTag + ":" + a.toString() + ", ws=" + a.workSource );
            }
            return false;
        }

        if( a.alarmClock != null ) return false;

        if( !isEnergySaveMode() ) return false;

        /*
        if( a.statsTag.contains("WifiConnectivityManager Schedule Periodic Scan Timer") ) {  
            final long now = SystemClock.elapsedRealtime();
            if( (a.when - now)  < 60*60*1000 ) {
                a.when = a.whenElapsed = a.maxWhenElapsed = a.origWhen = now + 60*60*1000;
            } 
            if( DEBUG ) {
                Slog.i(TAG,"DeviceIdleAlarm: AdjustAlarm (unrestricted):" + a.statsTag + ":" + a.toString() + ", ws=" + a.workSource );
            }
            return true;
        }*/

        // a.statsTag.contains("WifiConnectivityManager Schedule Watchdog Timer") ||
        // a.statsTag.contains("WifiConnectivityManager Schedule Periodic Scan Timer") ||
        // a.statsTag.contains("WifiConnectivityManager Restart") ) {


	    boolean block = false;

        a.wakeup = a.type == AlarmManager.ELAPSED_REALTIME_WAKEUP
                || a.type == AlarmManager.RTC_WAKEUP;

        if ( ((a.flags&(AlarmManager.FLAG_ALLOW_WHILE_IDLE_UNRESTRICTED | 
		       AlarmManager.FLAG_ALLOW_WHILE_IDLE | 
		       AlarmManager.FLAG_WAKE_FROM_IDLE)) != 0 
            || a.wakeup)) {

	        if( a.uid < Process.FIRST_APPLICATION_UID  ) {
                if( a.statsTag.equals("doze_time_tick") ) {
                    a.flags |= AlarmManager.FLAG_ALLOW_WHILE_IDLE_UNRESTRICTED;        
                    a.wakeup = true;
                    a.type |= AlarmManager.ELAPSED_REALTIME_WAKEUP;
                } else if( a.statsTag.contains("NETWORK_LINGER_COMPLETE") ||
            	    a.statsTag.contains("WriteBufferAlarm") ||
            	    a.statsTag.contains("WificondScannerImpl") ||
            	    a.statsTag.contains("WifiConnectivityManager") ) {
            	    a.flags &= ~(AlarmManager.FLAG_WAKE_FROM_IDLE);
            	    a.wakeup = false;
                }  else if( a.statsTag.contains("*sync") ||
            	    a.statsTag.contains("*job") || 
                    a.statsTag.contains("com.android.server.NetworkTimeUpdateService.action.POLL") ||
                    a.statsTag.contains("APPWIDGET_UPDATE") ) {
                    block = true;
                } 
	        } else {
        	    if( a.packageName.startsWith("com.google.android.gms") ) {
            	    block = true;
        	    } else if( a.statsTag.contains("StkMenuActivity") ) {
            	    block = true;
        	    } else if( a.statsTag.contains("com.google.android.clockwork.TIME_ZONE_SYNC") ) {
            	    block = true;
        	    } else if( a.statsTag.contains("org.altbeacon.beacon.startup.StartupBroadcastReceiver") ) {
            	    block = true;
                }
		        if( (a.flags & AlarmManager.FLAG_ALLOW_WHILE_IDLE_UNRESTRICTED) == 0 ) {
		            block = true;
		        }
            }
        } 

	    if( block ) {
	        a.wakeup = false;
            a.flags &= ~(AlarmManager.FLAG_WAKE_FROM_IDLE 
                    | AlarmManager.FLAG_ALLOW_WHILE_IDLE
                    | AlarmManager.FLAG_ALLOW_WHILE_IDLE_UNRESTRICTED);

            if( DEBUG ) {
                Slog.i(TAG,"DeviceIdleAlarm: restricted:" + a.statsTag + ":" + a.toString() + ", ws=" + a.workSource ); 
            }
	    }

	    if( !a.wakeup && (a.type == AlarmManager.ELAPSED_REALTIME_WAKEUP
          || a.type == AlarmManager.RTC_WAKEUP ) ) {
	        if( a.type == AlarmManager.ELAPSED_REALTIME_WAKEUP ) a.type = AlarmManager.ELAPSED_REALTIME;
	        else a.type = AlarmManager.RTC;
            a.wakeup = false;
            if( DEBUG ) {
                Slog.i(TAG,"DeviceIdleAlarm: blocked:" + a.statsTag + ":" + a.toString() + ", ws=" + a.workSource );
            }

	    }

        if( a.wakeup ) {
            if( DEBUG ) {
                Slog.i(TAG,"DeviceIdleAlarm: unrestricted:" + a.statsTag + ":" + a.toString() + ", ws=" + a.workSource );
            }
        }
	    return block;
   }
}
