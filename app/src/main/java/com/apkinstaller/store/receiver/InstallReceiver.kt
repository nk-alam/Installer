package com.apkinstaller.store.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class InstallReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "InstallReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val action = intent.action
        val packageName = intent.data?.schemeSpecificPart
        
        Log.d(TAG, "Received action: $action for package: $packageName")
        
        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                Log.d(TAG, "Package installed: $packageName")
                // Handle package installation
                handlePackageInstalled(context, packageName)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                Log.d(TAG, "Package removed: $packageName")
                // Handle package removal
                handlePackageRemoved(context, packageName)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Package replaced: $packageName")
                // Handle package replacement
                handlePackageReplaced(context, packageName)
            }
        }
    }
    
    private fun handlePackageInstalled(context: Context, packageName: String?) {
        if (packageName == null) return
        
        // Update app status in database or preferences
        val sharedPrefs = context.getSharedPreferences("app_status", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("${packageName}_installed", true).apply()
        
        // Send broadcast to update UI
        val updateIntent = Intent("com.apkinstaller.store.PACKAGE_STATUS_CHANGED").apply {
            putExtra("package_name", packageName)
            putExtra("status", "installed")
        }
        context.sendBroadcast(updateIntent)
    }
    
    private fun handlePackageRemoved(context: Context, packageName: String?) {
        if (packageName == null) return
        
        // Update app status in database or preferences
        val sharedPrefs = context.getSharedPreferences("app_status", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("${packageName}_installed", false).apply()
        
        // Send broadcast to update UI
        val updateIntent = Intent("com.apkinstaller.store.PACKAGE_STATUS_CHANGED").apply {
            putExtra("package_name", packageName)
            putExtra("status", "removed")
        }
        context.sendBroadcast(updateIntent)
    }
    
    private fun handlePackageReplaced(context: Context, packageName: String?) {
        if (packageName == null) return
        
        // Handle package replacement (update)
        val updateIntent = Intent("com.apkinstaller.store.PACKAGE_STATUS_CHANGED").apply {
            putExtra("package_name", packageName)
            putExtra("status", "updated")
        }
        context.sendBroadcast(updateIntent)
    }
}