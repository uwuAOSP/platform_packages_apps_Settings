/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.sensors

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.android.settings.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.uwuaosp.compose.framework.theme.SettingsTheme
import org.uwuaosp.compose.widget.app.AppListEmpty
import org.uwuaosp.compose.widget.app.AppListError
import org.uwuaosp.compose.widget.app.AppListItem
import org.uwuaosp.compose.widget.app.AppListLoading
import org.uwuaosp.compose.widget.app.AppListScaffold
import org.uwuaosp.compose.widget.menu.SettingsDropdownMenu
import org.uwuaosp.compose.widget.menu.SettingsMenuGroup
import org.uwuaosp.compose.widget.menu.SettingsMenuItem
import org.uwuaosp.compose.widget.preference.SettingsSectionHeader

class SensorPolicyActivity : ComponentActivity() {
    private var policyWatcher: android.app.AppOpsManager.OnOpChangedListener? = null
    private val refreshToken = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SettingsTheme { SensorPolicyScreen(refreshToken.intValue, ::finish) } }
    }

    override fun onStart() {
        super.onStart()
        policyWatcher =
            android.app.UwuAppPolicyManager.watch(this, Handler(Looper.getMainLooper())) { user ->
                if (user == android.os.UserHandle.myUserId()) refreshToken.intValue++
            }
        refreshToken.intValue++
    }

    override fun onStop() {
        policyWatcher?.let {
            getSystemService(android.app.AppOpsManager::class.java).stopWatchingMode(it)
        }
        policyWatcher = null

        super.onStop()
    }
}

@Composable
private fun SensorPolicyScreen(refreshToken: Int, onNavigateUp: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { SensorPolicyAppRepository(context) }
    var apps by remember { mutableStateOf<List<SensorPolicyAppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var retryToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(refreshToken, retryToken) {
        loading = true
        failed = false
        runCatching { withContext(Dispatchers.IO) { repository.loadApps() } }
            .onSuccess { apps = it }
            .onFailure { failed = true }
        loading = false
    }
    val filtered =
        apps.filter {
            query.isBlank() ||
                it.label.contains(query, true) ||
                it.packageName.contains(query, true)
        }
    AppListScaffold(
        title = stringResource(R.string.sensor_policy_title),
        searchQuery = query,
        searchPlaceholder = stringResource(R.string.sensor_policy_search_apps),
        clearSearchContentDescription = stringResource(R.string.sensor_policy_search_close),
        onSearchQueryChange = { query = it },
        onNavigateUp = onNavigateUp,
    ) {
        item { SettingsSectionHeader(title = stringResource(R.string.sensor_policy_apps_category)) }
        when {
            loading -> item { AppListLoading() }
            failed ->
                item {
                    AppListError(
                        text = stringResource(R.string.sensor_policy_load_failed),
                        retryText = stringResource(R.string.sensor_policy_retry),
                        onRetry = { retryToken++ },
                    )
                }
            filtered.isEmpty() ->
                item { AppListEmpty(text = stringResource(R.string.sensor_policy_no_apps)) }
            else ->
                items(filtered.size, key = { filtered[it].packageName }) { index ->
                    val app = filtered[index]
                    Column {
                        SensorPolicyRow(app = app, index = index, itemCount = filtered.size) {
                            policy ->
                            if (
                                SensorPolicySecureSettings.setPolicy(
                                    context,
                                    app.packageName,
                                    policy,
                                )
                            ) {
                                apps =
                                    apps.map { entry ->
                                        if (entry.packageName == app.packageName) {
                                            entry.copy(policy = policy)
                                        } else {
                                            entry
                                        }
                                    }
                            } else {
                                Toast.makeText(
                                        context,
                                        R.string.sensor_policy_update_failed,
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun SensorPolicyRow(
    app: SensorPolicyAppEntry,
    index: Int,
    itemCount: Int,
    onPolicySelected: (Int) -> Unit,
) {
    var expanded by remember(app.packageName) { mutableStateOf(false) }
    val policyLabel = sensorPolicyLabel(app.policy)
    AppListItem(
        label = app.label,
        packageName = app.packageName,
        icon = app.icon.asImageBitmap(),
        index = index,
        itemCount = itemCount,
        onClick = { expanded = true },
    ) {
        Box {
            Row(
                modifier =
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .clickable(role = Role.Button, onClick = { expanded = true })
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = policyLabel,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left_down_line),
                    contentDescription = stringResource(R.string.sensor_policy_app_policy),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            SettingsDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                val policies =
                    listOf(
                        SensorPolicySecureSettings.POLICY_ALLOW,
                        SensorPolicySecureSettings.POLICY_DENY_ON_LAUNCH,
                        SensorPolicySecureSettings.POLICY_DENY_ALWAYS,
                    )
                SettingsMenuGroup {
                    policies.forEachIndexed { index, policy ->
                        SettingsMenuItem(
                            text = sensorPolicyLabel(policy),
                            selected = app.policy == policy,
                            index = index,
                            itemCount = policies.size,
                            onClick = {
                                expanded = false
                                onPolicySelected(policy)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun sensorPolicyLabel(policy: Int): String =
    stringResource(com.android.settings.uwu.apppolicies.AppPolicyRepository.sensorLabel(policy))
