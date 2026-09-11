/*
 * Copyright (C) 2026 The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.spa.about

import android.app.ActivityManager
import android.app.WallpaperManager
import android.app.settings.SettingsEnums
import android.app.usage.StorageStatsManager
import android.app.Activity
import android.app.AlertDialog as FrameworkAlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.text.InputType
import android.widget.EditText
import android.content.res.Configuration
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.BatteryManager
import android.os.Build
import android.os.SystemProperties
import android.os.storage.StorageManager
import android.os.storage.VolumeInfo
import android.provider.Settings
import android.text.format.Formatter
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.internal.os.PowerProfile
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.deviceinfo.BuildNumberPreferenceController
import com.android.settings.deviceinfo.HardwareInfoPreferenceController
import com.android.settings.deviceinfo.StorageDashboardFragment
import com.android.settings.deviceinfo.aboutphone.MoreDeviceInfoFragment
import com.android.settings.deviceinfo.aboutphone.updateDeviceName
import com.android.settings.deviceinfo.batteryinfo.BatteryInfoFragment
import com.android.settings.deviceinfo.firmwareversion.FirmwareVersionSettings
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoFragment
import com.android.settings.wifi.tether.WifiDeviceNameTextValidator
import com.android.settingslib.DeviceInfoUtils
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ─── data ───────────────────────────────────────────────────────────────────

private data class PageStorageInfo(
    val used: Long = 0,
    val total: Long = 0,
    val loaded: Boolean = false,
)

// ─── entry point ────────────────────────────────────────────────────────────

@Composable
fun AboutPhoneHeaderPage() {
    val context = LocalContext.current
    var storage by remember { mutableStateOf(PageStorageInfo()) }

    LaunchedEffect(Unit) {
        storage = withContext(Dispatchers.IO) { readStorageInfo(context) }
            ?: PageStorageInfo(loaded = true)
    }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 840.dp)
                .padding(horizontal = 18.dp),
        ) {
            HeaderCard(context)
            Spacer(Modifier.height(12.dp))
            TopCards(context, storage)
            Spacer(Modifier.height(12.dp))
            VersionStrip(context)
            Spacer(Modifier.height(12.dp))
            DetailsGrid(context)
            Spacer(Modifier.height(12.dp))
            BuildNumberRow(context)
            Spacer(Modifier.height(8.dp))
            MoreInfoRow(context)
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── header card ────────────────────────────────────────────────────────────

@Composable
private fun HeaderCard(context: Context) {
    val blurRadius = with(LocalDensity.current) {
        (if (isNightMode(context)) 8.dp else 14.dp).toPx()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        AndroidView(
            factory = { viewContext ->
                ImageView(viewContext).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageDrawable(
                        runCatching {
                            WallpaperManager.getInstance(viewContext)
                                .getDrawable(WallpaperManager.FLAG_SYSTEM)
                        }.getOrNull()
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setRenderEffect(
                            RenderEffect.createBlurEffect(
                                blurRadius, blurRadius, Shader.TileMode.CLAMP)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))

        val subtitleColor =
            if (isNightMode(context)) Color.White.copy(alpha = 0.92f)
            else Color.White.copy(alpha = 0.80f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.about_phone_header_title),
                contentDescription = null,
                modifier = Modifier
                    .height(34.dp)
                    .widthIn(max = 220.dp),
                alignment = Alignment.CenterStart,
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = headerSubtitle(context),
                color = subtitleColor,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── top cards (device name + storage) ──────────────────────────────────────

@Composable
private fun TopCards(context: Context, storage: PageStorageInfo) {
    val showDeviceName = context.resources.getBoolean(R.bool.config_show_device_name)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (!showDeviceName) {
            StorageCard(context, storage, Modifier.fillMaxWidth())
        } else if (maxWidth < 320.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DeviceNameCard(context, Modifier.fillMaxWidth())
                StorageCard(context, storage, Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeviceNameCard(context, Modifier.weight(1f))
                StorageCard(context, storage, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DeviceNameCard(context: Context, modifier: Modifier) {
    var deviceName by remember { mutableStateOf(readableDeviceName(context)) }
    Card(
        icon = R.drawable.logo_devices,
        label = stringResource(R.string.my_device_info_device_name_preference_title),
        value = deviceName,
        onClick = {
            showDeviceNameEditDialog(context, deviceName) { newName ->
                showDeviceNameWarningDialog(context) {
                    context.updateDeviceName(newName)
                    deviceName = newName
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun StorageCard(context: Context, storage: PageStorageInfo, modifier: Modifier) {
    Card(
        icon = R.drawable.logo_storage,
        label = stringResource(R.string.storage_settings),
        onClick = { launchStorageSettings(context) },
        modifier = modifier,
    ) {
        if (storage.loaded && storage.total > 0) {
            LinearProgressIndicator(
                progress = { ((storage.used.toDouble() * 1000) / storage.total).roundToInt() / 1000f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer,
            )
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = if (storage.loaded && storage.total > 0) {
                stringResource(
                    R.string.about_phone_storage_summary,
                    Formatter.formatShortFileSize(context, storage.used),
                    Formatter.formatShortFileSize(context, storage.total),
                )
            } else if (!storage.loaded) {
                "\u2026"
            } else {
                stringResource(R.string.about_phone_unknown)
            },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── version strip ──────────────────────────────────────────────────────────

@Composable
private fun VersionStrip(context: Context) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceBright),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            StripItem(
                icon = R.drawable.ic_android_vd_theme_24,
                label = stringResource(R.string.firmware_version),
                value = nonEmpty(Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY, context),
                onClick = { launchFirmwareSettings(context) },
            )
            StripItem(
                icon = R.drawable.logo_rom,
                label = stringResource(R.string.about_phone_maintainer_label),
                value = romVersion(context),
            )
        }
    }
}

@Composable
private fun StripItem(
    icon: Int,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .semantics(mergeDescendants = true) {}
                        .clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(34.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                MaterialTheme.colorScheme.onSurfaceVariant
            ),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── details grid ───────────────────────────────────────────────────────────

@Composable
private fun DetailsGrid(context: Context) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceBright),
    ) {
        BoxWithConstraints(Modifier.padding(8.dp)) {
            if (maxWidth < 320.dp) {
                Column {
                    ModelCell(context, Modifier.fillMaxWidth())
                    CpuCell(context, Modifier.fillMaxWidth())
                    RamCell(context, Modifier.fillMaxWidth())
                    ResolutionCell(context, Modifier.fillMaxWidth())
                    BatteryCell(context, Modifier.fillMaxWidth())
                    KernelCell(context, Modifier.fillMaxWidth())
                }
            } else {
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        ModelCell(context, Modifier.weight(1f))
                        CpuCell(context, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth()) {
                        RamCell(context, Modifier.weight(1f))
                        ResolutionCell(context, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth()) {
                        BatteryCell(context, Modifier.weight(1f))
                        KernelCell(context, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelCell(context: Context, modifier: Modifier) = GridCell(
    icon = R.drawable.logo_devices,
    label = stringResource(R.string.model_info),
    value = nonEmpty(HardwareInfoPreferenceController.getDeviceModel(), context),
    modifier = modifier,
    onClick = { launchHardwareInfo(context) },
)

@Composable
private fun CpuCell(context: Context, modifier: Modifier) = GridCell(
    icon = R.drawable.logo_cpu,
    label = stringResource(R.string.about_phone_chipset_label),
    value = cpuSummary(context),
    modifier = modifier,
)

@Composable
private fun RamCell(context: Context, modifier: Modifier) = GridCell(
    icon = R.drawable.logo_ram,
    label = stringResource(R.string.about_phone_memory_label),
    value = ramSummary(context),
    modifier = modifier,
)

@Composable
private fun ResolutionCell(context: Context, modifier: Modifier) = GridCell(
    icon = R.drawable.logo_screen,
    label = stringResource(R.string.screen_resolution_title),
    value = resolutionSummary(context),
    modifier = modifier,
)

@Composable
private fun KernelCell(context: Context, modifier: Modifier) = GridCell(
    icon = R.drawable.logo_kernel,
    label = stringResource(R.string.kernel_version),
    value = kernelSummary(context),
    modifier = modifier,
)

@Composable
private fun GridCell(
    icon: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier
                        .semantics(mergeDescendants = true) {}
                        .clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(12.dp),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                MaterialTheme.colorScheme.onSurfaceVariant
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BatteryCell(context: Context, modifier: Modifier) {
    val capacity = remember { readBatteryCapacity(context) }
    if (capacity != null && capacity > 0) {
        GridCell(
            icon = R.drawable.logo_battery,
            label = stringResource(R.string.about_phone_battery_capacity_label),
            value = stringResource(R.string.about_phone_battery_capacity_value, capacity),
            onClick = { launchBatteryInfo(context) },
            modifier = modifier,
        )
    } else {
        GridCell(
            icon = R.drawable.logo_battery,
            label = stringResource(R.string.about_phone_battery_capacity_label),
            value = stringResource(R.string.about_phone_unknown),
            onClick = { launchBatteryInfo(context) },
            modifier = modifier,
        )
    }
}

// ─── build number row ───────────────────────────────────────────────────────

@Composable
private fun BuildNumberRow(context: Context) {
    val activity = context.findActivity() ?: return
    var showBiometricLockout by remember { mutableStateOf(false) }
    val controller = remember(context) {
        BuildNumberPreferenceController(context, BUILD_NUMBER_KEY)
    }
    val resultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        controller.onActivityResult(result.resultCode, result.data)
    }
    DisposableEffect(controller, activity, resultLauncher) {
        controller.setHost(
            activity,
            resultLauncher,
            SettingsEnums.DEVICEINFO,
            { showBiometricLockout = true },
        )
        controller.onStart()
        onDispose { }
    }
    val preference = remember(context) {
        Preference(context).apply { key = BUILD_NUMBER_KEY }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .semantics(mergeDescendants = true) {}
            .clickable { controller.handlePreferenceTreeClick(preference) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_build),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                    MaterialTheme.colorScheme.onSurfaceVariant
                ),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.build_number),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = nonEmpty(Build.DISPLAY, context),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    if (showBiometricLockout) {
        AlertDialog(
            onDismissRequest = { showBiometricLockout = false },
            title = { Text(stringResource(R.string.identity_check_lockout_error_title)) },
            text = {
                Text(stringResource(
                    R.string.identity_check_lockout_error_two_factor_auth_description_1))
            },
            confirmButton = {
                TextButton(onClick = { showBiometricLockout = false }) {
                    Text(stringResource(R.string.okay))
                }
            },
        )
    }
}

// ─── more info row ──────────────────────────────────────────────────────────

@Composable
private fun MoreInfoRow(context: Context) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .semantics(mergeDescendants = true) {}
            .height(48.dp)
            .clickable { launchMoreDeviceInfo(context) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.about_phone_more_info),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── shared card composable ─────────────────────────────────────────────────

@Composable
private fun Card(
    icon: Int,
    label: String,
    value: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .heightIn(min = 144.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .then(
                if (onClick != null) {
                    Modifier
                        .semantics(mergeDescendants = true) {}
                        .clickable { onClick() }
                } else {
                    Modifier
                }
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                    MaterialTheme.colorScheme.onSurfaceVariant
                ),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            content?.invoke()
            if (value != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun showDeviceNameEditDialog(
    context: Context,
    initialValue: String,
    onValidName: (String) -> Unit,
) {
    val validator = WifiDeviceNameTextValidator()
    val input = EditText(context).apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        setText(initialValue)
        selectAll()
        setSingleLine(true)
    }
    val dialog = FrameworkAlertDialog.Builder(context)
        .setTitle(R.string.my_device_info_device_name_preference_title)
        .setView(input)
        .setPositiveButton(android.R.string.ok, null)
        .setNegativeButton(android.R.string.cancel, null)
        .create()
    dialog.setOnShowListener {
        dialog.getButton(FrameworkAlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val value = input.text.toString().trim()
            if (validator.isTextValid(value)) {
                dialog.dismiss()
                onValidName(value)
            } else {
                input.error = validator.getErrorMessage(value)
            }
        }
    }
    dialog.show()
}

private fun showDeviceNameWarningDialog(context: Context, onConfirm: () -> Unit) {
    FrameworkAlertDialog.Builder(context)
        .setTitle(R.string.my_device_info_device_name_preference_title)
        .setMessage(R.string.about_phone_device_name_warning)
        .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm() }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}

// ─── navigation helpers ─────────────────────────────────────────────────────

private fun launchStorageSettings(context: Context) {
    runCatching {
        SubSettingLauncher(context)
            .setDestination(StorageDashboardFragment::class.java.name)
            .setArguments(android.os.Bundle.EMPTY)
            .setTitleRes(R.string.storage_settings)
            .setSourceMetricsCategory(SettingsEnums.DEVICEINFO)
            .launch()
    }
        .onFailure { showToast(context, R.string.about_phone_unavailable) }
}

private fun launchFirmwareSettings(context: Context) {
    SubSettingLauncher(context)
        .setDestination(FirmwareVersionSettings::class.java.name)
        .setTitleRes(R.string.firmware_version)
        .setSourceMetricsCategory(SettingsEnums.DEVICEINFO)
        .launch()
}

private fun launchHardwareInfo(context: Context) {
    SubSettingLauncher(context)
        .setDestination(HardwareInfoFragment::class.java.name)
        .setTitleRes(R.string.model_info)
        .setSourceMetricsCategory(SettingsEnums.DEVICEINFO)
        .launch()
}

private fun launchBatteryInfo(context: Context) {
    SubSettingLauncher(context)
        .setDestination(BatteryInfoFragment::class.java.name)
        .setTitleRes(R.string.battery_info)
        .setSourceMetricsCategory(SettingsEnums.DEVICEINFO)
        .launch()
}

private fun launchMoreDeviceInfo(context: Context) {
    SubSettingLauncher(context)
        .setDestination(MoreDeviceInfoFragment::class.java.name)
        .setTitleRes(R.string.about_phone_more_info)
        .setSourceMetricsCategory(SettingsEnums.DEVICEINFO)
        .launch()
}

// ─── system info readers ────────────────────────────────────────────────────

private fun readStorageInfo(context: Context): PageStorageInfo? =
    runCatching {
        val storageManager = context.getSystemService(StorageManager::class.java)
            ?: return@runCatching null
        val stats = context.getSystemService(StorageStatsManager::class.java)
            ?: return@runCatching null
        val volume = storageManager.findVolumeById(VolumeInfo.ID_PRIVATE_INTERNAL)
            ?: return@runCatching null
        val provider = StorageManagerVolumeProvider(storageManager)
        val total = provider.getTotalBytes(stats, volume)
        val free = provider.getFreeBytes(stats, volume)
        PageStorageInfo(used = (total - free).coerceIn(0, total), total = total, loaded = true)
    }.getOrNull()

private fun readableDeviceName(context: Context): String {
    val custom = Settings.Global.getString(
        context.contentResolver, Settings.Global.DEVICE_NAME,
    ).orEmpty().trim()
    return custom.ifEmpty { preferredDeviceLabel(context) }
}

private fun headerSubtitle(context: Context): String =
    firstSystemProperty(
        "bluetooth.device.default_name",
        "vendor.usb.product_string",
        "ro.product.marketname",
    ).ifEmpty { preferredDeviceLabel(context) }

private fun preferredDeviceLabel(context: Context): String =
    firstSystemProperty(
        "vendor.usb.product_string",
        "bluetooth.device.default_name",
        "ro.product.marketname",
    ).ifEmpty { nonEmpty(Build.MODEL, context) }

private fun romVersion(context: Context): String =
    firstSystemProperty(
        "org.uwuaosp.version",
        "ro.uwu.version",
        "ro.uwuaosp.version",
        "ro.uwu.build.version",
        "ro.lineage.version",
    ).ifEmpty { nonEmpty(Build.DISPLAY, context) }

private fun cpuSummary(context: Context): String {
    val soc = Build.SOC_MODEL.orEmpty().trim()
        .ifEmpty { SystemProperties.get("ro.soc.model", "").trim() }
    val abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty().trim()
    return listOf(soc, abi).filter(String::isNotEmpty).joinToString("\n")
        .ifEmpty { stringResource(context, R.string.about_phone_unknown) }
}

private fun ramSummary(context: Context): String {
    val manager = context.getSystemService(ActivityManager::class.java)
        ?: return stringResource(context, R.string.about_phone_unknown)
    val memory = ActivityManager.MemoryInfo()
    manager.getMemoryInfo(memory)
    return if (memory.totalMem > 0) {
        Formatter.formatShortFileSize(context, memory.totalMem)
    } else {
        stringResource(context, R.string.about_phone_unknown)
    }
}

private fun resolutionSummary(context: Context): String {
    val mode = context.display?.mode
    val width = mode?.physicalWidth ?: context.resources.displayMetrics.widthPixels
    val height = mode?.physicalHeight ?: context.resources.displayMetrics.heightPixels
    return stringResource(context, R.string.about_phone_resolution_value, width, height)
}

private fun readBatteryCapacity(context: Context): Int? {
    runCatching {
        val resources = android.content.res.Resources.getSystem()
        val id = resources.getIdentifier("config_batteryCapacity", "integer", "android")
        if (id != 0) resources.getInteger(id)
        else 0
    }.getOrDefault(0).takeIf { it > 0 }?.let { return it }

    runCatching { PowerProfile(context).batteryCapacity.roundToInt() }
        .getOrDefault(0).takeIf { it > 0 }?.let { return it }

    val batteryManager = context.getSystemService(BatteryManager::class.java) ?: return null
    val chargeMicroAmpHours = batteryManager.getIntProperty(
        BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
    val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    return if (chargeMicroAmpHours > 0 && level > 0) {
        ((chargeMicroAmpHours / 1000f) / (level / 100f)).roundToInt()
    } else {
        null
    }
}

private fun kernelSummary(context: Context): String =
    runCatching { DeviceInfoUtils.getFormattedKernelVersion(context).orEmpty().trim() }
        .getOrDefault("")
        .ifEmpty { System.getProperty("os.version").orEmpty().trim() }
        .ifEmpty { stringResource(context, R.string.about_phone_unknown) }

// ─── utilities ──────────────────────────────────────────────────────────────

private fun firstSystemProperty(vararg names: String): String =
    names.asSequence()
        .map { SystemProperties.get(it, "").trim() }
        .firstOrNull(String::isNotEmpty)
        .orEmpty()

private fun nonEmpty(value: String?, context: Context): String =
    value.orEmpty().trim().ifEmpty { stringResource(context, R.string.about_phone_unknown) }

private fun isNightMode(context: Context): Boolean =
    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES

private fun showToast(context: Context, stringRes: Int) {
    Toast.makeText(context, stringRes, Toast.LENGTH_SHORT).show()
}

private fun stringResource(context: Context, resId: Int, vararg formatArgs: Any): String =
    context.resources.getString(resId, *formatArgs)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val BUILD_NUMBER_KEY = "build_number"
