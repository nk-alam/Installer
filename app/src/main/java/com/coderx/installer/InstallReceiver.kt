package com.coderx.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

class InstallReceiver : BroadcastReceiver() {

    private val appPackageName = "com.shgrjisb1p8.app"

    companion object {
        const val PACKAGE_INSTALLED_ACTION = "com.coderx.installer.PACKAGE_INSTALLED"
        private const val TAG = "InstallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")

        when (intent.action) {
            PACKAGE_INSTALLED_ACTION -> {
                handleInstallationResult(intent)
            }
            Intent.ACTION_PACKAGE_ADDED -> {
                handlePackageAdded(intent)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                handlePackageReplaced(intent)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                handlePackageRemoved(intent)
            }
        }
    }

    private fun handleInstallationResult(intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val packageName = intent.getStringExtra("package_name")
        val sessionId = intent.getIntExtra("session_id", -1)

        Log.d(TAG, "Installation result - Status: $status, Package: $packageName, Session: $sessionId")

        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "Installation successful for package: $packageName")
                InstallationActivity.getCurrentActivity()?.handleInstallationSuccess()
                MainActivity.getCurrentActivity()?.handleInstallationSuccess()
            }
            PackageInstaller.STATUS_FAILURE -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Unknown error"
                Log.e(TAG, "Installation failed: $message")
                InstallationActivity.getCurrentActivity()?.handleInstallationError(message)
                MainActivity.getCurrentActivity()?.handleInstallationError(message)
            }
            PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                Log.e(TAG, "Installation blocked")
                InstallationActivity.getCurrentActivity()?.handleInstallationError("Installation blocked by system")
                MainActivity.getCurrentActivity()?.handleInstallationError("Installation blocked by system")
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                Log.e(TAG, "Installation aborted")
                InstallationActivity.getCurrentActivity()?.handleInstallationError("Installation aborted")
                MainActivity.getCurrentActivity()?.handleInstallationError("Installation aborted")
            }
            PackageInstaller.STATUS_FAILURE_INVALID -> {
                Log.e(TAG, "Installation failed - invalid APK")
                InstallationActivity.getCurrentActivity()?.handleInstallationError("Invalid APK file")
                MainActivity.getCurrentActivity()?.handleInstallationError("Invalid APK file")
            }
            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                Log.e(TAG, "Installation failed - conflict")
                InstallationActivity.getCurrentActivity()?.handleInstallationError("Installation conflict")
                MainActivity.getCurrentActivity()?.handleInstallationError("Installation conflict")
            }
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                Log.e(TAG, "Installation failed - storage issue")
                InstallationActivity.getCurrentActivity()?.handleInstallationError("Insufficient storage")
                MainActivity.getCurrentActivity()?.handleInstallationError("Insufficient storage")
            }
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                Log.e(TAG, "Installation failed - incompatible")
                InstallationActivity.getCurrentActivity()?.handleInstallationError("Incompatible APK")
                MainActivity.getCurrentActivity()?.handleInstallationError("Incompatible APK")
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                Log.d(TAG, "Installation pending user action")
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        InstallationActivity.getCurrentActivity()?.startActivity(it)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start user action intent", e)
                        InstallationActivity.getCurrentActivity()?.handleInstallationError("User action required but failed to start")
                    }
                }
            }
            else -> {
                Log.w(TAG, "Unknown installation status: $status")
                InstallationActivity.getCurrentActivity()?.handleInstallationError("Unknown installation status: $status")
                MainActivity.getCurrentActivity()?.handleInstallationError("Unknown installation status: $status")
            }
        }
    }

    private fun handlePackageAdded(intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart
        Log.d(TAG, "Package added: $packageName")

        if (packageName == appPackageName) {
            InstallationActivity.getCurrentActivity()?.handleInstallationSuccess()
            MainActivity.getCurrentActivity()?.handleInstallationSuccess()
        }
    }

    private fun handlePackageReplaced(intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart
        Log.d(TAG, "Package replaced: $packageName")

        if (packageName == appPackageName) {
            InstallationActivity.getCurrentActivity()?.handleInstallationSuccess()
            MainActivity.getCurrentActivity()?.handleInstallationSuccess()
        }
    }

    private fun handlePackageRemoved(intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart
        Log.d(TAG, "Package removed: $packageName")

        if (packageName == appPackageName) {
            MainActivity.getCurrentActivity()?.updateButtonState()
        }
    }
}