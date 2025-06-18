package com.coderx.myinstaller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.coderx.myinstaller.adapter.AppListAdapter
import com.coderx.myinstaller.databinding.ActivityInstallBinding
import com.coderx.myinstaller.utils.InstallationManager
import kotlinx.coroutines.launch

class InstallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstallBinding
    private lateinit var installationManager: InstallationManager
    private lateinit var appAdapter: AppListAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupUI()
        } else {
            Toast.makeText(this, "Permission required for installation", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInstallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        installationManager = InstallationManager(this)

        checkPermissions()
    }

    private fun checkPermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                if (!packageManager.canRequestPackageInstalls()) {
                    // Request install permission
                    requestPermissionLauncher.launch(Manifest.permission.REQUEST_INSTALL_PACKAGES)
                } else {
                    setupUI()
                }
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

            // Refresh the adapter to update button states
            appAdapter.notifyDataSetChanged()
        }

        installationManager.currentApp.observe(this) { app ->
            binding.textCurrentApp.text = app?.appName ?: ""
        }
    }

    private fun loadApps() {
        lifecycleScope.launch {
            val apps = installationManager.getInstalledApps()
            appAdapter.submitList(apps)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        installationManager.cleanup()
    }
}