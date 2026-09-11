/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.settings.deviceinfo.aboutphone;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.deviceinfo.DeviceNamePreferenceController;
import com.android.settings.spa.SpaActivity;

public class TopLevelAboutDevicePreferenceController extends BasePreferenceController {
    public TopLevelAboutDevicePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final DeviceNamePreferenceController deviceNamePreferenceController =
                new DeviceNamePreferenceController(mContext, "unused_key");
        return deviceNamePreferenceController.getSummary();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }
        SpaActivity.startSpaActivity(mContext, "AboutPhone");
        return true;
    }

}
