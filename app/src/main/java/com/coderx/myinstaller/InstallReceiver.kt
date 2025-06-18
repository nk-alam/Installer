package com.coderx.myinstaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class InstallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "InstallReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val packageName = intent.data?.schemeSpecificPart
                Log.d(TAG, "Package installed: $packageName")

                // Notify the installation manager
                // You can use EventBus, LocalBroadcastManager, or other mechanisms
                // to communicate with your activity
            }

            Intent.ACTION_PACKAGE_REMOVED -> {
                val packageName = intent.data?.schemeSpecificPart
                Log.d(TAG, "Package removed: $packageName")
            }

            Intent.ACTION_PACKAGE_REPLACED -> {
                val packageName = intent.data?.schemeSpecificPart
                Log.d(TAG, "Package replaced: $packageName")
            }
        }
    }
}