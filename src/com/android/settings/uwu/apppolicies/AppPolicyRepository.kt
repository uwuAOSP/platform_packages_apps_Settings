/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.uwu.apppolicies

import android.app.UwuAppPolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import com.android.settings.R
import com.android.settings.uwu.appjump.AppJumpPolicyBackend
import com.android.settings.uwu.clipboard.ClipboardPolicySecureSettings
import com.android.settings.uwu.sensors.SensorPolicySecureSettings

internal object AppPolicyRepository {
    const val AUTHORITY = "com.android.settings.uwu.apppolicies"
    const val ACTION = "com.android.settings.uwu.action.MANAGE_APP_POLICY"
    const val EXTRA_FEATURE = "feature"
    val features = listOf("clipboard", "jump", "sensor")

    fun requireApp(context: Context, name: String): ApplicationInfo {
        require(name.isNotBlank())
        val info =
            context.packageManager.getApplicationInfo(
                name,
                PackageManager.MATCH_DISABLED_COMPONENTS,
            )
        require(info.uid >= 0 && UserHandle.getUserId(info.uid) == UserHandle.myUserId())
        return info
    }

    fun snapshot(): Bundle =
        UwuAppPolicyManager.snapshot(UserHandle.myUserId()).also {
            check(it.getInt(UwuAppPolicyManager.MIGRATIONS) and 7 == 7) {
                "App policies not initialized"
            }
        }

    fun clipboardLabel(policy: Int): Int =
        when (policy) {
            0 -> R.string.clipboard_policy_allow
            1 -> R.string.clipboard_policy_ask
            2 -> R.string.clipboard_policy_deny
            else -> error("Invalid clipboard policy")
        }

    fun sensorLabel(policy: Int): Int =
        when (policy) {
            0 -> R.string.sensor_policy_allow
            1 -> R.string.sensor_policy_deny_on_launch
            2 -> R.string.sensor_policy_deny_always
            else -> error("Invalid sensor policy")
        }

    fun jumpLabel(policy: Int): Int =
        when (policy) {
            0 -> R.string.app_jump_mode_ask_summary
            1 -> R.string.app_jump_mode_allow_summary
            2 -> R.string.app_jump_mode_block_summary
            else -> error("Invalid jump policy")
        }

    fun clipboardPolicy(context: Context, info: ApplicationInfo, state: Bundle, type: Int): Int {
        val explicit = UwuAppPolicyManager.policy(state, info.packageName, type)
        return if (explicit != UwuAppPolicyManager.INHERIT) explicit
        else
            ClipboardPolicySecureSettings.getDefaultPolicy(
                ClipboardPolicySecureSettings.isPromptEnabled(context),
                info,
            )
    }

    fun clipboardEditable(context: Context, app: ApplicationInfo): Boolean {
        val ime =
            android.content.ComponentName.unflattenFromString(
                android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.DEFAULT_INPUT_METHOD,
                ) ?: ""
            )
        return UserHandle.isApp(app.uid) && ime?.packageName != app.packageName
    }

    fun summaries(context: Context, name: String): Bundle {
        val app = requireApp(context, name)
        val state = snapshot()
        val backend = AppJumpPolicyBackend(context)
        val read = clipboardPolicy(context, app, state, UwuAppPolicyManager.CLIPBOARD_READ)
        val write = clipboardPolicy(context, app, state, UwuAppPolicyManager.CLIPBOARD_WRITE)
        val clipboard =
            context.getString(
                R.string.app_policy_clipboard_summary,
                context.getString(clipboardLabel(read)),
                context.getString(clipboardLabel(write)),
            )
        val clipboardEditable = clipboardEditable(context, app)
        return Bundle().apply {
            putBoolean("clipboard_editable", clipboardEditable)
            putString(
                "clipboard_category",
                when {
                    !clipboardEditable -> "allowed"
                    read == 2 || write == 2 -> "denied"
                    read == 1 || write == 1 -> "ask"
                    else -> "allowed"
                },
            )
            putString(
                "jump_category",
                when (
                    android.app.ActivityTaskManager.getService()
                        .getAppJumpPermissionMode(name, UserHandle.myUserId())
                ) {
                    android.app.ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW -> "allowed"
                    android.app.ActivityTaskManager.APP_JUMP_SOURCE_MODE_ASK -> "ask"
                    else -> "denied"
                },
            )
            putString(
                "sensor_category",
                if (UwuAppPolicyManager.policy(state, name, UwuAppPolicyManager.SENSOR) == 0)
                    "allowed"
                else "denied",
            )
            putString(
                "clipboard",
                if (ClipboardPolicySecureSettings.isPromptEnabled(context)) clipboard
                else context.getString(R.string.app_policy_prompt_off_summary, clipboard),
            )
            if (!clipboardEditable)
                putString("clipboard", context.getString(R.string.app_policy_system_managed))
            val jumpSummary =
                context.getString(
                    R.string.app_policy_jump_summary,
                    context.getString(
                        jumpLabel(
                            UwuAppPolicyManager.policy(state, name, UwuAppPolicyManager.JUMP_SOURCE)
                        )
                    ),
                    context.getString(
                        jumpLabel(
                            UwuAppPolicyManager.policy(state, name, UwuAppPolicyManager.JUMP_TARGET)
                        )
                    ),
                    backend.getPairRuleCount(name),
                )
            putString(
                "jump",
                if (backend.isEnabled) jumpSummary
                else context.getString(R.string.app_policy_disabled_summary, jumpSummary),
            )
            putString(
                "sensor",
                context.getString(
                    sensorLabel(UwuAppPolicyManager.policy(state, name, UwuAppPolicyManager.SENSOR))
                ),
            )
        }
    }

    fun set(context: Context, name: String, type: Int, value: Int): Boolean {
        requireApp(context, name)
        return if (type == UwuAppPolicyManager.SENSOR) {
            SensorPolicySecureSettings.setPolicy(context, name, value)
        } else {
            ClipboardPolicySecureSettings.setPolicy(context, name, type, value)
        }
    }
}
