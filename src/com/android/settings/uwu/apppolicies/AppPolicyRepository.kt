/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.uwu.apppolicies

import android.app.UwuAppPolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.UserHandle
import androidx.core.graphics.drawable.toBitmap
import com.android.settings.R
import com.android.settings.uwu.appjump.AppJumpPolicyBackend
import com.android.settings.uwu.clipboard.ClipboardPolicySecureSettings
import com.android.settings.uwu.sensors.SensorPolicySecureSettings
import java.text.Collator
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

internal data class AppPolicyAppEntry(
    val packageName: String,
    val label: String,
    val icon: Bitmap,
    val category: String,
)

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

    private fun category(
        context: Context,
        app: ApplicationInfo,
        state: Bundle,
        feature: String,
    ): String =
        when (feature) {
            "clipboard" -> {
                val read = clipboardPolicy(context, app, state, UwuAppPolicyManager.CLIPBOARD_READ)
                val write =
                    clipboardPolicy(context, app, state, UwuAppPolicyManager.CLIPBOARD_WRITE)
                when {
                    !clipboardEditable(context, app) -> "allowed"
                    read == 2 || write == 2 -> "denied"
                    read == 1 || write == 1 -> "ask"
                    else -> "allowed"
                }
            }
            "jump" ->
                when (
                    android.app.ActivityTaskManager.getService()
                        .getAppJumpPermissionMode(app.packageName, UserHandle.myUserId())
                ) {
                    android.app.ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW -> "allowed"
                    android.app.ActivityTaskManager.APP_JUMP_SOURCE_MODE_ASK -> "ask"
                    else -> "denied"
                }
            "sensor" ->
                if (
                    UwuAppPolicyManager.policy(
                        state,
                        app.packageName,
                        UwuAppPolicyManager.SENSOR,
                    ) == 0
                )
                    "allowed"
                else "denied"
            else -> error("Unknown app policy feature")
        }

    suspend fun loadApps(context: Context, feature: String): List<AppPolicyAppEntry> {
        require(feature in features)
        val state = snapshot()
        val manager = context.packageManager
        val size = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val user = UserHandle.of(UserHandle.myUserId())
        val apps = mutableListOf<AppPolicyAppEntry>()
        for (info in manager.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)) {
            coroutineContext.ensureActive()
            if (!UserHandle.isApp(info.uid) || UserHandle.getUserId(info.uid) != user.identifier) {
                continue
            }
            val icon =
                runCatching {
                        manager.getUserBadgedIcon(info.loadIcon(manager), user).toBitmap(size, size)
                    }
                    .getOrElse {
                        manager
                            .getUserBadgedIcon(manager.defaultActivityIcon, user)
                            .toBitmap(size, size)
                    }
            apps +=
                AppPolicyAppEntry(
                    info.packageName,
                    info.loadLabel(manager).toString().ifBlank { info.packageName },
                    icon,
                    category(context, info, state, feature),
                )
        }
        val collator = Collator.getInstance()
        return apps.sortedWith { first, second ->
            val order = collator.compare(first.label, second.label)
            if (order != 0) order else first.packageName.compareTo(second.packageName)
        }
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
            for (feature in features) {
                putString(feature + "_category", category(context, app, state, feature))
            }
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
