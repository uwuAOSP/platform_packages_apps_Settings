/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.uwu.apppolicies

import android.Manifest
import android.app.UwuAppPolicyManager
import android.content.ContentProvider
import android.content.ContentValues
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings

class AppPolicySummaryProvider : ContentProvider() {
    private val uri = Uri.parse("content://${AppPolicyRepository.AUTHORITY}")

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        val handler = Handler(Looper.getMainLooper())
        UwuAppPolicyManager.watch(ctx, handler) { user ->
            if (user == UserHandle.myUserId()) ctx.contentResolver.notifyChange(uri, null)
        }
        val observer =
            object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    ctx.contentResolver.notifyChange(uri, null)
                }
            }
        for (key in
            arrayOf(
                Settings.Secure.UWU_APP_CLIPBOARD_PROMPTS_ENABLED,
                Settings.Secure.DEFAULT_INPUT_METHOD,
            )) {
            ctx.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(key),
                false,
                observer,
            )
        }
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val ctx = requireNotNull(context)
        ctx.enforceCallingOrSelfPermission(
            Manifest.permission.MANAGE_APP_OPS_MODES,
            "Read app policy summaries",
        )
        if (UserHandle.getUserId(Binder.getCallingUid()) != UserHandle.myUserId()) {
            ctx.enforceCallingOrSelfPermission(
                Manifest.permission.INTERACT_ACROSS_USERS_FULL,
                "Read another user's app policies",
            )
        }
        require(method == "get_app_policy_summary" && arg != null)
        val identity = Binder.clearCallingIdentity()
        try {
            return AppPolicyRepository.summaries(ctx, arg)
        } finally {
            Binder.restoreCallingIdentity(identity)
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = error("Read-only provider")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        error("Read-only provider")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = error("Read-only provider")
}
