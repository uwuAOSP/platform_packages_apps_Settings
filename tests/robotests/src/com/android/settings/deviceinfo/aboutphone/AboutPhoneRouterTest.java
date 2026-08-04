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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AboutPhoneRouterTest {
    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;

    private ActivityInfo mActivityInfo;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.enabled = true;
        mActivityInfo = new ActivityInfo();
        mActivityInfo.applicationInfo = applicationInfo;
        mActivityInfo.enabled = true;
        mActivityInfo.exported = true;
        mActivityInfo.packageName = AboutPhoneRouter.EXTERNAL_COMPONENT.getPackageName();
        mActivityInfo.name = AboutPhoneRouter.EXTERNAL_COMPONENT.getClassName();

        when(mPackageManager.getActivityInfo(
                AboutPhoneRouter.EXTERNAL_COMPONENT,
                PackageManager.MATCH_DISABLED_COMPONENTS)).thenReturn(mActivityInfo);
        when(mPackageManager.getComponentEnabledSetting(AboutPhoneRouter.EXTERNAL_COMPONENT))
                .thenReturn(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        when(mPackageManager.getApplicationEnabledSetting(
                AboutPhoneRouter.EXTERNAL_COMPONENT.getPackageName()))
                .thenReturn(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = mActivityInfo;
        when(mPackageManager.resolveActivity(any(Intent.class), eq(0))).thenReturn(resolveInfo);
    }

    @Test
    public void getExternalIntent_available_returnsExplicitIntent() {
        final Intent intent = AboutPhoneRouter.getExternalIntent(mContext);

        assertThat(intent).isNotNull();
        assertThat(intent.getComponent()).isEqualTo(AboutPhoneRouter.EXTERNAL_COMPONENT);
    }

    @Test
    public void getExternalIntent_activityDisabled_returnsNull() {
        mActivityInfo.enabled = false;

        assertThat(AboutPhoneRouter.getExternalIntent(mContext)).isNull();
    }

    @Test
    public void getExternalIntent_componentDisabledByUser_returnsNull() {
        when(mPackageManager.getComponentEnabledSetting(AboutPhoneRouter.EXTERNAL_COMPONENT))
                .thenReturn(PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER);

        assertThat(AboutPhoneRouter.getExternalIntent(mContext)).isNull();
    }

    @Test
    public void getExternalIntent_manifestActivityDisabledButOverrideEnabled_returnsIntent() {
        mActivityInfo.enabled = false;
        when(mPackageManager.getComponentEnabledSetting(AboutPhoneRouter.EXTERNAL_COMPONENT))
                .thenReturn(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        assertThat(AboutPhoneRouter.getExternalIntent(mContext)).isNotNull();
    }

    @Test
    public void getExternalIntent_applicationDisabled_returnsNull() {
        when(mPackageManager.getApplicationEnabledSetting(
                AboutPhoneRouter.EXTERNAL_COMPONENT.getPackageName()))
                .thenReturn(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);

        assertThat(AboutPhoneRouter.getExternalIntent(mContext)).isNull();
    }

    @Test
    public void getExternalIntent_notResolved_returnsNull() {
        when(mPackageManager.resolveActivity(any(Intent.class), eq(0))).thenReturn(null);

        assertThat(AboutPhoneRouter.getExternalIntent(mContext)).isNull();
    }

    @Test
    public void getExternalIntent_callerLacksTargetPermission_returnsNull() {
        mActivityInfo.permission = "test.permission.SETTINGS_EXT";
        when(mContext.checkSelfPermission(mActivityInfo.permission))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        assertThat(AboutPhoneRouter.getExternalIntent(mContext)).isNull();
    }

    @Test
    public void launchExternal_available_startsExplicitIntent() {
        assertThat(AboutPhoneRouter.launchExternal(mContext)).isTrue();

        verify(mContext).startActivity(any(Intent.class));
    }

    @Test
    public void launchExternal_activityNotFound_returnsFalse() {
        doThrow(new ActivityNotFoundException()).when(mContext).startActivity(any(Intent.class));

        assertThat(AboutPhoneRouter.launchExternal(mContext)).isFalse();
    }

    @Test
    public void launchExternal_securityException_returnsFalse() {
        doThrow(new SecurityException()).when(mContext).startActivity(any(Intent.class));

        assertThat(AboutPhoneRouter.launchExternal(mContext)).isFalse();
    }

    @Test
    public void launchExternal_runtimeException_returnsFalse() {
        doThrow(new RuntimeException()).when(mContext).startActivity(any(Intent.class));

        assertThat(AboutPhoneRouter.launchExternal(mContext)).isFalse();
    }
}
