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


import android.annotation.AttrRes;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StyleRes;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;


import com.android.internal.R;

public abstract class MessageHandler { 

    abstract boolean onMessage(Message msg);
    abstract void initialize();

    private Context mContext;
    private Handler mHandler;

    public void initialize(Context context, Handler handler) {
	mContext = context;
	mHandler = handler;
	initialize();
    }

    public Context getContext() {
	return mContext;
    }

    public Handler getHandler() {
	return mHandler;
    }
}
