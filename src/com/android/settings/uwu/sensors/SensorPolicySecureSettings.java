/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.sensors;

import android.app.UwuAppPolicyManager;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;

public final class SensorPolicySecureSettings {
    public static final int POLICY_ALLOW = Settings.Secure.UWU_APP_SENSOR_POLICY_ALLOW;
    public static final int POLICY_DENY_ON_LAUNCH =
            Settings.Secure.UWU_APP_SENSOR_POLICY_DENY_ON_LAUNCH;
    public static final int POLICY_DENY_ALWAYS = Settings.Secure.UWU_APP_SENSOR_POLICY_DENY_ALWAYS;

    private SensorPolicySecureSettings() {}

    public static synchronized ArrayMap<String, Integer> getPolicies(Context context) {
        final Bundle state = UwuAppPolicyManager.snapshot(UserHandle.myUserId());
        if (!UwuAppPolicyManager.migrated(state, UwuAppPolicyManager.FEATURE_SENSOR)) {
            throw new IllegalStateException("App policies not initialized");
        }
        final ArrayMap<String, Integer> result = new ArrayMap<>();
        for (String name : state.keySet()) {
            if (UwuAppPolicyManager.MIGRATIONS.equals(name)) continue;
            result.put(name, UwuAppPolicyManager.policy(state, name, UwuAppPolicyManager.SENSOR));
        }
        return result;
    }

    public static synchronized boolean setPolicy(Context context, String packageName, int policy) {
        try {
            UwuAppPolicyManager.setPolicy(
                    packageName, UserHandle.myUserId(), UwuAppPolicyManager.SENSOR, policy);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
