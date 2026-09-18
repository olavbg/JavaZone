package com.olavbg.javazone.ui.settings

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class AppPermissions(
    val canScheduleExact: Boolean,
    val canPostNotifications: Boolean,
    val isAggressiveOem: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = true
) {
    val allGranted: Boolean get() = canScheduleExact && canPostNotifications
    val showBatteryHint: Boolean get() = isAggressiveOem && !isIgnoringBatteryOptimizations
}

internal fun computeAppPermissions(context: Context): AppPermissions {
    val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    } else {
        true
    }
    val canPostNotifications = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> true
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED -> true
        else -> false
    }
    return AppPermissions(
        canScheduleExact = canScheduleExact,
        canPostNotifications = canPostNotifications,
        isAggressiveOem = isAggressiveOem(),
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
    )
}

private val AGGRESSIVE_OEM_MANUFACTURERS = setOf(
    "xiaomi", "redmi", "poco",
    "huawei", "honor",
    "oppo", "realme", "oneplus",
    "vivo", "iqoo",
    "samsung",
    "meizu",
    "asus", "nokia", "tecno", "infinix", "itel", "wiko"
)

private fun isAggressiveOem(): Boolean = isAggressiveOem(Build.MANUFACTURER, Build.BRAND)

internal fun isAggressiveOem(manufacturer: String, brand: String): Boolean {
    val manufacturerLower = manufacturer.lowercase()
    val brandLower = brand.lowercase()
    return AGGRESSIVE_OEM_MANUFACTURERS.any {
        manufacturerLower.contains(it) || brandLower.contains(it)
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

internal fun openBatteryOptimizationSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            )
        }
    }
}

@Suppress("InlinedApi")
internal fun openExactAlarmSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )
    }
}

internal fun openAppNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        )
    }
}

internal fun shouldShowPermissionRationale(context: Context, permission: String): Boolean {
    val activity = context.findActivity() ?: return true
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun isLocalBuild(context: Context): Boolean {
    if ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) return true
    val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        }.getOrNull()
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getInstallerPackageName(context.packageName)
    }
    return installer != "com.android.vending"
}