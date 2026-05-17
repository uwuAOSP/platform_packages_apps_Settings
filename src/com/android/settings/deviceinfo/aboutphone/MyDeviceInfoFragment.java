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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.BuildNumberPreferenceController;
import com.android.settings.deviceinfo.DeviceNamePreferenceController;
import com.android.settings.flags.Flags;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class MyDeviceInfoFragment extends DashboardFragment
        implements DeviceNamePreferenceController.DeviceNamePreferenceHost {

    private static final String LOG_TAG = "MyDeviceInfoFragment";
    private static final int MENU_INTERFACE_STYLE = Menu.FIRST;

    private BuildNumberPreferenceController mBuildNumberPreferenceController;

    private DeviceInfoViewModel mDeviceInfoViewModel;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVICEINFO;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_about;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(DeviceNamePreferenceController.class).setHost(this /* parent */);
        mBuildNumberPreferenceController = use(BuildNumberPreferenceController.class);
        mBuildNumberPreferenceController.setHost(this /* parent */);
        use(AboutPhoneHeaderController.class).setHost(this /* parent */);
    }

    @Override
    public void onCreate(@Nullable Bundle icicle) {
        super.onCreate(icicle);
        setHasOptionsMenu(true);
        mDeviceInfoViewModel = new ViewModelProvider(getActivity()).get(DeviceInfoViewModel.class);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.my_device_info;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.removeItem(MENU_INTERFACE_STYLE);
        menu.add(Menu.NONE, MENU_INTERFACE_STYLE, Menu.NONE, R.string.about_phone_interface_menu)
                .setIcon(R.drawable.ic_format_paint_vd_theme_24)
                .setShowAsActionFlags(
                        MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_INTERFACE_STYLE) {
            toggleInterfaceStyle();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return new ArrayList<>();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mBuildNumberPreferenceController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void showDeviceNameWarningDialog(String deviceName) {
        mDeviceInfoViewModel.setDeviceName(deviceName);
        DeviceNameWarningDialog.show(this);
    }

    private void toggleInterfaceStyle() {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        final boolean useMd3Style = AboutPhoneHeaderController.isMd3StyleEnabled(context);
        AboutPhoneHeaderController.setMd3StyleEnabled(context, !useMd3Style);
        use(AboutPhoneHeaderController.class).refreshUiStyle();
    }

    public void onSetDeviceNameConfirm(boolean confirm) {
        if (!isCatalystEnabled() || !Flags.catalystAboutPhoneDeviceName()) {
            final DeviceNamePreferenceController controller = use(
                    DeviceNamePreferenceController.class);
            controller.updateDeviceName(confirm);
        } else {
            if (confirm) {
                final String deviceName = mDeviceInfoViewModel.getDeviceName();
                if (deviceName != null) {
                    UtilsKt.updateDeviceName(getActivity(), deviceName);
                }
            }
        }
        mDeviceInfoViewModel.clearDeviceNme();
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return MyDeviceInfoScreen.KEY;
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.my_device_info) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return new ArrayList<>();
                }
            };
}
