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

import android.content.Intent;
import android.content.IntentFilter;

import android.content.BroadcastReceiver;

import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.CellLocation;
import android.telephony.CellInfo;
import android.telephony.SignalStrength;
import android.telephony.PreciseCallState;
import android.telephony.PreciseDataConnectionState;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.VoLteServiceState;

import java.util.List;

public class Telephony extends MessageHandler { 

    private static final String TAG = "Baikal.Telephony";

    boolean mTorchOnCall = false;
    boolean mActiveIncomingCall = false;
    Thread mTorchThread = null;

   @Override
    protected void initialize() {
	    TelephonyManager telephonyManager =
	       (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
      	telephonyManager.listen(mPhoneStateListener, 0xFFFFFFF);
        if( Constants.DEBUG_TELEPHONY ) Slog.i(TAG,"initialize()");
        updateSettings();                
    }


    @Override
    public boolean onMessage(Message msg) {
	if( msg.what == Messages.MESSAGE_SETTINGS_UPDATE ) {
	    updateSettings();
	    return false;
	}
	return false;
    }

    private void updateSettings() {
	if( mTorchOnCall != BaikalSettings.getTorchInCallEnabled() ) {
	    mTorchOnCall = BaikalSettings.getTorchInCallEnabled();
	}
    }

    public void setTorchOnIncomingCall(boolean mode) {
    	mTorchOnCall = mode;
    }


    private void onCallStateChangedLocked(int state, String phoneNumber) {

        if( Constants.DEBUG_TELEPHONY ) {
            Slog.i(TAG,"PhoneStateListener: onCallStateChanged(" + state + "," + phoneNumber + ")");
	    }

	//if( !mTorchOnCall ) return;
        /*
        if( state == 1 ) {
            mActiveIncomingCall = true;
        } else {
            mActiveIncomingCall = false;
        }
        if( mActiveIncomingCall && mTorchThread == null ) {
            mTorchThread = new Thread(() -> {
                while( true ) {
        		    boolean torchOnCall = false;
        		    synchronized(Telephony.this) {
        	    		torchOnCall = mTorchOnCall;
        		    }
        		    if( torchOnCall ) {
        	            try {
        	                Torch.toggleTorch(true);
        	                Thread.sleep(30);
        	                Torch.toggleTorch(false);
        	                Thread.sleep(50);
                            Torch.toggleTorch(true);
        	                Thread.sleep(30);
        	                Torch.toggleTorch(false);
        	                Thread.sleep(50);
                        
        	                Torch.toggleTorch(true);
        	                Thread.sleep(30);
        	                Torch.toggleTorch(false);
        	                Thread.sleep(1500);
        	            } catch( Exception e ) {
        	            }
        		    } else {
        		    	try {
            			    Thread.sleep(1500);
        	    		} catch( Exception e ) {
        		        }
        		    }

                    synchronized(Telephony.this) {
                        if( !mTorchOnCall || !mActiveIncomingCall ) {
                            mTorchThread = null;
                            return;
                        }
                    }
                }
            }); 
            mTorchThread.start();
        }*/
    }

    private BroadcastReceiver mRingerModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                //mHandler.sendEmptyMessage(MESSAGE_REFRESH);
            //}

            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"mRingerModeReceiver: onReceive(" + intent + ")");
            }

        }
    };

    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            //if (!mHasTelephony) return;
            //final boolean inAirplaneMode = serviceState.getState() == ServiceState.STATE_POWER_OFF;
            //mAirplaneState = inAirplaneMode ? ToggleAction.State.On : ToggleAction.State.Off;
            //mAirplaneModeOn.updateState(mAirplaneState);
            //mAdapter.notifyDataSetChanged();
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onServiceStateChanged(" + serviceState + ")");
            }

        }

        /**
         * Callback invoked when network signal strength changes.
         *
         * @see ServiceState#STATE_EMERGENCY_ONLY
         * @see ServiceState#STATE_IN_SERVICE
         * @see ServiceState#STATE_OUT_OF_SERVICE
         * @see ServiceState#STATE_POWER_OFF
         * @deprecated Use {@link #onSignalStrengthsChanged(SignalStrength)}
         */
        @Override
        public void onSignalStrengthChanged(int asu) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onSignalStrengthChanged(" + asu + ")");
            }

            // default implementation empty
        }

        /**
         * Callback invoked when the message-waiting indicator changes.
         */
        @Override
        public void onMessageWaitingIndicatorChanged(boolean mwi) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onMessageWaitingIndicatorChanged(" + mwi + ")");
            }
            // default implementation empty
        }

        /**
         * Callback invoked when the call-forwarding indicator changes.
         */
        @Override
        public void onCallForwardingIndicatorChanged(boolean cfi) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onCallForwardingIndicatorChanged(" + cfi + ")");
            }
            // default implementation empty
        }

        /**
         * Callback invoked when device cell location changes.
         */
        @Override
        public void onCellLocationChanged(CellLocation location) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onCellLocationChanged(" + location + ")");
            }
            // default implementation empty
        }

        /**
         * Callback invoked when device call state changes.
         * @param state call state
         * @param incomingNumber incoming call phone number. If application does not have
         * {@link android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE} permission, an empty
         * string will be passed as an argument.
         *
         * @see TelephonyManager#CALL_STATE_IDLE
         * @see TelephonyManager#CALL_STATE_RINGING
         * @see TelephonyManager#CALL_STATE_OFFHOOK
         */
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onCallStateChanged(" + state + "," + incomingNumber + ")");
            }

            synchronized (this) {
                onCallStateChangedLocked(state,incomingNumber);
            }

        // default implementation empty
        }

        /**
         * Callback invoked when connection state changes.
         *
         * @see TelephonyManager#DATA_DISCONNECTED
         * @see TelephonyManager#DATA_CONNECTING
         * @see TelephonyManager#DATA_CONNECTED
         * @see TelephonyManager#DATA_SUSPENDED
         */
        @Override
        public void onDataConnectionStateChanged(int state) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onDataConnectionStateChanged(" + state + ")");
            }
            // default implementation empty
        }

        /**
         * same as above, but with the network type.  Both called.
         */
        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onDataConnectionStateChanged(" + state + "," + networkType + ")");
            }
        }

        /**
         * Callback invoked when data activity state changes.
         *
         * @see TelephonyManager#DATA_ACTIVITY_NONE
         * @see TelephonyManager#DATA_ACTIVITY_IN
         * @see TelephonyManager#DATA_ACTIVITY_OUT
         * @see TelephonyManager#DATA_ACTIVITY_INOUT
         * @see TelephonyManager#DATA_ACTIVITY_DORMANT
         */
        @Override
        public void onDataActivity(int direction) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onDataActivity(" + direction + ")");
            }
            // default implementation empty
        }

        /**
         * Callback invoked when network signal strengths changes.
         *
         * @see ServiceState#STATE_EMERGENCY_ONLY
         * @see ServiceState#STATE_IN_SERVICE
         * @see ServiceState#STATE_OUT_OF_SERVICE
         * @see ServiceState#STATE_POWER_OFF
         */
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onSignalStrengthsChanged(" + signalStrength + ")");
            }
            // default implementation empty
        }


        /**
         * The Over The Air Service Provisioning (OTASP) has changed. Requires
         * the READ_PHONE_STATE permission.
         * @param otaspMode is integer <code>OTASP_UNKNOWN=1<code>
         *   means the value is currently unknown and the system should wait until
         *   <code>OTASP_NEEDED=2<code> or <code>OTASP_NOT_NEEDED=3<code> is received before
         *   making the decision to perform OTASP or not.
         *
         * @hide
         */

        /* Seems unavailable in A11
         @Override
        /public void onOtaspChanged(int otaspMode) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onOtaspChanged(" + otaspMode + ")");
            }
            // default implementation empty
        }*/

        /**
         * Callback invoked when a observed cell info has changed,
         * or new cells have been added or removed.
         * @param cellInfo is the list of currently visible cells.
         */
        @Override
        public void onCellInfoChanged(List<CellInfo> cellInfo) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onCellInfoChanged(" + cellInfo + ")");
            }
        }

        /**
         * Callback invoked when precise device call state changes.
         *
         * @hide
         */
        @Override
        public void onPreciseCallStateChanged(PreciseCallState callState) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onPreciseCallStateChanged(" + callState + ")");
            }
            // default implementation empty
        }

        /**
         * Callback invoked when data connection state changes with precise information.
         *
         * @hide
         */
        @Override
        public void onPreciseDataConnectionStateChanged(
                PreciseDataConnectionState dataConnectionState) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onPreciseDataConnectionStateChanged(" + dataConnectionState + ")");
            }
            // default implementation empty
        }

        /**
         * Callback invoked when data connection state changes with precise information.
         *
         * @hide
         */
        @Override
        public void onDataConnectionRealTimeInfoChanged(
                DataConnectionRealTimeInfo dcRtInfo) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onDataConnectionRealTimeInfoChanged(" + dcRtInfo + ")");
            }
            // default implementation empty
        }

        /**
         * Callback invoked when the service state of LTE network
         * related to the VoLTE service has changed.
         * @param stateInfo is the current LTE network information
         * @hide
         */
        /*@Override
        public void onVoLteServiceStateChanged(VoLteServiceState stateInfo) {
            if( DEBUG ) {
                Slog.i(TAG,"PhoneStateListener: onVoLteServiceStateChanged(" + stateInfo + ")");
            }
        }*/

        /**
         * Callback invoked when the SIM voice activation state has changed
         * @param state is the current SIM voice activation state
         * @hide
         */
        @Override
        public void onVoiceActivationStateChanged(int state) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onVoiceActivationStateChanged(" + state + ")");
            }
   
        }

        /**
         * Callback invoked when the SIM data activation state has changed
         * @param state is the current SIM data activation state
         * @hide
         */
        @Override
        public void onDataActivationStateChanged(int state) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onDataActivationStateChanged(" + state + ")");
            }
    
        }

        /**
         * Callback invoked when OEM hook raw event is received. Requires
         * the READ_PRIVILEGED_PHONE_STATE permission.
         * @param rawData is the byte array of the OEM hook raw data.
         * @hide
         */
        @Override
        public void onOemHookRawEvent(byte[] rawData) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onOemHookRawEvent( raw )");
            }
            // default implementation empty
        }

        /**
         * Callback invoked when telephony has received notice from a carrier
         * app that a network action that could result in connectivity loss
         * has been requested by an app using
         * {@link android.telephony.TelephonyManager#notifyCarrierNetworkChange(boolean)}
         *
         * @param active Whether the carrier network change is or shortly
         *               will be active. This value is true to indicate
         *               showing alternative UI and false to stop.
         *
         * @hide
         */
        @Override
        public void onCarrierNetworkChange(boolean active) {
            if( Constants.DEBUG_TELEPHONY_RAW ) {
                Slog.i(TAG,"PhoneStateListener: onCarrierNetworkChange(" + active + ")");
            }
            // default implementation empty
        }

    };
}
