/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.uwuaosp.appops

import android.content.Context
import android.content.Intent

object UwUAppPolicyApps {
    @JvmStatic
    fun createIntent(context: Context, feature: String): Intent {
        require(feature in AppPolicyRepository.features)
        return Intent(Intent.ACTION_MANAGE_PERMISSION_APPS)
            .setPackage(context.packageManager.permissionControllerPackageName)
            .putExtra(Intent.EXTRA_PERMISSION_GROUP_NAME, "uwu_control_$feature")
    }
}
