/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.uwuaosp.appops.sensors

import android.os.Bundle
import org.uwuaosp.appops.clipboard.AppPolicyAppListFragment
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R as ToolbarR

class SensorPolicyActivity : CollapsingToolbarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(ToolbarR.id.content_frame, AppPolicyAppListFragment.newInstance(FEATURE))
                .commit()
        }
    }

    companion object {
        private const val FEATURE = "sensor"
    }
}
