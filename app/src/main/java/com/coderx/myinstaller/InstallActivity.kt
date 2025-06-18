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
    ) { result ->
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

        binding = ActivityInstallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        installationManager = EnhancedInstallationManager(this)

        checkPermissions()
    }

    private fun checkPermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    checkInstallPermission()
                }
            }
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
    }

    private fun checkInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                showInstallPermissionDialog()
            } else {
                setupUI()
            }
        } else {
            setupUI()
        }
    }

    private fun showInstallPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("🔐 Install Permission Required")
            .setMessage("To install apps like Play Store, this app needs permission to install other apps. Please enable 'Install unknown apps' in the settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                installPermissionLauncher.launch(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedDialog() {
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
    }

    private fun showInstallPermissionDeniedDialog() {
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
    }

    private fun setupUI() {
        setupRecyclerView()
        setupObservers()
        loadApps()
    }

    private fun setupRecyclerView() {
        appAdapter = AppListAdapter(
            onInstallClick = { appInfo ->
                installationManager.installApp(appInfo)
            },
            onLaunchClick = { appInfo ->
                installationManager.launchApp(appInfo.packageName)
            },
            checkInstallationState = { appInfo ->
                installationManager.checkInstallationState(appInfo)
            }
        )

        binding.recyclerViewApps.apply {
            layoutManager = LinearLayoutManager(this@InstallActivity)
            adapter = appAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        installationManager.installationProgress.observe(this) { progress ->
            binding.progressBar.progress = progress
            binding.textProgress.text = "$progress%"
        }

        installationManager.installationState.observe(this) { state ->
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
        }

        installationManager.currentApp.observe(this) { app ->
            binding.textCurrentApp.text = if (app != null) {
                "Installing ${app.appName}..."
            } else {
                ""
            }
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
                Toast.makeText(this@InstallActivity, "Failed to load apps: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        installationManager.cleanup()
    }
}