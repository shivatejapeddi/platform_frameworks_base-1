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
import android.os.Handler;
import android.os.Message;

import android.os.Process;
import android.os.SystemClock;

import android.os.PowerManager;
import android.os.PowerManagerInternal;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SystemSensorManager;


public class Sensors extends MessageHandler { 

    private static final String TAG = "Baikal.Sensors";

    private static final int SENSOR_HALL_TYPE=33171016;

    // The sensor manager.
    private SensorManager mSensorManager;

    private ProximityService mProximityService;
    private HallSensorService mHallSensorService;
    PowerManager.WakeLock mProximityWakeLock;
    PowerManager mPowerManager;

    private boolean mProximityServiceWakeupEnabled = false;
    private boolean mProximityServiceSleepEnabled = false;
    private boolean mHallSensorServiceEnabled = false;

   

   @Override
    protected void initialize() {
	if( Constants.DEBUG_SENSORS ) Slog.i(TAG,"initialize()");

            mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
	    if( Constants.DEBUG_SENSORS ) Slog.i(TAG,"SensorManager initialized");

	    mPowerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
	    mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "*baikal_proximity*");

	    mProximityService = new ProximityService();
	    mProximityService.Initialize();

	    mHallSensorService = new HallSensorService();
	    mHallSensorService.Initialize();
        updateSettings();
    }

    @Override
    public boolean onMessage(Message msg) {
        if( msg.what == Messages.MESSAGE_SETTINGS_UPDATE ) {
	        updateSettings();
	        return false;
        }
        if( msg.what == Messages.MESSAGE_SENSOR_PROXIMITY_TIMEOUT ) {
            if( mProximityService != null ) {
                mProximityService.handleProximityTimeout();
            }
            return true;
        }
        return false;
    }

    private void updateSettings() {
        if( mProximityServiceWakeupEnabled != BaikalSettings.getProximityWakeEnabled() |
            mProximityServiceSleepEnabled != BaikalSettings.getProximitySleepEnabled() |
            mHallSensorServiceEnabled != BaikalSettings.getHallSensorEnabled() ) {
                setProximitySensorEnabled(BaikalSettings.getProximityWakeEnabled(),
                    BaikalSettings.getProximitySleepEnabled(),
                    BaikalSettings.getHallSensorEnabled());
                setHallSensorEnabled(BaikalSettings.getHallSensorEnabled());
    	}
    }

    public void setProximitySensorEnabled(boolean proxymityWake,boolean proxymitySleep,boolean hall) {

	mProximityServiceWakeupEnabled = proxymityWake;
	mProximityServiceSleepEnabled = proxymitySleep;
	mHallSensorServiceEnabled = hall;

	if( mProximityService != null ) {
            mProximityService.setProximitySensorEnabled(mProximityServiceWakeupEnabled | mProximityServiceSleepEnabled | mHallSensorServiceEnabled);
	}
    }

    public void setHallSensorEnabled(boolean hall) {

	mHallSensorServiceEnabled = hall;

	if( mHallSensorService != null ) {
            mHallSensorService.setHallSensorEnabled(mHallSensorServiceEnabled);
	}
    }

    void goToSleep() {
        if( mPowerManager != null ) {
            mPowerManager.goToSleep(SystemClock.uptimeMillis(), PowerManager.GO_TO_SLEEP_REASON_POWER_BUTTON, 0);
        }
    }

    void wakeUp() {
        if( mPowerManager != null ) {
            mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.policy:LID");
        }
    }


    final class ProximityService {
        // The proximity sensor, or null if not available or needed.
        private Sensor mProximitySensor;
        private float  mProximityThreshold;
        private boolean mProximitySensorEnabled;

        private long proximityPositiveTime;
        private long proximityNegativeTime;
        private long proximityClickStart;
        private long proximityClickCount;


        private final SensorEventListener mProximitySensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (mProximitySensorEnabled) {
                    final long time = SystemClock.uptimeMillis();
                    final float distance = event.values[0];
                    boolean positive = distance >= 0.0f && distance < mProximityThreshold;
                    handleProximitySensorEvent(time, positive);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Not used.
            }
        };


        void Initialize() {
            mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY,true);
            if( mProximitySensor != null ) { 
                mProximityThreshold = mProximitySensor.getMaximumRange();
            }
            if( Constants.DEBUG_SENSORS ) {
                Slog.i(TAG,"Proximity Initialize: sensor=" + mProximitySensor);
            }
        }

        void setProximitySensorEnabled(boolean enable) {
            if( Constants.DEBUG_SENSORS ) {
                Slog.i(TAG,"setProximitySensorEnabled: enable=" + enable);
            }
            if( mProximitySensor == null ) return; 
            if (enable) {
                if (!mProximitySensorEnabled) {
                    mProximitySensorEnabled = true;
                    mSensorManager.registerListener(mProximitySensorListener, mProximitySensor,
                        SensorManager.SENSOR_DELAY_FASTEST, 1000000);
                }
            } else {
                if (mProximitySensorEnabled) {
                    mProximitySensorEnabled = false;
                    mSensorManager.unregisterListener(mProximitySensorListener);
                }
            }
        }

        private void handleProximitySensorEvent(long time, boolean positive) {
            if (mProximitySensorEnabled) {

                boolean isInteractive = mPowerManager.isInteractive();

                if( Constants.DEBUG_SENSORS ) {
                    Slog.i(TAG,"handleProximitySensorEvent: value=" + positive + ", time=" + time);
                }

                if( (!mProximityServiceWakeupEnabled && !isInteractive) ||
                    (!mProximityServiceSleepEnabled && isInteractive) ) {
                    AcquireWakelock(true);
                    return;
                }

                AcquireWakelock(false);

                //final long now = SystemClock.elapsedRealtime();
                if( positive == false ) {
                    proximityNegativeTime = time;
                    if( (time - proximityClickStart) < 2500 ) {
                        proximityClickCount++;
                        if( Constants.DEBUG_SENSORS ) {
                            Slog.i(TAG,"handleProximitySensorEvent: open <2000 :" + (time-proximityClickStart) + ":" + proximityClickCount);
                        }
                        if( proximityClickCount == 2 ) {
                            handleWakeup(isInteractive);
                        }
                    } else {
                        if( Constants.DEBUG_SENSORS ) {
                            Slog.i(TAG,"handleProximitySensorEvent: open >2000 :" + (time-proximityClickStart) + ":0");
                        }
                        proximityClickCount = 0;
                    }
                } else {
                    proximityPositiveTime = time;
                    if( (time - proximityClickStart) > 2500 ) {
                        proximityClickStart = time;
                        proximityClickCount = 0;
                        if( Constants.DEBUG_SENSORS ) {
                            Slog.i(TAG,"handleProximitySensorEvent: closed > 2000 :" + (time-proximityClickStart) + ":0");
                        }
                    } else {
                        if( Constants.DEBUG_SENSORS ) {
                            Slog.i(TAG,"handleProximitySensorEvent: closed < 2000 :" + (time-proximityClickStart) + ":" + proximityClickCount);
                        }
                    }
                }
            }
        }

        void handleWakeup(boolean interactive) {
            if( Constants.DEBUG_SENSORS ) {
                Slog.i(TAG,"handleProximitySensorWakeup()");
            }
            if( mPowerManager != null && interactive ) {
                goToSleep();
            } else {
                wakeUp();
            }
        }
        
        void handleProximityTimeout() {
            ReleaseWakelock();
        }

        void setProximityTimeout(boolean wakeonly) {
            final long timeout = wakeonly?500:3000; 
            getHandler().removeMessages(Messages.MESSAGE_SENSOR_PROXIMITY_TIMEOUT);
            Message msg = getHandler().obtainMessage(Messages.MESSAGE_SENSOR_PROXIMITY_TIMEOUT);
            getHandler().sendMessageDelayed(msg,timeout);
        }

        private void ReleaseWakelock() {
            if (mProximityWakeLock.isHeld()) {
                if( Constants.DEBUG_SENSORS ) {
                    Slog.i(TAG,"ProximitySensor: ReleaseWakelock()");
                }
                mProximityWakeLock.release();
            }
        }

        private void AcquireWakelock(boolean wakeonly) {
            setProximityTimeout(wakeonly);
            if (!mProximityWakeLock.isHeld()) {
                if( Constants.DEBUG_SENSORS ) {
                    Slog.i(TAG,"ProximitySensor: AcquireWakelock()");
                }
                mProximityWakeLock.acquire();
            }
        }

    }

    final class HallSensorService {
        // The hall sensor, or null if not available or needed.
        private Sensor mHallSensor;
        private boolean mHallSensorEnabled;

        private final SensorEventListener mHallSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (mHallSensorEnabled) {
                    final long time = SystemClock.uptimeMillis();
                    final float distance = event.values[0];
                    boolean positive = distance > 0.0f;
                    handleHallSensorEvent(time, positive);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Not used.
            }
        };


        void Initialize() {
            mHallSensor = mSensorManager.getDefaultSensor(SENSOR_HALL_TYPE, true);
            if( Constants.DEBUG_SENSORS ) {
                Slog.i(TAG,"Hall Initialize (wakeup): sensor=" + mHallSensor);
            }
            if( mHallSensor == null ) {
                mHallSensor = mSensorManager.getDefaultSensor(SENSOR_HALL_TYPE,false);
                if( Constants.DEBUG_SENSORS ) {
                    Slog.i(TAG,"Hall Initialize (non-wake): sensor=" + mHallSensor);
                }
            }
        }

        void setHallSensorEnabled(boolean enable) {
            if( Constants.DEBUG_SENSORS ) {
                Slog.i(TAG,"setHallSensorEnabled: enable=" + enable);
            }
            if( mHallSensor == null ) return; 
            if (enable) {
                if (!mHallSensorEnabled) {
                    mHallSensorEnabled = true;
                    mSensorManager.registerListener(mHallSensorListener, mHallSensor,
                        SensorManager.SENSOR_DELAY_FASTEST, 1000000);
                }
            } else {
                if (mHallSensorEnabled) {
                    mHallSensorEnabled = false;
                    mSensorManager.unregisterListener(mHallSensorListener);
                }
            }
        }

        private void handleHallSensorEvent(long time, boolean positive) {
            if (mHallSensorEnabled) {
                if( Constants.DEBUG_SENSORS ) {
                    Slog.i(TAG,"handleHallSensorEvent: value=" + positive + ", time=" + time);
                }
                handleWakeup(!positive);
            }
        }

        void handleWakeup(boolean wakeup) {
            if( Constants.DEBUG_SENSORS ) {
                Slog.i(TAG,"handleHallSensorWakeup()");
            }
            if( wakeup ) {
                wakeUp();
            } else {
                goToSleep();
            }
        }
    }

}

