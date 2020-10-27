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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class Bluetooth extends MessageHandler { 

    private static final String TAG = "Baikal.Bluetooth";

    @Override
    protected void initialize() {

	if( Constants.DEBUG_BLUETOOTH ) Slog.i(TAG,"initialize()");                

        IntentFilter btFilter = new IntentFilter();
        btFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        btFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        btFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        getContext().registerReceiver(mBluetoothReceiver, btFilter);
    }


    @Override
    public boolean onMessage(Message msg) {
	return false;
    }


    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //synchronized (BaikalService.this) {
                String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    updateBluetoothDeviceState(0,device);
                } else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    updateBluetoothDeviceState(1,device);
                } else if(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                    updateBluetoothDeviceState(2,device);
                }
            //}
        }
    };

    private void updateBluetoothDeviceState(int state, BluetoothDevice device) {
       	if( Constants.DEBUG_BLUETOOTH ) Slog.i(TAG,"updateBluetoothDeviceState: state=" + state + ", device=" + device);                
        Intent intent = new Intent(Actions.ACTION_BLUETOOTH_DEVICE_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
	intent.putExtra(Actions.EXTRA_INT_MODE,state);
	intent.putExtra(Actions.EXTRA_BT_DEVICE,device);
	Actions.enqueueIntent(intent);
    }
}
