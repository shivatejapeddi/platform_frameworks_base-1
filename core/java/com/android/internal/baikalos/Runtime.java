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

public class Runtime { 

    private static final String TAG = "Baikal.Runtime";

	private static Object _staticGmsLock = new Object();
	private static Object _staticIdleLock = new Object();

	private static int mGmsUid = -1;
	private static int mGpsUid = -1;
	private static boolean mIdleMode = false;

	public static void setGmsUid(int uid) {
        mGmsUid = uid;
        Slog.i(TAG,"setGmsUid=" + uid);
	}

	public static void setGpsUid(int uid) {
        mGpsUid = uid;
        Slog.i(TAG,"setGpsUid=" + uid);
	}

	public static boolean isGpsUid(int id) {
        return id == mGpsUid;
    }

	public static int gmsUid() {
        return mGmsUid;
	}
	public static int gmsAppId() {
	    return UserHandle.getAppId(gmsUid());
	}

	public static boolean isGmsAppId(int id) {
	    return gmsAppId() == id;
	}

	public static boolean isGmsUid(int id) {
	    return gmsUid() == id;
	}

	public static boolean isIdleMode() {
	    synchronized(_staticIdleLock) {
	        return mIdleMode;
	    }
	}

	public static void setIdleMode(boolean mode) {
	    synchronized(_staticIdleLock) {
	        mIdleMode = mode;
	    }
	}
}
