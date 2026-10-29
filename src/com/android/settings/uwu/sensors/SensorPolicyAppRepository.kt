/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.sensors

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.UserHandle
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator
import java.util.Comparator

internal data class SensorPolicyAppEntry(
    val label: String,
    val packageName: String,
    val icon: Bitmap,
    val policy: Int,
)

internal class SensorPolicyAppRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val iconSizePx =
        (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

    fun loadApps(): List<SensorPolicyAppEntry> {
        val policies = SensorPolicySecureSettings.getPolicies(context)
        val userId = UserHandle.myUserId()
        return packageManager
            .getInstalledApplicationsAsUser(PackageManager.MATCH_DISABLED_COMPONENTS, userId)
            .asSequence()
            .filter { UserHandle.isApp(it.uid) }
            .filter { (it.flags and ApplicationInfo.FLAG_PERSISTENT) == 0 }
            .map { info ->
                val packageName = info.packageName
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
                SensorPolicyAppEntry(
                    info.loadLabel(packageManager).toString().ifBlank { packageName },
                    packageName,
                    icon,
                    policies[packageName] ?: SensorPolicySecureSettings.POLICY_ALLOW,
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
