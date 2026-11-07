/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.clipboard

import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
import org.uwuaosp.compose.widget.preference.MainSwitchPreference
import org.uwuaosp.compose.widget.preference.SettingsSectionHeader

class ClipboardPolicyActivity : ComponentActivity() {
    private var policyWatcher: android.app.AppOpsManager.OnOpChangedListener? = null
    private val refreshToken = mutableIntStateOf(0)
    private val observer =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refreshToken.intValue++
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SettingsTheme { ClipboardPolicyScreen(refreshToken.intValue, ::finish) } }
    }

    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.UWU_APP_CLIPBOARD_PROMPTS_ENABLED),
            false,
            observer,
        )
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
        contentResolver.unregisterContentObserver(observer)
        super.onStop()
    }
}

@Composable
private fun ClipboardPolicyScreen(refreshToken: Int, onNavigateUp: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { ClipboardPolicyAppRepository(context) }
    var apps by remember { mutableStateOf<List<ClipboardPolicyAppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var retryToken by remember { mutableIntStateOf(0) }
    var promptsEnabled by
        remember(refreshToken) {
            mutableStateOf(ClipboardPolicySecureSettings.isPromptEnabled(context))
        }

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
        title = stringResource(R.string.clipboard_policy_title),
        searchQuery = query,
        searchPlaceholder = stringResource(R.string.clipboard_policy_search_apps),
        clearSearchContentDescription = stringResource(R.string.clipboard_policy_search_close),
        onSearchQueryChange = { query = it },
        onNavigateUp = onNavigateUp,
    ) {
        item {
            MainSwitchPreference(
                title = stringResource(R.string.clipboard_policy_prompt_enabled),
                checked = promptsEnabled,
                onCheckedChange = { enabled ->
                    if (ClipboardPolicySecureSettings.setPromptEnabled(context, enabled)) {
                        promptsEnabled = enabled
                        retryToken++
                    } else {
                        Toast.makeText(
                                context,
                                R.string.clipboard_policy_prompt_update_failed,
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                    }
                },
            )
        }
        item {
            SettingsSectionHeader(title = stringResource(R.string.clipboard_policy_apps_category))
        }
        when {
            loading -> item { AppListLoading() }
            failed ->
                item {
                    AppListError(
                        text = stringResource(R.string.clipboard_policy_load_failed),
                        retryText = stringResource(R.string.clipboard_policy_retry),
                        onRetry = { retryToken++ },
                    )
                }
            filtered.isEmpty() ->
                item { AppListEmpty(text = stringResource(R.string.clipboard_policy_no_apps)) }
            else ->
                items(filtered.size, key = { filtered[it].packageName }) { index ->
                    val app = filtered[index]
                    Column {
                        ClipboardPolicyRow(app = app, index = index, itemCount = filtered.size) {
                            operation,
                            policy ->
                            if (
                                ClipboardPolicySecureSettings.setPolicy(
                                    context,
                                    app.packageName,
                                    operation,
                                    policy,
                                )
                            ) {
                                apps =
                                    apps.map { entry ->
                                        if (entry.packageName == app.packageName) {
                                            if (
                                                operation ==
                                                    ClipboardPolicySecureSettings.OPERATION_READ
                                            ) {
                                                entry.copy(readPolicy = policy)
                                            } else {
                                                entry.copy(writePolicy = policy)
                                            }
                                        } else {
                                            entry
                                        }
                                    }
                            } else {
                                Toast.makeText(
                                        context,
                                        R.string.clipboard_policy_update_failed,
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
private fun ClipboardPolicyRow(
    app: ClipboardPolicyAppEntry,
    index: Int,
    itemCount: Int,
    onPolicySelected: (Int, Int) -> Unit,
) {
    var expanded by remember(app.packageName) { mutableStateOf(false) }
    AppListItem(
        label = app.label,
        packageName = app.packageName,
        icon = app.icon.asImageBitmap(),
        index = index,
        itemCount = itemCount,
        onClick = { expanded = true },
    ) {
        ClipboardPolicyMenu(
            readPolicy = app.readPolicy,
            writePolicy = app.writePolicy,
            expanded = expanded,
            onExpandedChange = { expanded = it },
            onPolicySelected = onPolicySelected,
        )
    }
}

@Composable
private fun ClipboardPolicyMenu(
    readPolicy: Int,
    writePolicy: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPolicySelected: (Int, Int) -> Unit,
) {
    Box {
        Row(
            modifier =
                Modifier.clip(RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button, onClick = { onExpandedChange(true) })
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    stringResource(
                        R.string.clipboard_policy_read_write_summary,
                        if (readPolicy == writePolicy) {
                            clipboardPolicyLabel(readPolicy)
                        } else {
                            stringResource(R.string.clipboard_policy_separate)
                        },
                    ),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left_down_line),
                contentDescription = stringResource(R.string.clipboard_policy_app_policy),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        SettingsDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            ClipboardPolicyMenuSection(
                label = stringResource(R.string.clipboard_policy_read),
                groupIndex = 0,
                operation = ClipboardPolicySecureSettings.OPERATION_READ,
                policy = readPolicy,
                onPolicySelected = { operation, policy ->
                    onExpandedChange(false)
                    onPolicySelected(operation, policy)
                },
            )
            ClipboardPolicyMenuSection(
                label = stringResource(R.string.clipboard_policy_write),
                groupIndex = 1,
                operation = ClipboardPolicySecureSettings.OPERATION_WRITE,
                policy = writePolicy,
                onPolicySelected = { operation, policy ->
                    onExpandedChange(false)
                    onPolicySelected(operation, policy)
                },
            )
        }
    }
}

@Composable
private fun ClipboardPolicyMenuSection(
    label: String,
    groupIndex: Int,
    operation: Int,
    policy: Int,
    onPolicySelected: (Int, Int) -> Unit,
) {
    val policies =
        listOf(
            ClipboardPolicySecureSettings.POLICY_ALLOW,
            ClipboardPolicySecureSettings.POLICY_ASK,
            ClipboardPolicySecureSettings.POLICY_DENY,
        )
    SettingsMenuGroup(index = groupIndex, groupCount = 2, title = label) {
        policies.forEachIndexed { index, choice ->
            SettingsMenuItem(
                text = clipboardPolicyLabel(choice),
                selected = policy == choice,
                onClick = { onPolicySelected(operation, choice) },
                index = index,
                itemCount = policies.size,
            )
        }
    }
}

@Composable
private fun clipboardPolicyLabel(policy: Int): String =
    stringResource(com.android.settings.uwu.apppolicies.AppPolicyRepository.clipboardLabel(policy))
