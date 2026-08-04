/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

/**
 * No-display trampoline used by metadata/Catalyst callers which cannot handle launch failures.
 */
public class AboutPhoneRouteActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AboutPhoneRouter.launchExternal(this)) {
            AboutPhoneRouter.launchNative(this);
        }
        finish();
    }
}
