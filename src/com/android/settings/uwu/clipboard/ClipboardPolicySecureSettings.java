/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.clipboard;

import android.app.UwuAppPolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;

import com.android.internal.app.ClipboardAccessPolicy;

public final class ClipboardPolicySecureSettings {
    public static final int OPERATION_READ = 0;
    public static final int OPERATION_WRITE = 1;
    public static final int POLICY_ALLOW = Settings.Secure.UWU_APP_CLIPBOARD_POLICY_ALLOW;
    public static final int POLICY_ASK = Settings.Secure.UWU_APP_CLIPBOARD_POLICY_ASK;
    public static final int POLICY_DENY = Settings.Secure.UWU_APP_CLIPBOARD_POLICY_DENY;

    private ClipboardPolicySecureSettings() {}

    public static boolean isPromptEnabled(Context context) {
        return Settings.Secure.getIntForUser(
                        context.getContentResolver(),
                        Settings.Secure.UWU_APP_CLIPBOARD_PROMPTS_ENABLED,
                        0,
                        UserHandle.myUserId())
                != 0;
    }

    public static boolean setPromptEnabled(Context context, boolean enabled) {
        return Settings.Secure.putIntForUser(
                context.getContentResolver(),
                Settings.Secure.UWU_APP_CLIPBOARD_PROMPTS_ENABLED,
                enabled ? 1 : 0,
                UserHandle.myUserId());
    }

    public static int getDefaultPolicy(boolean promptsEnabled, ApplicationInfo info) {
        final boolean systemApp =
                (info.flags
                                & (ApplicationInfo.FLAG_SYSTEM
                                        | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))
                        != 0;
        return ClipboardAccessPolicy.shouldAskByDefault(promptsEnabled, systemApp, info.packageName)
                ? POLICY_ASK
                : POLICY_ALLOW;
    }

    public static synchronized ArrayMap<String, Integer> getPolicies(
            Context context, int operation) {
        final int type = policyType(operation);
        final Bundle state = UwuAppPolicyManager.snapshot(UserHandle.myUserId());
        if (!UwuAppPolicyManager.migrated(state, UwuAppPolicyManager.FEATURE_CLIPBOARD)) {
            throw new IllegalStateException("App policies not initialized");
        }
        final ArrayMap<String, Integer> result = new ArrayMap<>();
        for (String name : state.keySet()) {
            if (UwuAppPolicyManager.MIGRATIONS.equals(name)) continue;
            int policy = UwuAppPolicyManager.policy(state, name, type);
            if (policy != UwuAppPolicyManager.INHERIT) result.put(name, policy);
        }
        return result;
    }

    public static synchronized boolean setPolicy(
            Context context, String packageName, int operation, int policy) {
        try {
            UwuAppPolicyManager.setPolicy(
                    packageName, UserHandle.myUserId(), policyType(operation), policy);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static int policyType(int operation) {
        if (operation == OPERATION_READ) return UwuAppPolicyManager.CLIPBOARD_READ;
        if (operation == OPERATION_WRITE) return UwuAppPolicyManager.CLIPBOARD_WRITE;
        throw new IllegalArgumentException("Unknown clipboard operation");
    }
}
