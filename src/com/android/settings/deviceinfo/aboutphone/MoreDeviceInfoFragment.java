/*
 * Copyright (C) 2026 The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.deviceinfo.aboutphone;

import static androidx.core.content.ContextCompat.getMainExecutor;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.BluetoothAddressPreferenceController;
import com.android.settings.deviceinfo.FccEquipmentIdPreferenceController;
import com.android.settings.deviceinfo.FeedbackPreferenceController;
import com.android.settings.deviceinfo.IpAddressPreferenceController;
import com.android.settings.deviceinfo.ManualPreferenceController;
import com.android.settings.deviceinfo.RegulatoryInfoPreferenceController;
import com.android.settings.deviceinfo.SafetyInfoPreferenceController;
import com.android.settings.deviceinfo.UptimePreferenceController;
import com.android.settings.deviceinfo.WifiMacAddressPreferenceController;
import com.android.settings.deviceinfo.imei.ImeiInfoPreferenceController;
import com.android.settings.deviceinfo.simstatus.EidStatus;
import com.android.settings.deviceinfo.simstatus.SimEidPreferenceController;
import com.android.settings.deviceinfo.simstatus.SimStatusPreferenceController;
import com.android.settings.deviceinfo.simstatus.SlotSimStatus;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@SearchIndexable
public class MoreDeviceInfoFragment extends DashboardFragment {
    private static final String KEY_EID_INFO = "eid_info";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.more_device_info;
    }

    @Override
    protected String getLogTag() {
        return "MoreDeviceInfo";
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVICEINFO;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Fragment fragment, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Executor executor = fragment == null
                ? getMainExecutor(context) : Executors.newSingleThreadExecutor();
        final SlotSimStatus slotSimStatus = new SlotSimStatus(
                context, executor, fragment == null ? null : fragment.getLifecycle());

        controllers.add(new IpAddressPreferenceController(context, lifecycle));
        controllers.add(new WifiMacAddressPreferenceController(context, lifecycle));
        controllers.add(new BluetoothAddressPreferenceController(context, lifecycle));
        controllers.add(new RegulatoryInfoPreferenceController(context));
        controllers.add(new SafetyInfoPreferenceController(context));
        controllers.add(new ManualPreferenceController(context));
        controllers.add(new FeedbackPreferenceController(fragment, context));
        controllers.add(new FccEquipmentIdPreferenceController(context));
        controllers.add(new UptimePreferenceController(context, lifecycle));

        final Consumer<String> addImei = key -> {
            final ImeiInfoPreferenceController controller =
                    new ImeiInfoPreferenceController(context, key);
            controller.init(fragment, slotSimStatus);
            controllers.add(controller);
        };
        if (fragment != null) {
            addImei.accept(ImeiInfoPreferenceController.DEFAULT_KEY);
        }
        for (int slot = 0; slot < slotSimStatus.size(); slot++) {
            final SimStatusPreferenceController simController =
                    new SimStatusPreferenceController(context, slotSimStatus.getPreferenceKey(slot));
            simController.init(fragment, slotSimStatus);
            controllers.add(simController);
            if (fragment != null) {
                addImei.accept(ImeiInfoPreferenceController.DEFAULT_KEY + (slot + 1));
            }
        }
        if (fragment != null) {
            final SimEidPreferenceController eidController =
                    new SimEidPreferenceController(context, KEY_EID_INFO);
            eidController.init(slotSimStatus, new EidStatus(slotSimStatus, context, executor));
            controllers.add(eidController);
        }
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.more_device_info) {
                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null, null);
                }
            };
}
