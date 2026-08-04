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

package com.android.settings.uwu;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

/** Shows the uwu extension entry only while its exported activity is usable. */
public final class TopLevelUwuSettingsExtPreferenceController extends BasePreferenceController {
    private static final String TAG = "TopLevelUwuSettings";
    private static final ComponentName COMPONENT = new ComponentName(
            "org.uwuaosp.settingsext",
            "org.uwuaosp.settingsext.SettingsExtActivity");

    public TopLevelUwuSettingsExtPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return getLaunchIntent() != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }
        final Intent intent = getLaunchIntent();
        if (intent == null) {
            return false;
        }
        try {
            mContext.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "uwuSettingsExt became unavailable before launch", e);
            return false;
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to launch uwuSettingsExt", e);
            return false;
        }
    }

    private Intent getLaunchIntent() {
        final PackageManager packageManager = mContext.getPackageManager();
        final ActivityInfo info;
        try {
            info = packageManager.getActivityInfo(
                    COMPONENT, PackageManager.MATCH_DISABLED_COMPONENTS);
            if (!info.exported || info.applicationInfo == null
                    || !isEnabled(packageManager, info)) {
                return null;
            }
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }

        final Intent intent = new Intent().setComponent(COMPONENT);
        return packageManager.resolveActivity(intent, 0) != null ? intent : null;
    }

    private static boolean isEnabled(PackageManager packageManager, ActivityInfo info) {
        final int componentState = packageManager.getComponentEnabledSetting(COMPONENT);
        if (componentState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                && (componentState != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                        || !info.enabled)) {
            return false;
        }
        final int applicationState =
                packageManager.getApplicationEnabledSetting(COMPONENT.getPackageName());
        return applicationState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                || (applicationState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                        && info.applicationInfo.enabled);
    }
}
