package com.apkinstaller.store.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.apkinstaller.store.R
import com.apkinstaller.store.databinding.ActivityInstallerBinding
import com.apkinstaller.store.model.AppInfo
import com.apkinstaller.store.model.InstallationState
import com.apkinstaller.store.utils.ApkParser
import com.apkinstaller.store.utils.InstallationManager
import com.apkinstaller.store.viewmodel.InstallerViewModel
import kotlinx.coroutines.launch

class InstallerActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "InstallerActivity"
    }
    
    private lateinit var binding: ActivityInstallerBinding
    private lateinit var viewModel: InstallerViewModel
    private lateinit var installationManager: InstallationManager
    private lateinit var apkParser: ApkParser
    
    private var appInfo: AppInfo? = null
    
    private val installationResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.apkinstaller.store.INSTALLATION_RESULT") {
                val packageName = intent.getStringExtra("package_name") ?: ""
                val status = intent.getIntExtra("status", -1)
                
                if (packageName == appInfo?.packageName) {
                    handleInstallationResult(status)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstallerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupViewModel()
        getIntentData()
        setupUI()
        observeInstallation()
        setupClickListeners()
        registerReceiver()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
        installationManager.cleanup()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[InstallerViewModel::class.java]
        installationManager = InstallationManager(this)
        apkParser = ApkParser(this)
    }
    
    private fun getIntentData() {
        appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("app_info", AppInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("app_info")
        }
        
        if (appInfo == null) {
            Log.e(TAG, "No app info provided")
            Toast.makeText(this, "Error: No app information provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }
    
    private fun setupUI() {
        appInfo?.let { info ->
            binding.apply {
                textViewAppName.text = info.name
                textViewAppVersion.text = "Version ${info.getFormattedVersion()}"
                textViewAppSize.text = info.getFormattedSize()
                textViewAppPackage.text = "Package: ${info.packageName}"
                
                // Set app icon
                if (info.icon != null) {
                    imageViewAppIcon.setImageDrawable(info.icon)
                } else {
                    imageViewAppIcon.setImageResource(R.drawable.ic_android)
                }
                
                // Display additional information
                val additionalInfo = buildString {
                    append("Min SDK: ${info.minSdkVersion}")
                    append("\nTarget SDK: ${info.targetSdkVersion}")
                    if (info.permissions.isNotEmpty()) {
                        append("\nPermissions: ${info.permissions.size}")
                    }
                }
                textViewAppPermissions.text = additionalInfo
                
                // Check compatibility
                val deviceSdk = apkParser.getCurrentDeviceSdk()
                if (!info.isCompatible(deviceSdk)) {
                    textViewInstallStatus.text = "Incompatible with this device (requires SDK ${info.minSdkVersion}+)"
                    buttonInstall.isEnabled = false
                    return@let
                }
                
                // Check if app is already installed
                lifecycleScope.launch {
                    val isInstalled = apkParser.isAppInstalled(info.packageName)
                    if (isInstalled) {
                        val installedInfo = apkParser.getInstalledAppInfo(info.packageName)
                        if (installedInfo != null && installedInfo.versionCode >= info.versionCode) {
                            buttonInstall.visibility = View.GONE
                            buttonOpen.visibility = View.VISIBLE
                            textViewInstallStatus.text = "App is already installed"
                        } else {
                            buttonInstall.text = "Update"
                            textViewInstallStatus.text = "Update available"
                        }
                    } else {
                        buttonInstall.visibility = View.VISIBLE
                        buttonOpen.visibility = View.GONE
                        textViewInstallStatus.text = "Ready to install"
                    }
                }
            }
        }
    }
    
    private fun observeInstallation() {
        installationManager.installationProgress.observe(this) { progress ->
            binding.apply {
                when (progress.state) {
                    InstallationState.PREPARING -> {
                        buttonInstall.visibility = View.GONE
                        progressBarInstall.visibility = View.VISIBLE
                        animationView.visibility = View.VISIBLE
                        textViewInstallStatus.text = progress.message
                        progressBarInstall.progress = progress.progress
                    }
                    InstallationState.SIGNING -> {
                        buttonInstall.visibility = View.GONE
                        progressBarInstall.visibility = View.VISIBLE
                        animationView.visibility = View.VISIBLE
                        textViewInstallStatus.text = progress.message
                        progressBarInstall.progress = progress.progress
                    }
                    InstallationState.INSTALLING -> {
                        buttonInstall.visibility = View.GONE
                        progressBarInstall.visibility = View.VISIBLE
                        animationView.visibility = View.VISIBLE
                        textViewInstallStatus.text = progress.message
                        progressBarInstall.progress = progress.progress
                    }
                    InstallationState.INSTALLED -> {
                        progressBarInstall.visibility = View.GONE
                        animationView.visibility = View.GONE
                        buttonOpen.visibility = View.VISIBLE
                        textViewInstallStatus.text = "Installation completed successfully"
                        Toast.makeText(this@InstallerActivity, "App installed successfully!", Toast.LENGTH_SHORT).show()
                    }
                    InstallationState.FAILED -> {
                        progressBarInstall.visibility = View.GONE
                        animationView.visibility = View.GONE
                        buttonInstall.visibility = View.VISIBLE
                        textViewInstallStatus.text = "Installation failed: ${progress.error}"
                        Toast.makeText(this@InstallerActivity, "Installation failed", Toast.LENGTH_SHORT).show()
                    }
                    InstallationState.CANCELLED -> {
                        progressBarInstall.visibility = View.GONE
                        animationView.visibility = View.GONE
                        buttonInstall.visibility = View.VISIBLE
                        textViewInstallStatus.text = "Installation cancelled"
                    }
                    else -> {
                        // Handle other states
                    }
                }
            }
        }
        
        installationManager.installationResult.observe(this) { result ->
            Log.d(TAG, "Installation result: ${result.success} for ${result.packageName}")
            if (result.success) {
                binding.apply {
                    progressBarInstall.visibility = View.GONE
                    animationView.visibility = View.GONE
                    buttonOpen.visibility = View.VISIBLE
                    buttonInstall.visibility = View.GONE
                    textViewInstallStatus.text = "Installation completed successfully"
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.buttonInstall.setOnClickListener {
            appInfo?.let { info ->
                if (info.apkPath.isNotEmpty()) {
                    installationManager.installApk(info.apkPath, info.packageName)
                } else {
                    Toast.makeText(this, "APK file not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        binding.buttonOpen.setOnClickListener {
            openApp()
        }
        
        binding.buttonCancel.setOnClickListener {
            appInfo?.let { info ->
                installationManager.cancelInstallation(info.packageName)
            }
        }
    }
    
    private fun openApp() {
        appInfo?.let { info ->
            if (installationManager.openApp(info.packageName)) {
                // App opened successfully
            } else {
                Toast.makeText(this, "Cannot open ${info.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleInstallationResult(status: Int) {
        when (status) {
            android.content.pm.PackageInstaller.STATUS_SUCCESS -> {
                binding.apply {
                    progressBarInstall.visibility = View.GONE
                    animationView.visibility = View.GONE
                    buttonOpen.visibility = View.VISIBLE
                    buttonInstall.visibility = View.GONE
                    textViewInstallStatus.text = "Installation completed successfully"
                }
                Toast.makeText(this, "App installed successfully!", Toast.LENGTH_SHORT).show()
            }
            else -> {
                binding.apply {
                    progressBarInstall.visibility = View.GONE
                    animationView.visibility = View.GONE
                    buttonInstall.visibility = View.VISIBLE
                    textViewInstallStatus.text = "Installation failed"
                }
                Toast.makeText(this, "Installation failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun registerReceiver() {
        val filter = IntentFilter("com.apkinstaller.store.INSTALLATION_RESULT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(installationResultReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(installationResultReceiver, filter)
        }
    }
    
    private fun unregisterReceiver() {
        try {
            unregisterReceiver(installationResultReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver", e)
        }
    }
}