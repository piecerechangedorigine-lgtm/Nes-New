package com.novafinance.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.novafinance.app.R

private const val CHANNEL_ID_FINANCIAL_HEALTH = "financial_health"

/**
 * One channel for every notification this app posts — debt reminders
 * and forecast alerts are both "things about your money that need
 * attention soon," the same category from a person's notification
 * settings perspective, not two separate toggles to manage.
 */
fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        CHANNEL_ID_FINANCIAL_HEALTH,
        "Financial health alerts",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Debt due dates and month-end forecast warnings."
    }
    val manager = context.getSystemService(NotificationManager::class.java)
    manager?.createNotificationChannel(channel)
}

/**
 * Checks the real runtime grant before posting — a person can revoke
 * notification access in system settings independent of whatever
 * `ProfileSettings.areNotificationsEnabled` says, and posting without
 * checking would throw a `SecurityException` on API 33+. Returns
 * whether the notification was actually posted, so a caller can tell
 * "posted" apart from "silently skipped because permission is off."
 */
fun postFinancialHealthNotification(context: Context, notificationId: Int, title: String, message: String): Boolean {
    val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    if (!hasPermission) return false

    val notification = NotificationCompat.Builder(context, CHANNEL_ID_FINANCIAL_HEALTH)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
    return true
}
