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
import android.util.KeyValueListParser;

import android.os.UserHandle;
import android.os.Handler;
import android.os.Message;

import android.content.Context;
import android.content.ContentResolver;

import android.net.Uri;

import android.database.ContentObserver;

import android.provider.Settings;

import java.util.HashMap;

public class AppProfile {

    private static final String TAG = "Baikal.AppProfile";

    public String mPackageName;
    public int mBrightness;
    public String mPerfProfile;
    public String mThermalProfile;
    public int mFrameRate;
    public boolean mReader;
    public boolean mPinned;
    public int mBackground;
    public boolean mStamina;
    public boolean mRequireGms;

    public AppProfile() {
        mPerfProfile = "default";
        mThermalProfile = "default";
        mPackageName = "";
        mFrameRate = 0;
    }

    public boolean isDefault() {
        if( mBrightness == 0 &&
            !mReader &&
            !mPinned &&
            !mStamina &&
            !mRequireGms &&
            mFrameRate == 0 &&
            mBackground == 0 &&
            mPerfProfile.equals("default") &&
            mThermalProfile.equals("default") ) return true;
        return false;
    }

    public String Serialize() {
        if( mPackageName == null || mPackageName.equals("") ) return null;
        return "pn=" + mPackageName + "," +
        "br=" + mBrightness + "," +
        "pp=" + (mPerfProfile != null ? mPerfProfile : "") + "," +
        "tp=" + (mThermalProfile != null ? mThermalProfile : "") + "," +
        "rm=" + mReader + "," +
        "pd=" + mPinned + "," +
        "fr=" + mFrameRate + "," +
        "as=" + mStamina + "," +
        "bk=" + mBackground + "," +
        "gms=" + mRequireGms 
        ;
    }

    public static AppProfile Deserialize(String profileString) {

        KeyValueListParser parser = new KeyValueListParser(',');

        AppProfile profile = new AppProfile();
        try {
            parser.setString(profileString);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Bad profile settings", e);
            return null;
        }
        profile.mPackageName = parser.getString("pn",null);
        if( profile.mPackageName == null || profile.mPackageName.equals("") ) return null;
        try {
            profile.mBrightness = parser.getInt("br",0);
            profile.mPerfProfile = parser.getString("pp","default");
            profile.mThermalProfile = parser.getString("tp","default");
            profile.mReader = parser.getBoolean("rm",false);
            profile.mPinned = parser.getBoolean("pd",false);
            profile.mStamina = parser.getBoolean("as",false);
            profile.mFrameRate = parser.getInt("fr",0);
            profile.mBackground = parser.getInt("bk",0);
            profile.mRequireGms = parser.getBoolean("gms",false);
        } catch( Exception e ) {

        }
        return profile;
    }
}
