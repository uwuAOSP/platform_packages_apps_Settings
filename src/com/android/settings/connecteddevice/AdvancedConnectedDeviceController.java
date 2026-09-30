/*
 * Copyright 2018 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.connecteddevice;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Controls the homepage summary for More connection settings. */
// LINT.IfChange
public class AdvancedConnectedDeviceController extends BasePreferenceController {

    public AdvancedConnectedDeviceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getText(R.string.uwu_more_connection_settings_summary);
    }

}
// LINT.ThenChange(AdvancedConnectedDeviceScreen.kt,
// AdvancedConnectedDeviceApiScreen.kt)
