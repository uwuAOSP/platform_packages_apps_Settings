/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.uwu.apppolicies

import android.app.AppOpsManager
import android.app.UwuAppPolicyManager
import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.settings.R
import com.android.settings.uwu.appjump.AppJumpSettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.uwuaosp.compose.framework.theme.SettingsTheme
import org.uwuaosp.compose.widget.app.AppListError
import org.uwuaosp.compose.widget.app.AppListLoading
import org.uwuaosp.compose.widget.preference.RadioPreferenceRow
import org.uwuaosp.compose.widget.preference.SettingsSection
import org.uwuaosp.compose.widget.scaffold.SettingsScaffold

class AppPolicyActivity : ComponentActivity() {
    private var watcher: AppOpsManager.OnOpChangedListener? = null
    private val refresh = mutableIntStateOf(0)
    private val observer =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refresh.intValue++
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)
        val feature = intent.getStringExtra(AppPolicyRepository.EXTRA_FEATURE)
        if (
            name == null ||
                feature !in AppPolicyRepository.features ||
                runCatching { AppPolicyRepository.requireApp(this, name) }.isFailure
        ) {
            Toast.makeText(this, R.string.app_policy_load_failed, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (feature == "jump") {
            startActivity(AppJumpSettingsActivity.createAppDetailIntent(this, name))
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            SettingsTheme { AppPolicyScreen(name, feature!!, refresh.intValue, ::finish) }
        }
    }

    override fun onStart() {
        super.onStart()
        watcher =
            UwuAppPolicyManager.watch(this, Handler(Looper.getMainLooper())) { user ->
                if (user == android.os.UserHandle.myUserId()) refresh.intValue++
            }
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.UWU_APP_CLIPBOARD_PROMPTS_ENABLED),
            false,
            observer,
        )
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD),
            false,
            observer,
        )
        refresh.intValue++
    }

    override fun onStop() {
        watcher?.let { getSystemService(AppOpsManager::class.java).stopWatchingMode(it) }
        watcher = null
        contentResolver.unregisterContentObserver(observer)
        super.onStop()
    }
}

@Composable
private fun AppPolicyScreen(name: String, feature: String, refresh: Int, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<Bundle?>(null) }
    var label by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    LaunchedEffect(refresh, revision) {
        runCatching {
                withContext(Dispatchers.IO) {
                    val info = AppPolicyRepository.requireApp(context, name)
                    val current = AppPolicyRepository.snapshot()
                    current.putBoolean(
                        "_clipboard_editable",
                        AppPolicyRepository.clipboardEditable(context, info),
                    )
                    if (feature == "clipboard") {
                        val values =
                            current.getIntArray(name)?.clone() ?: UwuAppPolicyManager.defaults()
                        for (type in 0..1) values[type] =
                            AppPolicyRepository.clipboardPolicy(context, info, current, type)
                        current.putIntArray(name, values)
                    }
                    Triple(
                        current,
                        info.loadLabel(context.packageManager).toString(),
                        context.packageManager
                            .getUserBadgedIcon(
                                info.loadIcon(context.packageManager),
                                android.os.UserHandle.of(android.os.UserHandle.myUserId()),
                            )
                            .toBitmap(144, 144)
                            .asImageBitmap(),
                    )
                }
            }
            .onSuccess { (current, title, image) ->
                state = current
                label = title
                icon = image
                failed = false
            }
            .onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                state = null
                failed = true
            }
    }
    val change: (Int, Int) -> Unit = { type, policy ->
        if (!busy) {
            busy = true
            scope.launch {
                val saved =
                    runCatching {
                            withContext(Dispatchers.IO) {
                                AppPolicyRepository.set(context, name, type, policy)
                            }
                        }
                        .getOrElse {
                            if (it is kotlinx.coroutines.CancellationException) throw it
                            false
                        }
                busy = false
                if (!saved)
                    Toast.makeText(context, R.string.app_policy_update_failed, Toast.LENGTH_SHORT)
                        .show()
                revision++
            }
        }
    }
    SettingsScaffold(
        title =
            stringResource(
                if (feature == "clipboard") R.string.app_policy_clipboard
                else R.string.app_policy_sensor
            ),
        showBackButton = true,
        onNavigateUp = onBack,
    ) {
        when {
            failed ->
                AppListError(
                    stringResource(R.string.app_policy_load_failed),
                    stringResource(R.string.sensor_policy_retry),
                    { revision++ },
                )
            state == null -> AppListLoading()
            else -> {
                Column(
                    Modifier.padding(vertical = 24.dp).align(Alignment.CenterHorizontally),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    icon?.let { Icon(it, null, Modifier.size(72.dp), tint = Color.Unspecified) }
                    Text(
                        label,
                        Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                if (feature == "clipboard" && !state!!.getBoolean("_clipboard_editable")) {
                    Text(
                        stringResource(R.string.app_policy_system_managed),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val types = if (feature == "clipboard") listOf(0, 1) else listOf(2)
                types.forEach { type ->
                    SettingsSection(
                        title =
                            stringResource(
                                when (type) {
                                    0 -> R.string.clipboard_policy_read
                                    1 -> R.string.clipboard_policy_write
                                    else -> R.string.sensor_policy_app_policy
                                }
                            ),
                        modifier = Modifier.selectableGroup(),
                    ) {
                        for (choice in 0..2) item {
                            RadioPreferenceRow(
                                title =
                                    stringResource(
                                        if (type == 2) AppPolicyRepository.sensorLabel(choice)
                                        else AppPolicyRepository.clipboardLabel(choice)
                                    ),
                                selected =
                                    UwuAppPolicyManager.policy(state!!, name, type) == choice,
                                onSelect = { change(type, choice) },
                                enabled =
                                    !busy &&
                                        (feature != "clipboard" ||
                                            state!!.getBoolean("_clipboard_editable")),
                            )
                        }
                    }
                }
            }
        }
    }
}
