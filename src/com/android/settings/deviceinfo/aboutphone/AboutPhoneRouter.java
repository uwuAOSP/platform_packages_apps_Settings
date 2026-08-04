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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settings.Settings;

/** Routes the top-level About phone entry to SettingsExt with a platform fallback. */
public final class AboutPhoneRouter {
    private static final String TAG = "AboutPhoneRouter";

    static final ComponentName EXTERNAL_COMPONENT = new ComponentName(
            "org.uwuaosp.settingsext",
            "org.uwuaosp.settingsext.aboutphone.AboutPhoneActivity");

    private AboutPhoneRouter() {
    }

    /** Returns an internal trampoline intent which guarantees a native fallback. */
    public static Intent getRouteIntent(Context context) {
        return new Intent(context, AboutPhoneRouteActivity.class);
    }

    /** Starts SettingsExt if its About phone activity is installed, enabled and resolvable. */
    public static boolean launchExternal(Context context) {
        final Intent intent = getExternalIntent(context);
        if (intent == null) {
            return false;
        }
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "SettingsExt About phone disappeared before launch", e);
        } catch (SecurityException e) {
            Log.w(TAG, "SettingsExt About phone rejected the launch", e);
        } catch (RuntimeException e) {
            Log.w(TAG, "SettingsExt About phone failed to launch", e);
        }
        return false;
    }

    /** Starts the preserved platform About phone activity. */
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

    @Nullable
    static Intent getExternalIntent(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final ActivityInfo activityInfo;
        try {
            activityInfo = packageManager.getActivityInfo(
                    EXTERNAL_COMPONENT, PackageManager.MATCH_DISABLED_COMPONENTS);
            if (!isEnabled(packageManager, activityInfo)
                    || !activityInfo.exported
                    || !canCall(context, activityInfo.permission)) {
                return null;
            }
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }

        final Intent intent = new Intent().setComponent(EXTERNAL_COMPONENT);
        final ResolveInfo resolveInfo;
        try {
            resolveInfo = packageManager.resolveActivity(intent, 0);
        } catch (RuntimeException e) {
            return null;
        }
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return null;
        }
        final ComponentName resolvedComponent = new ComponentName(
                resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        return EXTERNAL_COMPONENT.equals(resolvedComponent) ? intent : null;
    }

    private static boolean isEnabled(PackageManager packageManager, ActivityInfo activityInfo) {
        if (activityInfo.applicationInfo == null) {
            return false;
        }

        final int componentState =
                packageManager.getComponentEnabledSetting(EXTERNAL_COMPONENT);
        final boolean componentEnabled =
                componentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        || (componentState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                                && activityInfo.enabled);
        if (!componentEnabled) {
            return false;
        }

        final int applicationState =
                packageManager.getApplicationEnabledSetting(EXTERNAL_COMPONENT.getPackageName());
        return applicationState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                || (applicationState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                        && activityInfo.applicationInfo.enabled);
    }

    private static boolean canCall(Context context, @Nullable String permission) {
        return TextUtils.isEmpty(permission)
                || context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
}
