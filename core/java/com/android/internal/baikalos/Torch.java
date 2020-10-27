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


import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

public class Torch extends MessageHandler { 

    private static final String TAG = "Baikal.Torch";

    private static boolean mTorchEnabled = false;

    private static CameraManager mCameraManager;
    private static String mRearFlashCameraId;
    private static Object mLock = new Object();

    @Override
    protected void initialize() {
	synchronized(mLock) {
	    mCameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
	    mCameraManager.registerTorchCallback(new TorchModeCallback(), getHandler());
	    try {
	    	mRearFlashCameraId = getRearFlashCameraId();
	    } catch (CameraAccessException e) {
	    }
	}
	if( Constants.DEBUG_TORCH ) Slog.i(TAG,"initialize()");                
    }


    @Override
    public boolean onMessage(Message msg) {
	return false;
    }

    public static void toggleTorch(boolean on) {
	synchronized(mLock) {
	    toggleTorchLocked(on);
	}
    }


    private static void toggleTorchLocked(boolean on) {
	if( on !=  mTorchEnabled ) {
            final boolean origEnabled = mTorchEnabled;
            try {
                if (mRearFlashCameraId != null) {
                    mCameraManager.setTorchMode(mRearFlashCameraId, on);
                    mTorchEnabled = on;
                }
            } catch (CameraAccessException e) {
            // Ignore
            }
        }
    }


    private static String getRearFlashCameraId() throws CameraAccessException {
        if (mRearFlashCameraId != null) return mRearFlashCameraId;
        for (final String id : mCameraManager.getCameraIdList()) {
            CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
            boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            int lensDirection = c.get(CameraCharacteristics.LENS_FACING);
            if (flashAvailable && lensDirection == CameraCharacteristics.LENS_FACING_BACK) {
                mRearFlashCameraId = id;
            }
        }
        return mRearFlashCameraId;
    }
    private static class TorchModeCallback extends CameraManager.TorchCallback {
        @Override
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            //if (!cameraId.equals(mRearFlashCameraId)) return;
            //mTorchEnabled = enabled;
            //if (!mTorchEnabled) {
            //    cancelTorchOff();
            //}
        }
        @Override
        public void onTorchModeUnavailable(String cameraId) {
            //if (!cameraId.equals(mRearFlashCameraId)) return;
            //mTorchEnabled = false;
            //cancelTorchOff();
        }
    }
}
