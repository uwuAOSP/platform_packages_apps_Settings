/*
 * Copyright (C) 2026 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.deviceinfo.aboutphone;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AboutPhoneRouterTest {
    @Mock private Context mContext;
    @Mock private Activity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageName()).thenReturn("com.android.settings");
        when(mActivity.getPackageName()).thenReturn("com.android.settings");
    }

    @Test
    public void getRouteIntent_returnsTrampoline() {
        final Intent intent = AboutPhoneRouter.getRouteIntent(mContext);

        assertThat(intent.getComponent().getClassName())
                .isEqualTo(AboutPhoneRouteActivity.class.getName());
    }

    @Test
    public void launchNative_available_startsNativeAboutPhone() {
        assertThat(AboutPhoneRouter.launchNative(mActivity)).isTrue();

        verify(mActivity).startActivity(any(Intent.class));
    }

    @Test
    public void launchNative_activityNotFound_returnsFalse() {
        doThrow(new ActivityNotFoundException()).when(mActivity).startActivity(any(Intent.class));

        assertThat(AboutPhoneRouter.launchNative(mActivity)).isFalse();
    }

    @Test
    public void launchNative_securityException_returnsFalse() {
        doThrow(new SecurityException()).when(mActivity).startActivity(any(Intent.class));

        assertThat(AboutPhoneRouter.launchNative(mActivity)).isFalse();
    }

    @Test
    public void launchNative_runtimeException_returnsFalse() {
        doThrow(new RuntimeException()).when(mActivity).startActivity(any(Intent.class));

        assertThat(AboutPhoneRouter.launchNative(mActivity)).isFalse();
    }
}
