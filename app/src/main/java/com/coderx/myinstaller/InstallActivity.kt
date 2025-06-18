package com.coderx.myinstaller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.coderx.myinstaller.adapter.AppListAdapter
import com.coderx.myinstaller.databinding.ActivityInstallBinding
import com.coderx.myinstaller.utils.EnhancedInstallationManager
import kotlinx.coroutines.launch

class InstallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstallBinding
    private lateinit var installationManager: EnhancedInstallationManager
    private lateinit var appAdapter: AppListAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkInstallPermission()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.canRequestPackageInstalls()) {
                setupUI()
            } else {
                showInstallPermissionDeniedDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityInstallBinding.inflate(layoutInflater)
            setContentView(binding.root)

            installationManager = EnhancedInstallationManager(this)

            checkPermissions()
        } catch (e: Exception) {
            handleError("Failed to initialize app", e)
        }
    }

    private fun checkPermissions() {
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    checkInstallPermission()
                }
                else -> {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        setupUI()
                    }
                }
            }
        } catch (e: Exception) {
            handleError("Permission check failed", e)
        }
    }

    private fun checkInstallPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!packageManager.canRequestPackageInstalls()) {
                    showInstallPermissionDialog()
                } else {
                    setupUI()
                }
            } else {
                setupUI()
            }
        } catch (e: Exception) {
            handleError("Install permission check failed", e)
        }
    }

    private fun showInstallPermissionDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("🔐 Install Permission Required")
                .setMessage("To install apps like Play Store, this app needs permission to install other apps. Please enable 'Install unknown apps' in the settings.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        installPermissionLauncher.launch(intent)
                    } catch (e: Exception) {
                        handleError("Failed to open settings", e)
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            handleError("Failed to show permission dialog", e)
        }
    }

    private fun showPermissionDeniedDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Permission Required")
                .setMessage("This app requires storage permission to function properly.")
                .setPositiveButton("Retry") { _, _ ->
                    checkPermissions()
                }
                .setNegativeButton("Exit") { _, _ ->
                    finish()
                }
                .show()
        } catch (e: Exception) {
            handleError("Failed to show permission denied dialog", e)
        }
    }

    private fun showInstallPermissionDeniedDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("❌ Install Permission Required")
                .setMessage("Without install permission, this app cannot install other apps.")
                .setPositiveButton("Retry") { _, _ ->
                    checkInstallPermission()
                }
                .setNegativeButton("Exit") { _, _ ->
                    finish()
                }
                .show()
        } catch (e: Exception) {
            handleError("Failed to show install permission denied dialog", e)
        }
    }

    private fun setupUI() {
        try {
            setupRecyclerView()
            setupObservers()
            loadApps()
        } catch (e: Exception) {
            handleError("Failed to setup UI", e)
        }
    }

    private fun setupRecyclerView() {
        try {
            appAdapter = AppListAdapter(
                onInstallClick = { appInfo ->
                    try {
                        installationManager.installApp(appInfo)
                    } catch (e: Exception) {
                        handleError("Failed to start installation", e)
                    }
                },
                onLaunchClick = { appInfo ->
                    try {
                        installationManager.launchApp(appInfo.packageName)
                    } catch (e: Exception) {
                        handleError("Failed to launch app", e)
                    }
                },
                checkInstallationState = { appInfo ->
                    try {
                        installationManager.checkInstallationState(appInfo)
                    } catch (e: Exception) {
                        com.coderx.myinstaller.data.InstallationState.NOT_INSTALLED
                    }
                }
            )

            binding.recyclerViewApps.apply {
                layoutManager = LinearLayoutManager(this@InstallActivity)
                adapter = appAdapter
                setHasFixedSize(true)
            }
        } catch (e: Exception) {
            handleError("Failed to setup RecyclerView", e)
        }
    }

    private fun setupObservers() {
        try {
            installationManager.installationProgress.observe(this) { progress ->
                try {
                    binding.progressBar.progress = progress
                    binding.textProgress.text = "$progress%"
                } catch (e: Exception) {
                    // Ignore UI update errors
                }
            }

            installationManager.installationState.observe(this) { state ->
                try {
                    binding.progressContainer.visibility = when (state) {
                        com.coderx.myinstaller.data.InstallationState.INSTALLING -> View.VISIBLE
                        else -> View.GONE
                    }

                    appAdapter.notifyDataSetChanged()

                    when (state) {
                        com.coderx.myinstaller.data.InstallationState.INSTALLED -> {
                            Toast.makeText(this, "✅ Installation completed successfully!", Toast.LENGTH_SHORT).show()
                        }
                        com.coderx.myinstaller.data.InstallationState.FAILED -> {
                            Toast.makeText(this, "❌ Installation failed. Please try again.", Toast.LENGTH_LONG).show()
                        }
                        else -> { /* No action needed */ }
                    }
                } catch (e: Exception) {
                    // Ignore UI update errors
                }
            }

            installationManager.currentApp.observe(this) { app ->
                try {
                    binding.textCurrentApp.text = if (app != null) {
                        "Installing ${app.appName}..."
                    } else {
                        ""
                    }
                } catch (e: Exception) {
                    // Ignore UI update errors
                }
            }
        } catch (e: Exception) {
            handleError("Failed to setup observers", e)
        }
    }

    private fun loadApps() {
        lifecycleScope.launch {
            try {
                val apps = installationManager.getAvailableApps()
                appAdapter.submitList(apps)
                
                if (apps.isEmpty()) {
                    Toast.makeText(this@InstallActivity, "No apps available for installation", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                handleError("Failed to load apps", e)
            }
        }
    }

    private fun handleError(message: String, exception: Exception) {
        try {
            Toast.makeText(this, "$message: ${exception.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // If even Toast fails, just finish the activity
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (::appAdapter.isInitialized) {
                appAdapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            // Ignore errors on resume
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::installationManager.isInitialized) {
                installationManager.cleanup()
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}