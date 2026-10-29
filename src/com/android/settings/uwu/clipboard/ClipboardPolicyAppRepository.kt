/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.clipboard

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.UserHandle
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator
import java.util.Comparator

internal data class ClipboardPolicyAppEntry(
    val label: String,
    val packageName: String,
    val icon: Bitmap,
    val readPolicy: Int,
    val writePolicy: Int,
)

internal class ClipboardPolicyAppRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val iconSizePx =
        (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

    fun loadApps(): List<ClipboardPolicyAppEntry> {
        val readPolicies =
            ClipboardPolicySecureSettings.getPolicies(
                context,
                ClipboardPolicySecureSettings.OPERATION_READ,
            )
        val writePolicies =
            ClipboardPolicySecureSettings.getPolicies(
                context,
                ClipboardPolicySecureSettings.OPERATION_WRITE,
            )
        val promptsEnabled = ClipboardPolicySecureSettings.isPromptEnabled(context)
        val userId = UserHandle.myUserId()
        val launchablePackages =
            packageManager
                .queryIntentActivitiesAsUser(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                    0,
                    userId,
                )
                .mapNotNullTo(mutableSetOf<String>()) { it.activityInfo?.packageName }
        return packageManager
            .getInstalledApplicationsAsUser(PackageManager.MATCH_DISABLED_COMPONENTS, userId)
            .asSequence()
            .filter { it.packageName in launchablePackages }
            .map { info ->
                val packageName = info.packageName
                val defaultPolicy =
                    ClipboardPolicySecureSettings.getDefaultPolicy(promptsEnabled, info)
                val icon =
                    runCatching {
                            packageManager
                                .getUserBadgedIcon(
                                    info.loadIcon(packageManager),
                                    UserHandle.of(userId),
                                )
                                .toBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888)
                        }
                        .getOrElse {
                            packageManager.defaultActivityIcon.toBitmap(
                                iconSizePx,
                                iconSizePx,
                                Bitmap.Config.ARGB_8888,
                            )
                        }
                ClipboardPolicyAppEntry(
                    info.loadLabel(packageManager).toString().ifBlank { packageName },
                    packageName,
                    icon,
                    readPolicies[packageName] ?: defaultPolicy,
                    writePolicies[packageName] ?: defaultPolicy,
                )
            }
            .toList()
            .sortedWith(
                Comparator { first, second ->
                    val labelOrder = Collator.getInstance().compare(first.label, second.label)
                    if (labelOrder != 0) labelOrder
                    else first.packageName.compareTo(second.packageName)
                }
            )
    }
}
