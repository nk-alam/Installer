package com.coderx.installer

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.coderx.installer.databinding.ActivityMainBinding
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val targetPackage = "com.shgrjisb1p8.app"
    private var permissionRequested = false

    companion object {
        private const val TAG = "MainActivity"

        // Use WeakReference to prevent memory leaks
        private var activityReference: WeakReference<MainActivity>? = null

        fun getCurrentActivity(): MainActivity? = activityReference?.get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set weak reference
        activityReference = WeakReference(this)

        setupUI()
        checkIfInstalled()
    }

    private fun setupUI() {
        binding.updateButton.setOnClickListener {
            handleButtonClick()
        }
    }

    private fun handleButtonClick() {
        if (packageManager.isPackageInstalled(targetPackage)) {
            openApp()
        } else {
            if (!canRequestPackageInstalls()) {
                if (!permissionRequested) {
                    showPermissionDialog()
                } else {
                    Toast.makeText(this, "Please enable 'Install unknown apps' in settings", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Start installation activity
                InstallationActivity.startInstallation(this)
            }
        }
    }

    private fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            true // No special permission needed for older versions
        }
    }

    fun updateButtonState() {
        if (!isDestroyed && !isFinishing) {
            runOnUiThread {
                binding.updateButton.apply {
                    when {
                        packageManager.isPackageInstalled(targetPackage) -> {
                            text = "Open"
                            isEnabled = true
                        }
                        else -> {
                            text = "Update"
                            isEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun checkIfInstalled() {
        updateButtonState()
    }

    private fun openApp() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                showError("Failed to open app: No launch intent found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app", e)
            showError("Failed to open app: ${e.message}")
        }
    }

    private fun showPermissionDialog() {
        if (isDestroyed || isFinishing) return

        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs permission to install packages. You will be redirected to settings to enable this permission.")
            .setPositiveButton("OK") { _, _ ->
                requestInstallPermission()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestInstallPermission() {
        permissionRequested = true
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:$packageName"))
            } else {
                Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
            }
            startActivity(intent)

            Toast.makeText(this, "Please enable 'Install unknown apps' and return to this app", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Could not open settings", e)
            showError("Could not open settings: ${e.message}")
        }
    }

    private fun showError(message: String) {
        if (!isDestroyed && !isFinishing) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun handleInstallationError(message: String) {
        Log.e(TAG, "Installation error: $message")

        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                updateButtonState()

                AlertDialog.Builder(this)
                    .setTitle("Installation Error")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    fun handleInstallationSuccess() {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                updateButtonState()
                Toast.makeText(this, "App installed successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Auto-install if permission was granted and app is not installed
        if (canRequestPackageInstalls() &&
            !packageManager.isPackageInstalled(targetPackage) &&
            permissionRequested) {

            permissionRequested = false
            // Start installation activity
            InstallationActivity.startInstallation(this)
        } else {
            updateButtonState()
        }
    }

    override fun onDestroy() {
        // Clean up weak reference
        activityReference?.clear()
        activityReference = null

        super.onDestroy()
    }
}

// Extension function to check if package is installed
fun PackageManager.isPackageInstalled(packageName: String): Boolean {
    return try {
        getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}