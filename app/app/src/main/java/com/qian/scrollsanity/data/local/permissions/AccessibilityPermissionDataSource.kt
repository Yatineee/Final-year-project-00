package com.qian.scrollsanity.data.local.permissions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.qian.scrollsanity.controller.blocker.old.monitor.AccessibilityMonitorService

class AccessibilityPermissionDataSource(
    private val context: Context
) {
    companion object {
        private const val TAG = "AccessibilityPermission"
    }

    fun hasPermission(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )

        Log.d(TAG, "Accessibility enabled setting: $accessibilityEnabled")

        if (accessibilityEnabled != 1) {
            Log.d(TAG, "Accessibility is globally disabled")
            return false
        }

        val expectedComponent = ComponentName(
            context,
            AccessibilityMonitorService::class.java
        ).flattenToString()

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        Log.d(TAG, "Expected accessibility service: $expectedComponent")
        Log.d(TAG, "Enabled services: $enabledServices")

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)

        while (splitter.hasNext()) {
            val service = splitter.next()
            if (service.equals(expectedComponent, ignoreCase = true)) {
                Log.d(TAG, "Accessibility service found: true")
                return true
            }
        }

        Log.d(TAG, "Accessibility service found: false")
        return false
    }

    fun openSettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}