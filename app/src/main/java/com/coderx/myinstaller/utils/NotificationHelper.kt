package com.coderx.myinstaller.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.coderx.myinstaller.InstallActivity
import com.coderx.myinstaller.R

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "apk_installer_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "APK Installation",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for APK installation progress"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showInstallationProgress(appName: String, progress: Int) {
        val intent = Intent(context, InstallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Installing $appName")
            .setContentText("$progress% complete")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showInstallationComplete(appName: String, success: Boolean) {
        val intent = Intent(context, InstallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(if (success) "Installation Complete" else "Installation Failed")
            .setContentText(
                if (success) "$appName installed successfully" 
                else "Failed to install $appName"
            )
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done 
                else android.R.drawable.stat_notify_error
            )
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelInstallationNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}