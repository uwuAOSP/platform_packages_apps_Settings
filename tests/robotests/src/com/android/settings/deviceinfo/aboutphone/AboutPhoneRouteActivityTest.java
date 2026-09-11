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

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;

import com.android.settings.spa.SpaActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AboutPhoneRouteActivityTest {
    @Test
    public void create_launchesOnlySpaAboutPhoneAndFinishes() {
        final AboutPhoneRouteActivity activity =
                Robolectric.buildActivity(AboutPhoneRouteActivity.class).create().get();

        final Intent intent = shadowOf(activity).getNextStartedActivity();
        assertThat(intent).isNotNull();
        assertThat(intent.getComponent().getClassName())
                .isEqualTo(SpaActivity.class.getName());
        assertThat(activity.isFinishing()).isTrue();
    }
}
