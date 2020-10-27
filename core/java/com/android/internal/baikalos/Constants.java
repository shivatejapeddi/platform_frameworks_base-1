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

public class Constants { 

    public static final boolean DEBUG = true;
    public static final boolean DEBUG_RAW = false;
    public static final boolean DEBUG_TEMPLATE = DEBUG | false;
    public static final boolean DEBUG_SENSORS = DEBUG | false;
    public static final boolean DEBUG_TORCH = DEBUG | false;
    public static final boolean DEBUG_TELEPHONY = DEBUG | false;
    public static final boolean DEBUG_TELEPHONY_RAW = DEBUG_RAW | false;
    public static final boolean DEBUG_BLUETOOTH = DEBUG | false;
    public static final boolean DEBUG_ACTIONS = DEBUG | false;
    public static final boolean DEBUG_APP_PROFILE = DEBUG | false;
    public static final boolean DEBUG_DEV_PROFILE = DEBUG | false;

    public static final int MESSAGE_MIN = 10000;

    public static final int MESSAGE_SETTINGS = MESSAGE_MIN;
    public static final int MESSAGE_ACTIONS = MESSAGE_SETTINGS +1000;
    public static final int MESSAGE_SENSORS = MESSAGE_ACTIONS + 1000;
    public static final int MESSAGE_TELEPHONY = MESSAGE_SENSORS + 1000;
    public static final int MESSAGE_TORCH = MESSAGE_TELEPHONY + 1000;
    public static final int MESSAGE_ACTION = MESSAGE_TORCH + 1000;

    public static final int MESSAGE_APP_PROFILE = MESSAGE_ACTION + 1000;
    public static final int MESSAGE_DEV_PROFILE = MESSAGE_APP_PROFILE + 1000;

    public static final int MESSAGE_MAX = MESSAGE_DEV_PROFILE + 1000;
}
