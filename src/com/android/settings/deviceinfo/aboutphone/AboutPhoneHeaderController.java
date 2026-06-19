/*
 * Copyright (C) 2026 The uwuAOSP Project
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

import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.os.PowerProfile;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.deviceinfo.HardwareInfoPreferenceController;
import com.android.settings.deviceinfo.StorageDashboardFragment;
import com.android.settings.deviceinfo.VersionUtils;
import com.android.settings.deviceinfo.batteryinfo.BatteryInfoFragment;
import com.android.settings.deviceinfo.firmwareversion.FirmwareVersionSettings;
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoFragment;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.LayoutPreference;

/** Binds the migrated About page header layout. */
public class AboutPhoneHeaderController extends BasePreferenceController
        implements LifecycleObserver, OnResume, OnStop {

    private static final String ABOUT_PHONE_PREFS = "about_phone_prefs";
    private static final String HEADER_ROM_TITLE = "uwuAOSP";
    private static final String KEY = "about_phone_custom_header";
    private static final String KEY_BASIC_INFO_CATEGORY = "basic_info_category";
    private static final String KEY_MORE_DEVICE_INFO = "more_device_info_pref_screen";
    private static final String KEY_BUILD_NUMBER = "build_number";
    private static final String KEY_FIRMWARE_VERSION = "firmware_version";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_DEVICE_MODEL = "device_model";
    private static final String KEY_BRANDED_ACCOUNT = "branded_account";
    private static final String PREF_USE_MD3_STYLE = "use_md3_style";

    @Nullable
    private LayoutPreference mLayoutPreference;
    @Nullable
    private MyDeviceInfoFragment mHostFragment;
    @Nullable
    private PreferenceScreen mPreferenceScreen;
    @Nullable
    private AboutPhoneBgEffectPainter mBgEffectPainter;

    public AboutPhoneHeaderController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    public static boolean isMd3StyleEnabled(@NonNull Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(ABOUT_PHONE_PREFS, Context.MODE_PRIVATE)
                .getBoolean(PREF_USE_MD3_STYLE, true);
    }

    public static void setMd3StyleEnabled(@NonNull Context context, boolean enabled) {
        context.getApplicationContext()
                .getSharedPreferences(ABOUT_PHONE_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_USE_MD3_STYLE, enabled)
                .apply();
    }

    public void setHost(@NonNull MyDeviceInfoFragment hostFragment) {
        mHostFragment = hostFragment;
    }

    public void refreshUiStyle() {
        bindLayout();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        mLayoutPreference = screen.findPreference(getPreferenceKey());
        hideDuplicatedPreferences(screen);
        bindLayout();
    }

    @Override
    public void onResume() {
        bindLayout();
    }

    @Override
    public void onStop() {
        stopAnimatedBackground();
    }

    private void hideDuplicatedPreferences(@NonNull PreferenceScreen screen) {
        setPreferenceVisibility(screen, KEY_BASIC_INFO_CATEGORY, false);
        setPreferenceVisibility(screen, KEY_FIRMWARE_VERSION, false);
        setPreferenceVisibility(screen, KEY_DEVICE_NAME, false);
        setPreferenceVisibility(screen, KEY_DEVICE_MODEL, false);
        setPreferenceVisibility(screen, KEY_BRANDED_ACCOUNT, false);
    }

    private void setPreferenceVisibility(@NonNull PreferenceScreen screen, @NonNull String key,
            boolean visible) {
        final Preference preference = screen.findPreference(key);
        if (preference != null) {
            preference.setVisible(visible);
        }
    }

    private void bindLayout() {
        if (mLayoutPreference == null) {
            return;
        }

        final boolean useMd3Style = isMd3StyleEnabled(mContext);
        final View headerCard = mLayoutPreference.findViewById(R.id.headerCard);
        if (headerCard == null) {
            stopAnimatedBackground();
            return;
        }
        final View deviceNameCard = mLayoutPreference.findViewById(R.id.deviceNameCard);
        final View storageCard = mLayoutPreference.findViewById(R.id.storageCard);
        final View versionPanel = mLayoutPreference.findViewById(R.id.versionPanel);
        final View detailsPanel = mLayoutPreference.findViewById(R.id.detailsPanel);
        final View buildNumberRow = mLayoutPreference.findViewById(R.id.buildNumberRow);

        applyCardStyle(headerCard, useMd3Style,
                R.drawable.about_phone_bg_header, R.drawable.about_phone_bg_header_md3);
        applyCardStyle(deviceNameCard, useMd3Style,
                R.drawable.about_phone_bg_card, R.drawable.about_phone_bg_card_md3);
        applyCardStyle(storageCard, useMd3Style,
                R.drawable.about_phone_bg_card, R.drawable.about_phone_bg_card_md3);
        applyCardStyle(versionPanel, useMd3Style,
                R.drawable.about_phone_bg_panel, R.drawable.about_phone_bg_panel_md3);
        applyCardStyle(detailsPanel, useMd3Style,
                R.drawable.about_phone_bg_panel, R.drawable.about_phone_bg_panel_md3);
        applyCardStyle(buildNumberRow, useMd3Style,
                R.drawable.about_phone_bg_card, R.drawable.about_phone_bg_card_md3);
        applyDetailsPanelShadowStyle(versionPanel);
        applyDetailsPanelShadowStyle(detailsPanel);

        bindAnimatedBackground();

        final TextView titleText = mLayoutPreference.findViewById(R.id.titleText);
        final TextView subtitleText = mLayoutPreference.findViewById(R.id.subtitleText);
        final ImageView headerImage = mLayoutPreference.findViewById(R.id.headerImage);
        final View headerScrim = mLayoutPreference.findViewById(R.id.headerScrim);
        final ProgressBar storageUsageBar = mLayoutPreference.findViewById(R.id.storageUsageBar);
        if (titleText == null || subtitleText == null || headerImage == null
                || headerScrim == null || storageUsageBar == null) {
            return;
        }

        applyHeaderWallpaper(headerImage);
        applyHeaderContrast(titleText, subtitleText, headerScrim);

        titleText.setText(getHeaderTitle());
        subtitleText.setText(getHeaderSubtitle());
        ((TextView) mLayoutPreference.findViewById(R.id.deviceNameValue)).setText(
                getReadableDeviceName());

        final StorageInfo storageInfo = getStorageInfo();
        ((TextView) mLayoutPreference.findViewById(R.id.storageValue)).setText(
                mContext.getString(R.string.about_phone_storage_summary,
                        storageInfo.usedFormatted, storageInfo.totalFormatted));
        storageUsageBar.setProgress(storageInfo.percentPermille);

        ((TextView) mLayoutPreference.findViewById(R.id.androidVersionValue)).setText(
                getAndroidVersionSummary());
        ((TextView) mLayoutPreference.findViewById(R.id.maintainerValue)).setText(
                getMaintainerSummary());
        ((TextView) mLayoutPreference.findViewById(R.id.buildNumberValue)).setText(
                getBuildNumberSummary());
        ((TextView) mLayoutPreference.findViewById(R.id.deviceValue)).setText(
                HardwareInfoPreferenceController.getDeviceModel());
        ((TextView) mLayoutPreference.findViewById(R.id.cpuValue)).setText(getCpuSummary());
        ((TextView) mLayoutPreference.findViewById(R.id.ramValue)).setText(getRamSummary());
        ((TextView) mLayoutPreference.findViewById(R.id.resolutionValue)).setText(
                getResolutionSummary());
        ((TextView) mLayoutPreference.findViewById(R.id.batteryValue)).setText(
                getBatteryCapacitySummary());
        ((TextView) mLayoutPreference.findViewById(R.id.kernelValue)).setText(getKernelSummary());

        mLayoutPreference.findViewById(R.id.deviceNameCard)
                .setOnClickListener(v -> showDeviceNameDialog());
        mLayoutPreference.findViewById(R.id.storageCard)
                .setOnClickListener(v -> launchStorageSettings());
        mLayoutPreference.findViewById(R.id.androidVersionCard)
                .setOnClickListener(v -> launchFirmwareSettings());
        mLayoutPreference.findViewById(R.id.buildNumberRow)
                .setOnClickListener(v -> triggerBuildNumberFlow());
        mLayoutPreference.findViewById(R.id.deviceModelRow)
                .setOnClickListener(v -> launchHardwareInfo());
        mLayoutPreference.findViewById(R.id.batteryInfoRow)
                .setOnClickListener(v -> launchBatteryInfo());
        mLayoutPreference.findViewById(R.id.moreInfoRow)
                .setOnClickListener(v -> launchMoreDeviceInfo());
    }

    private void applyCardStyle(@Nullable View cardView, boolean useMd3Style,
            int defaultBackgroundRes, int md3BackgroundRes) {
        if (cardView == null) {
            return;
        }
        cardView.setBackgroundResource(useMd3Style ? md3BackgroundRes : defaultBackgroundRes);
        cardView.setElevation(0f);
        cardView.setTranslationZ(0f);
    }

    private void applyDetailsPanelShadowStyle(@Nullable View detailsPanel) {
        if (detailsPanel == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        detailsPanel.setOutlineAmbientShadowColor(Color.TRANSPARENT);
        detailsPanel.setOutlineSpotShadowColor(Color.TRANSPARENT);
    }

    private void bindAnimatedBackground() {
        if (mLayoutPreference == null) {
            return;
        }

        final View shaderBackground =
                mLayoutPreference.findViewById(R.id.aboutPhoneShaderBackground);
        if (shaderBackground == null) {
            stopAnimatedBackground();
            return;
        }

        if (isMd3StyleEnabled(mContext)) {
            stopAnimatedBackground();
            shaderBackground.setVisibility(View.GONE);
            shaderBackground.setBackground(null);
            return;
        }

        shaderBackground.setVisibility(View.VISIBLE);
        if (mBgEffectPainter == null) {
            mBgEffectPainter = new AboutPhoneBgEffectPainter(mContext.getApplicationContext());
        }

        mBgEffectPainter.attach(shaderBackground);
        mBgEffectPainter.start();
    }

    private void stopAnimatedBackground() {
        if (mBgEffectPainter != null) {
            mBgEffectPainter.stop();
        }
    }

    private void showDeviceNameDialog() {
        final MyDeviceInfoFragment host = mHostFragment;
        if (host != null) {
            final Preference preference = mPreferenceScreen == null
                    ? null : mPreferenceScreen.findPreference(KEY_DEVICE_NAME);
            if (preference != null) {
                host.onDisplayPreferenceDialog(preference);
            }
        }
    }

    private void launchStorageSettings() {
        new SubSettingLauncher(mContext)
                .setDestination(StorageDashboardFragment.class.getName())
                .setTitleRes(R.string.storage_settings)
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    private void launchFirmwareSettings() {
        new SubSettingLauncher(mContext)
                .setDestination(FirmwareVersionSettings.class.getName())
                .setTitleRes(R.string.firmware_version)
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    private void launchHardwareInfo() {
        new SubSettingLauncher(mContext)
                .setDestination(HardwareInfoFragment.class.getName())
                .setTitleRes(R.string.model_info)
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    private void launchBatteryInfo() {
        new SubSettingLauncher(mContext)
                .setDestination(BatteryInfoFragment.class.getName())
                .setTitleRes(R.string.battery_info)
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    private void launchMoreDeviceInfo() {
        final Preference preference = mPreferenceScreen == null
                ? null : mPreferenceScreen.findPreference(KEY_MORE_DEVICE_INFO);
        if (preference != null && mHostFragment != null) {
            mHostFragment.onPreferenceTreeClick(preference);
            return;
        }

        new SubSettingLauncher(mContext)
                .setDestination(MoreDeviceInfoFragment.class.getName())
                .setTitleRes(R.string.my_device_info_more_info_title)
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    private void triggerBuildNumberFlow() {
        if (mHostFragment == null || mPreferenceScreen == null) {
            return;
        }
        final Preference preference = mPreferenceScreen.findPreference(KEY_BUILD_NUMBER);
        if (preference != null) {
            mHostFragment.onPreferenceTreeClick(preference);
        }
    }

    private void applyHeaderWallpaper(@NonNull ImageView headerImage) {
        headerImage.setImageDrawable(null);
        headerImage.setRenderEffect(null);
        try {
            final Drawable wallpaper = WallpaperManager.getInstance(mContext)
                    .getDrawable(WallpaperManager.FLAG_SYSTEM);
            if (wallpaper != null) {
                headerImage.setImageDrawable(wallpaper);
            }
        } catch (SecurityException ignored) {
            // Fall back to the card background if wallpaper access is unavailable.
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            final float blurRadius = isNightMode() ? 8f : 14f;
            headerImage.setRenderEffect(
                    RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP));
        }
    }

    private void applyHeaderContrast(
            @NonNull TextView titleView,
            @NonNull TextView subtitleView,
            @NonNull View scrimView) {
        titleView.setTextColor(Color.WHITE);
        if (isNightMode()) {
            subtitleView.setTextColor(adjustAlpha(Color.WHITE, 0.92f));
            titleView.setShadowLayer(4f, 0f, 1f, adjustAlpha(Color.BLACK, 0.22f));
            subtitleView.setShadowLayer(2f, 0f, 1f, adjustAlpha(Color.BLACK, 0.12f));
            scrimView.setBackgroundColor(Color.TRANSPARENT);
            return;
        }

        subtitleView.setTextColor(adjustAlpha(Color.WHITE, 0.80f));
        titleView.setShadowLayer(12f, 0f, 4f, adjustAlpha(Color.BLACK, 0.28f));
        subtitleView.setShadowLayer(8f, 0f, 2f, adjustAlpha(Color.BLACK, 0.18f));
        scrimView.setBackgroundColor(0x44000000);
    }

    @NonNull
    private String getHeaderTitle() {
        return HEADER_ROM_TITLE;
    }

    @NonNull
    private String getHeaderSubtitle() {
        final String bluetoothDefaultName = safeTrim(
                SystemProperties.get("bluetooth.device.default_name", ""));
        if (!bluetoothDefaultName.isEmpty()) {
            return bluetoothDefaultName;
        }

        final String usbProduct = safeTrim(SystemProperties.get("vendor.usb.product_string", ""));
        if (!usbProduct.isEmpty()) {
            return usbProduct;
        }

        final String deviceLabel = getPreferredDeviceLabel();
        return deviceLabel.isEmpty() ? mContext.getString(R.string.unknown) : deviceLabel;
    }

    @NonNull
    private String getReadableDeviceName() {
        final String customName = Settings.Global.getString(
                mContext.getContentResolver(), Settings.Global.DEVICE_NAME);
        if (!TextUtils.isEmpty(customName)) {
            return customName;
        }
        final String deviceLabel = getPreferredDeviceLabel();
        return deviceLabel.isEmpty() ? mContext.getString(R.string.unknown) : deviceLabel;
    }

    @NonNull
    private String getAndroidVersionSummary() {
        final String release = safeTrim(Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY);
        return release.isEmpty() ? mContext.getString(R.string.unknown) : release;
    }

    @NonNull
    private String getBuildNumberSummary() {
        final String buildId = safeTrim(Build.DISPLAY);
        return buildId.isEmpty() ? mContext.getString(R.string.unknown) : buildId;
    }

    @NonNull
    private String getMaintainerSummary() {
        final String maintainer = safeTrim(SystemProperties.get("org.uwuaosp.version", ""));
        if (!maintainer.isEmpty()) {
            return maintainer;
        }
        final String customVersion = safeTrim(VersionUtils.getCustomVersion());
        if (!customVersion.isEmpty()) {
            return customVersion;
        }
        final String lineageVersion = safeTrim(SystemProperties.get("ro.lineage.version", ""));
        if (!lineageVersion.isEmpty()) {
            return lineageVersion;
        }
        return mContext.getString(R.string.unknown);
    }

    @NonNull
    private String getCpuSummary() {
        final String soc = safeTrim(Build.SOC_MODEL);
        final String primaryAbi = Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                ? safeTrim(Build.SUPPORTED_ABIS[0]) : "";
        if (!soc.isEmpty() && !primaryAbi.isEmpty()) {
            return soc + "\n" + primaryAbi;
        }
        if (!soc.isEmpty()) {
            return soc;
        }
        if (!primaryAbi.isEmpty()) {
            return primaryAbi;
        }
        return mContext.getString(R.string.unknown);
    }

    @NonNull
    private String getRamSummary() {
        final ActivityManager activityManager = mContext.getSystemService(ActivityManager.class);
        if (activityManager == null) {
            return mContext.getString(R.string.unknown);
        }
        final ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return Formatter.formatShortFileSize(mContext, memoryInfo.totalMem);
    }

    @NonNull
    private String getResolutionSummary() {
        final int width = mContext.getResources().getDisplayMetrics().widthPixels;
        final int height = mContext.getResources().getDisplayMetrics().heightPixels;
        try {
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            if (wm != null) {
                wm.getDefaultDisplay().getRealMetrics(metrics);
                return mContext.getString(R.string.about_phone_resolution_value, metrics.widthPixels,
                        metrics.heightPixels);
            }

        } catch (ClassCastException ignored) {
        }
        return mContext.getString(R.string.unknown);
    }

    @NonNull
    private String getBatteryCapacitySummary() {
        final Integer designCapacity = readBatteryCapacity();
        if (designCapacity != null && designCapacity > 0) {
            return mContext.getString(R.string.about_phone_battery_capacity_value, designCapacity);
        }
        return mContext.getString(R.string.unknown);
    }

    @Nullable
    private Integer readBatteryCapacity() {
        try {
            final Resources systemResources = Resources.getSystem();
            final int resId = systemResources.getIdentifier(
                    "config_batteryCapacity", "integer", "android");
            if (resId != 0) {
                final int capacity = systemResources.getInteger(resId);
                if (capacity > 0) {
                    return capacity;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            final PowerProfile powerProfile = new PowerProfile(mContext);
            final double capacity = powerProfile.getBatteryCapacity();
            if (capacity > 0) {
                return (int) Math.round(capacity);
            }
        } catch (Exception ignored) {
        }

        final BatteryManager batteryManager = mContext.getSystemService(BatteryManager.class);
        if (batteryManager == null) {
            return null;
        }
        final int microAmpHours = batteryManager.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        final int level = getBatteryLevel();
        if (microAmpHours > 0 && level > 0) {
            return Math.round((microAmpHours / 1000f) / (level / 100f));
        }
        return null;
    }

    private int getBatteryLevel() {
        final Intent intent = mContext.registerReceiver(
                null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) {
            return -1;
        }
        final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        final int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) {
            return -1;
        }
        return Math.round((level * 100f) / scale);
    }

    @NonNull
    private String getKernelSummary() {
        final String kernelVersion = safeTrim(DeviceInfoUtils.getFormattedKernelVersion(mContext));
        if (!kernelVersion.isEmpty()) {
            return kernelVersion;
        }
        return mContext.getString(R.string.my_device_info_more_info_title);
    }

    @NonNull
    private StorageInfo getStorageInfo() {
        final StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        final long total = statFs.getTotalBytes();
        final long used = total - statFs.getAvailableBytes();
        final int percentPermille = total > 0L
                ? (int) Math.min(1000L, Math.round((used * 1000d) / total)) : 0;
        return new StorageInfo(
                Formatter.formatShortFileSize(mContext, used),
                Formatter.formatShortFileSize(mContext, total),
                percentPermille);
    }

    private int adjustAlpha(int color, float factor) {
        return Color.argb(
                Math.round(Color.alpha(color) * factor),
                Color.red(color),
                Color.green(color),
                Color.blue(color));
    }

    private boolean isNightMode() {
        return (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    @NonNull
    private String getPreferredDeviceLabel() {
        final String usbProduct = safeTrim(SystemProperties.get("vendor.usb.product_string", ""));
        if (!usbProduct.isEmpty()) {
            return usbProduct;
        }

        final String bluetoothDefaultName = safeTrim(
                SystemProperties.get("bluetooth.device.default_name", ""));
        if (!bluetoothDefaultName.isEmpty()) {
            return bluetoothDefaultName;
        }

        final String marketName = safeTrim(SystemProperties.get("ro.product.marketname", ""));
        if (!marketName.isEmpty()) {
            return marketName;
        }

        return safeTrim(Build.MODEL);
    }

    @NonNull
    private String safeTrim(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private static final class StorageInfo {
        @NonNull
        final String usedFormatted;
        @NonNull
        final String totalFormatted;
        final int percentPermille;

        StorageInfo(@NonNull String usedFormatted, @NonNull String totalFormatted,
                int percentPermille) {
            this.usedFormatted = usedFormatted;
            this.totalFormatted = totalFormatted;
            this.percentPermille = percentPermille;
        }
    }
}
