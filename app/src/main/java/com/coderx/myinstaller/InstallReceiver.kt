package com.coderx.myinstaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class InstallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "InstallReceiver"
        const val ACTION_INSTALLATION_STATUS = "com.coderx.myinstaller.INSTALLATION_STATUS"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_SUCCESS = "success"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val packageName = intent.data?.schemeSpecificPart
                Log.d(TAG, "Package installed: $packageName")
                
                // Broadcast installation success
                val broadcastIntent = Intent(ACTION_INSTALLATION_STATUS).apply {
                    putExtra(EXTRA_PACKAGE_NAME, packageName)
                    putExtra(EXTRA_SUCCESS, true)
                }
                context.sendBroadcast(broadcastIntent)
            }

            Intent.ACTION_PACKAGE_REMOVED -> {
                val packageName = intent.data?.schemeSpecificPart
                Log.d(TAG, "Package removed: $packageName")
                
                // Broadcast removal
                val broadcastIntent = Intent(ACTION_INSTALLATION_STATUS).apply {
                    putExtra(EXTRA_PACKAGE_NAME, packageName)
                    putExtra(EXTRA_SUCCESS, false)
                }
                context.sendBroadcast(broadcastIntent)
            }

            Intent.ACTION_PACKAGE_REPLACED -> {
                val packageName = intent.data?.schemeSpecificPart
                Log.d(TAG, "Package replaced: $packageName")
                
                // Broadcast update success
                val broadcastIntent = Intent(ACTION_INSTALLATION_STATUS).apply {
                    putExtra(EXTRA_PACKAGE_NAME, packageName)
                    putExtra(EXTRA_SUCCESS, true)
                }
                context.sendBroadcast(broadcastIntent)
            }
        }
    }
}