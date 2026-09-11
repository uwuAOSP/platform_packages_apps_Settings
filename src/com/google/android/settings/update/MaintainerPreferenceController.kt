/*
 * Copyright (C) 2026 The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.settings.update

import android.content.Context
import android.os.SystemProperties
import com.android.settings.core.BasePreferenceController

class MaintainerPreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int =
        if (getMaintainer().isEmpty()) UNSUPPORTED_ON_DEVICE else AVAILABLE

    override fun getSummary(): CharSequence = getMaintainer()

    private fun getMaintainer(): String =
        SystemProperties.get("ro.uwu.maintainer", "").trim()
}
