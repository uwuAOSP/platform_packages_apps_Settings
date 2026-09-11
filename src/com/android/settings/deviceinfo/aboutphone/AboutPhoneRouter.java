/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.deviceinfo.aboutphone;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.settings.Settings;

/** Provides the native fallback used when the SPA About phone page cannot be launched. */
public final class AboutPhoneRouter {
    private static final String TAG = "AboutPhoneRouter";

    private AboutPhoneRouter() {
    }

    public static Intent getRouteIntent(Context context) {
        return new Intent(context, AboutPhoneRouteActivity.class);
    }

    public static boolean launchNative(Activity activity) {
        final Intent intent = new Intent(activity, Settings.MyDeviceInfoActivity.class);
        try {
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Platform About phone activity is missing", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Platform About phone activity rejected the launch", e);
        } catch (RuntimeException e) {
            Log.e(TAG, "Platform About phone activity failed to launch", e);
        }
        return false;
    }
}
