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

import android.os.UserHandle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.Uri;

import android.database.ContentObserver;

import android.provider.Settings;

public class BaikalSettings extends ContentObserver {

    private static final String TAG = "Baikal.Settings";

    private static ContentResolver mResolver;
    private static Handler mHandler;
    private static Context mContext;
   	private static Object _staticLock = new Object();

   	private static boolean mHallSensorEnabled;
   	private static boolean mProximityWakeEnabled;
   	private static boolean mProximitySleepEnabled;
   	private static boolean mTorchInCallEnabled;
   	private static boolean mTorchNotificationEnabled;
   	private static boolean mAggressiveIdleEnabled;
   	private static boolean mExtremeIdleEnabled;
   	private static boolean mHideGmsEnabled;
   	private static boolean mUnrestrictedNetEnabled;
    private static boolean mStaminaMode;
    private static boolean mStaminaOiMode;
    private static boolean mDisableHeadphonesDetect;

    private static boolean mIsGmsBlocked;
    private static boolean mIsGmsRestricted;
    private static boolean mIsGmsRestrictedIdle;
    private static boolean mIsGmsRestrictedStamina;
    private static boolean mIsGpsRestricted;

    private static int mTopAppUid;
    private static String mTopAppPackageName;

    private static boolean mIdleMode;

   	public static boolean getHallSensorEnabled() {
   	    return mHallSensorEnabled;
   	}

   	public static boolean getProximityWakeEnabled() {
   	    return mProximityWakeEnabled;
   	}

   	public static boolean getProximitySleepEnabled() {
   	    return mProximitySleepEnabled;
   	} 

   	public static boolean getTorchInCallEnabled() {
   	    return mTorchInCallEnabled;
   	}

	public static boolean getTorchNotificationEnabled() {
	    return mTorchNotificationEnabled;
	}

	public static boolean getAggressiveIdleEnabled() {
	    return mAggressiveIdleEnabled || mExtremeIdleEnabled;
	}

	public static boolean getExtremeIdleEnabled() {
	    return mExtremeIdleEnabled;
	}

	public static boolean getExtremeIdleActive() {
	    return mExtremeIdleEnabled && Runtime.isIdleMode();
	}
	
	public static boolean getHideGmsEnabled() {
	    return mHideGmsEnabled;
	}

	public static boolean getUnrestrictedNetEnabled() {
	    return mUnrestrictedNetEnabled;
	}

	public static boolean getStaminaMode() {

        if( mStaminaMode || (mStaminaOiMode && Runtime.isIdleMode()) ) {
            Slog.e(TAG, "getStaminaMode: ret=true");
            return true;
        }
        return false;
        //return ( mStaminaMode || (mStaminaOiMode && Runtime.isIdleMode()) );
	    //return mStaminaMode;
	}

	public static boolean getDisableHeadphonesDetect() {
	    return mDisableHeadphonesDetect;
	}

    public static boolean getAppRestricted(int uid) {
        return getAppRestricted(uid,null);
    }

    public static boolean getAppRestricted(int uid, String packageName) {
        boolean ret = getAppRestrictedInternal(uid,packageName);

        if( ret /*Constants.DEBUG_RAW*/ ) {
            Slog.e(TAG, "getAppRestricted: ret=" + ret + ", uid=" + uid + ", pkg=" + packageName + ", top=" + mTopAppUid );
        }
        return ret;
    }

    public static boolean getAppBlocked(int uid, String packageName) {
        boolean ret = getAppBlockedInternal(uid,packageName);

        if( ret /*Constants.DEBUG_RAW*/ ) {
            Slog.e(TAG, "getAppBlocked: ret=" + ret + ", uid=" + uid + ", pkg=" + packageName + ", top=" + mTopAppUid );
        }
        return ret;
    }
                          
    public static boolean getAppBlockedInternal(int uid, String packageName) {

        if( isGmsBlocked() && /*packageName.startsWith("com.google.android.gms") */  Runtime.isGmsUid(uid) ) {
            return true;
        }

        if( uid == mTopAppUid ) return false;

        if( uid < Process.FIRST_APPLICATION_UID ) return false;

        AppProfile profile = null; 
        if( packageName == null ) profile = AppProfileSettings.getProfileStatic(uid);
        else profile = AppProfileSettings.getProfileStatic(packageName);
        if( profile == null ) {
            if( getStaminaMode() ) return true;
            return false;
        }

        if( getStaminaMode() )  {
            if(profile.mStamina) return false;
            return true;
        }

        if( profile.mBackground > 1 && Runtime.isIdleMode() ) return true;
        if( profile.mBackground == 3 ) return true;

        return false;
    }


    public static boolean getAppRestrictedInternal(int uid, String packageName) {
        if( isGmsRestricted() && Runtime.isGmsUid(uid) ) {
            return true;
        }

        if( uid == mTopAppUid ) return false;

        AppProfile profile = null; 
        if( packageName == null ) profile = AppProfileSettings.getProfileStatic(uid);
        else profile = AppProfileSettings.getProfileStatic(packageName);
        if( profile == null ) return false;

        if( profile.mBackground > 1 ) return true;
        if( profile.mBackground > 0 && Runtime.isIdleMode() ) return true;

        return false;
    }

    public static boolean isGmsRestricted() {
        AppProfile profile = AppProfileManager.getCurrentProfile();
        if( profile != null && profile.mRequireGms ) return false;

        if( mIsGmsRestricted ) return true;
        if( mIsGmsRestrictedIdle && Runtime.isIdleMode() ) return true;
        if( mIsGmsRestrictedStamina && getStaminaMode() ) return true;
        return false;
    }

    public static boolean isGmsBlocked() {

        AppProfile profile = AppProfileManager.getCurrentProfile();
        if( profile != null && profile.mRequireGms ) return false;

        if( mIsGmsBlocked ) return true;
        if( mIsGmsRestrictedStamina && getStaminaMode() ) return true;
        return false;
    }


    public static boolean isGpsRestricted() {
        if( mIsGpsRestricted ) return true;
        if( mIsGmsRestrictedIdle && Runtime.isIdleMode() ) return true;
        if( mIsGmsRestrictedStamina && getStaminaMode() ) return true;
        return false;
    }

    public static AppProfile getCurrentProfile() {
        return AppProfileManager.getCurrentProfile();
    }

    public static void setTopApp(int uid, String packageName) {
        mTopAppUid = uid;
        synchronized(_staticLock) {
            mTopAppPackageName = packageName;
        }
    }

    public static int getTopAppUid() {
        return mTopAppUid;
    }

    public static String getTopAppPackageName() {
        synchronized(_staticLock) {
            return mTopAppPackageName;
        }
    }

    public BaikalSettings(Handler handler, Context context) {
        super(handler);
	    mHandler = handler;
        mContext = context;
        mResolver = context.getContentResolver();

        try {
                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_HALL_SENSOR),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_PROXIMITY_WAKE_SENSOR),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_PROXIMITY_SLEEP_SENSOR),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_TORCH_INCALL),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_AGGRESSIVE_IDLE),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_EXTREME_IDLE),
                    false, this);


                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_TORCH_NOTIFICATION),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_HIDE_GMS),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_UNRESTRICTED_NET),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_STAMINA_ENABLED),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_DISABLE_HP_DETECT),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_STAMINA_OI_ENABLED),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_GMS_RESTRICTED),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_GPS_RESTRICTED),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_GMS_IDLE_RESTRICTED),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_GMS_STAMINA_RESTRICTED),
                    false, this);

                mResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BAIKALOS_GMS_BLOCKED),
                    false, this);


            } catch( Exception e ) {
            }

            updateConstants(true);
            loadStaticConstants(mContext);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        loadStaticConstants(mContext);
        updateConstants(false);
    }

    private void updateConstants(boolean startup) {
        synchronized (_staticLock) {
            updateConstantsLocked(startup);
        }
    }

    public static void loadStaticConstants(Context context) {
        synchronized (_staticLock) {
            try {

                mIsGmsBlocked = Settings.Global.getInt(context.getContentResolver(),
                        Settings.Global.BAIKALOS_GMS_BLOCKED,0) == 1;

                mIsGmsRestricted = Settings.Global.getInt(context.getContentResolver(),
                        Settings.Global.BAIKALOS_GMS_RESTRICTED,0) == 1;

                mIsGpsRestricted = Settings.Global.getInt(context.getContentResolver(),
                        Settings.Global.BAIKALOS_GPS_RESTRICTED,0) == 1;

                mIsGmsRestrictedIdle = Settings.Global.getInt(context.getContentResolver(),
                        Settings.Global.BAIKALOS_GMS_IDLE_RESTRICTED,0) == 1;

                mIsGmsRestrictedStamina = Settings.Global.getInt(context.getContentResolver(),
                        Settings.Global.BAIKALOS_GMS_STAMINA_RESTRICTED,0) == 1;

                } catch (Exception e) {
                    Slog.e(TAG, "Bad BaikalService settings ", e);
                }
        }
    }

    public void updateConstantsLocked(boolean startup) {
        try {

            boolean hallSensorEnabled = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_HALL_SENSOR,0) == 1;

	        if( hallSensorEnabled != mHallSensorEnabled ) hallSensorEnabledChanged(hallSensorEnabled);

            boolean proximityWakeEnabled = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_PROXIMITY_WAKE_SENSOR,0) == 1;

            boolean proximitySleepEnabled = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_PROXIMITY_SLEEP_SENSOR,0) == 1;

            if( proximityWakeEnabled != mProximityWakeEnabled ||
                proximitySleepEnabled != mProximitySleepEnabled ) proximityEnabledChanged(proximityWakeEnabled,proximitySleepEnabled);

            boolean torchInCallEnabled = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_TORCH_INCALL,0) == 1;

            if( torchInCallEnabled != mTorchInCallEnabled ) torchInCallEnabledChanged(torchInCallEnabled);

            boolean aggressiveIdleEnabled = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_AGGRESSIVE_IDLE,0) == 1;

            boolean extremeIdleEnabled = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_EXTREME_IDLE,0) == 1;

            if( aggressiveIdleEnabled != mAggressiveIdleEnabled ||
		        extremeIdleEnabled != mExtremeIdleEnabled ) idleModeEnabledChanged(aggressiveIdleEnabled,extremeIdleEnabled);

            boolean torchNotificationEnabled = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_TORCH_NOTIFICATION,0) == 1;

            if( torchNotificationEnabled != mTorchNotificationEnabled ) torchNotificationEnabledChanged(torchNotificationEnabled);

            boolean hideGmsEnabled = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_HIDE_GMS,0) == 1;

            if( hideGmsEnabled != mHideGmsEnabled ) hideGmsEnabledChanged(hideGmsEnabled);

            boolean unrestrictedNetEnabled = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_UNRESTRICTED_NET,0) == 1;

            if( unrestrictedNetEnabled != mUnrestrictedNetEnabled ) unrestrictedNetEnabledChanged(unrestrictedNetEnabled);

            if( startup ) {
                Settings.Global.putInt(mResolver,
                    Settings.Global.BAIKALOS_STAMINA_ENABLED,0);
                mStaminaMode = false;
            } else {
                boolean staminaMode = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_STAMINA_ENABLED,0) == 1;
                if( staminaMode != mStaminaMode ) staminaModeChanged(staminaMode);
            }

            mDisableHeadphonesDetect = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_DISABLE_HP_DETECT,0) == 1;

            mStaminaOiMode = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_STAMINA_OI_ENABLED,0) == 1;

            mIsGmsRestricted = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_GMS_RESTRICTED,0) == 1;

            mIsGpsRestricted = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_GPS_RESTRICTED,0) == 1;

            mIsGmsRestrictedIdle = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_GMS_IDLE_RESTRICTED,0) == 1;

            mIsGmsRestrictedStamina = Settings.Global.getInt(mResolver,
                    Settings.Global.BAIKALOS_GMS_STAMINA_RESTRICTED,0) == 1;

            mHandler.removeMessages(Messages.MESSAGE_SETTINGS_UPDATE);
            Message msg = mHandler.obtainMessage(Messages.MESSAGE_SETTINGS_UPDATE);
            mHandler.sendMessage(msg);

        } catch (Exception e) {
            Slog.e(TAG, "Bad BaikalService settings ", e);
        }
    }

    void hallSensorEnabledChanged(boolean hall) {
	    mHallSensorEnabled = hall;
	}

	void proximityEnabledChanged(boolean wake, boolean sleep) {
	    mProximityWakeEnabled = wake;
	    mProximitySleepEnabled = sleep;
	}
	void torchInCallEnabledChanged(boolean enabled) {
	    mTorchInCallEnabled = enabled;
	}

	void torchNotificationEnabledChanged(boolean enabled) {
	    mTorchNotificationEnabled = enabled;
	}

	void hideGmsEnabledChanged(boolean enabled) {
	    mHideGmsEnabled = enabled;
	}

	void unrestrictedNetEnabledChanged(boolean enabled) {
	    mUnrestrictedNetEnabled = enabled;
	}

	void staminaModeChanged(boolean enabled) {
	    mStaminaMode = enabled;

        Actions.sendStaminaChanged(enabled);

        /*if( !mStaminaMode ) {
            final Intent bootIntent = new Intent(Intent.ACTION_BOOT_COMPLETED, null);
            bootIntent.putExtra(Intent.EXTRA_USER_HANDLE, 0);
            bootIntent.addFlags(Intent.FLAG_RECEIVER_NO_ABORT
                | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);

            mContext.sendBroadcastAsUser(bootIntent, UserHandle.ALL);
        }*/
	}

	void idleModeEnabledChanged(boolean aggressive, boolean extreme) {

	    if( mExtremeIdleEnabled != extreme ) {

	        mExtremeIdleEnabled = extreme;

            if( mExtremeIdleEnabled ) {
                Settings.Global.putInt(mResolver,
	    	        Settings.Global.FORCED_APP_STANDBY_ENABLED, mExtremeIdleEnabled ? 1:0);
            } else if( !mAggressiveIdleEnabled ) {
                Settings.Global.putInt(mResolver,
	    	        Settings.Global.FORCED_APP_STANDBY_ENABLED, mAggressiveIdleEnabled ? 1:0);
            }
            Settings.Global.putInt(mResolver,
                Settings.Global.FORCED_APP_STANDBY_FOR_SMALL_BATTERY_ENABLED, mExtremeIdleEnabled ? 1:0);

	    }

        
        if( mAggressiveIdleEnabled != aggressive ) {

	        mAggressiveIdleEnabled = aggressive;

            Settings.Global.putInt(mResolver,
		        Settings.Global.FORCED_APP_STANDBY_ENABLED, mAggressiveIdleEnabled ? 1:0);
        }

	}

    public static void setIdleMode(boolean mode) {
        if( mIdleMode != mode ) {
            mIdleMode = mode;

            if( mAggressiveIdleEnabled ) {
                Settings.Global.putInt(mResolver,
    		        Settings.Global.FORCED_APP_STANDBY_ENABLED, mIdleMode ? 1:0);
            }

    	    if( mExtremeIdleEnabled ) {
                Settings.Global.putInt(mResolver,
                    Settings.Global.FORCED_APP_STANDBY_FOR_SMALL_BATTERY_ENABLED, mIdleMode ? 1:0);
            }

            if( !mIdleMode && mStaminaOiMode ) {
                if( mContext != null ) {
                    final Intent bootIntent = new Intent(Intent.ACTION_BOOT_COMPLETED, null);
                    bootIntent.putExtra(Intent.EXTRA_USER_HANDLE, 0);
                    bootIntent.addFlags(Intent.FLAG_RECEIVER_NO_ABORT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);

                    mContext.sendBroadcastAsUser(bootIntent, UserHandle.ALL);
                }
            }
        }
    }
}
