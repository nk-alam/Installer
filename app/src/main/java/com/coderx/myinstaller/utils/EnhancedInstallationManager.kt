package com.coderx.myinstaller.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.coderx.myinstaller.InstallReceiver
import com.coderx.myinstaller.R
import com.coderx.myinstaller.data.AppInfo
import com.coderx.myinstaller.data.InstallationState
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class EnhancedInstallationManager(private val context: Context) {

    companion object {
        private const val TAG = "EnhancedInstallationManager"
    }

    private val _installationProgress = MutableLiveData<Int>()
    val installationProgress: LiveData<Int> = _installationProgress

    private val _installationState = MutableLiveData<InstallationState>()
    val installationState: LiveData<InstallationState> = _installationState

    private val _currentApp = MutableLiveData<AppInfo?>()
    val currentApp: LiveData<AppInfo?> = _currentApp

    private val advancedApkSigner = AdvancedApkSigner(context)
    private val apkBuilder = ApkBuilder(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val notificationHelper = NotificationHelper(context)

    private val installReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                InstallReceiver.ACTION_INSTALLATION_STATUS -> {
                    val packageName = intent.getStringExtra(InstallReceiver.EXTRA_PACKAGE_NAME)
                    val success = intent.getBooleanExtra(InstallReceiver.EXTRA_SUCCESS, false)
                    
                    if (packageName == _currentApp.value?.packageName) {
                        onInstallationResult(success)
                    }
                }
            }
        }
    }

    init {
        // Register for installation status updates
        val filter = IntentFilter(InstallReceiver.ACTION_INSTALLATION_STATUS)
        context.registerReceiver(installReceiver, filter)
    }

    fun getAvailableApps(): List<AppInfo> {
        return listOf(
            AppInfo(
                packageName = "com.example.sampleapp",
                appName = "Sample App",
                versionName = "1.0.0",
                versionCode = 1,
                iconResId = R.drawable.ic_app_default,
                apkAssetPath = "sample_app.apk",
                description = "A sample application demonstrating basic Android functionality with modern UI components and smooth animations.",
                size = 2048000L,
                permissions = listOf(
                    "android.permission.INTERNET",
                    "android.permission.ACCESS_NETWORK_STATE"
                )
            ),
            AppInfo(
                packageName = "com.example.demoapp",
                appName = "Demo App",
                versionName = "2.1.0",
                versionCode = 21,
                iconResId = R.drawable.ic_app_default,
                apkAssetPath = "demo_app.apk",
                description = "Advanced demo application featuring camera integration, location services, and file management capabilities.",
                size = 5120000L,
                permissions = listOf(
                    "android.permission.CAMERA",
                    "android.permission.WRITE_EXTERNAL_STORAGE",
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.RECORD_AUDIO"
                )
            )
        )
    }

    fun checkInstallationState(appInfo: AppInfo): InstallationState {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    appInfo.packageName, 
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(appInfo.packageName, 0)
            }
            
            when {
                packageInfo.longVersionCode < appInfo.versionCode -> InstallationState.UPDATE_AVAILABLE
                else -> InstallationState.INSTALLED
            }
        } catch (e: PackageManager.NameNotFoundException) {
            InstallationState.NOT_INSTALLED
        }
    }

    fun installApp(appInfo: AppInfo) {
        _currentApp.value = appInfo
        _installationState.value = InstallationState.INSTALLING
        _installationProgress.value = 0

        scope.launch {
            try {
                // Step 1: Build APK dynamically (30%)
                updateProgress(10, "Building APK...")
                val builtApk = buildApkDynamically(appInfo)

                // Step 2: Sign APK with advanced signing (60%)
                updateProgress(40, "Signing APK...")
                val signedApk = signApkAdvanced(builtApk, appInfo.packageName)

                // Step 3: Validate signed APK (80%)
                updateProgress(70, "Validating APK...")
                if (!validateSignedApk(signedApk)) {
                    throw RuntimeException("APK validation failed")
                }

                // Step 4: Install APK (100%)
                updateProgress(90, "Installing...")
                installSignedApk(signedApk)

                updateProgress(100, "Installation complete!")

            } catch (e: Exception) {
                Log.e(TAG, "Installation failed", e)
                _installationState.postValue(InstallationState.FAILED)
                notificationHelper.showInstallationComplete(appInfo.appName, false)
            }
        }
    }

    private fun updateProgress(progress: Int, status: String) {
        _installationProgress.postValue(progress)
        _currentApp.value?.let { app ->
            notificationHelper.showInstallationProgress(app.appName, progress)
        }
        Log.d(TAG, "Progress: $progress% - $status")
    }

    private suspend fun buildApkDynamically(appInfo: AppInfo): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.getExternalFilesDir(null), "built_apks")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val outputFile = File(outputDir, "built_${appInfo.packageName}_${System.currentTimeMillis()}.apk")

        val success = apkBuilder.buildMinimalApk(
            appInfo.packageName,
            appInfo.appName,
            appInfo.versionName,
            appInfo.versionCode.toInt(),
            outputFile
        )

        if (!success) {
            throw RuntimeException("Failed to build APK")
        }

        outputFile
    }

    private suspend fun signApkAdvanced(inputApk: File, packageName: String): File = withContext(Dispatchers.IO) {
        val signedApkFile = File(inputApk.parent, "signed_${packageName}_${System.currentTimeMillis()}.apk")

        val success = advancedApkSigner.signApkAdvanced(
            inputApk.absolutePath,
            signedApkFile.absolutePath
        )

        if (!success) {
            throw RuntimeException("Advanced APK signing failed")
        }

        // Clean up original file
        inputApk.delete()

        signedApkFile
    }

    private suspend fun validateSignedApk(apkFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                Log.e(TAG, "Signed APK file does not exist or is empty")
                return@withContext false
            }

            // Validate APK structure
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_ACTIVITIES or PackageManager.GET_PERMISSIONS
            )
            
            if (packageInfo == null) {
                Log.e(TAG, "Could not parse signed APK file")
                return@withContext false
            }

            Log.d(TAG, "Signed APK validation successful: ${packageInfo.packageName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Signed APK validation failed", e)
            false
        }
    }

    private suspend fun installSignedApk(apkFile: File) = withContext(Dispatchers.Main) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
                }
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                throw RuntimeException("No app available to handle APK installation")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start installation", e)
            _installationState.postValue(InstallationState.FAILED)
            throw e
        }
    }

    fun launchApp(packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } else {
                Log.w(TAG, "No launch intent found for package: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app: $packageName", e)
        }
    }

    private fun onInstallationResult(success: Boolean) {
        _installationState.value = if (success) {
            InstallationState.INSTALLED
        } else {
            InstallationState.FAILED
        }
        
        _currentApp.value?.let { app ->
            notificationHelper.showInstallationComplete(app.appName, success)
        }
        
        if (success) {
            // Clean up after successful installation
            scope.launch {
                delay(2000) // Wait 2 seconds before cleanup
                notificationHelper.cancelInstallationNotification()
            }
        }
    }

    fun cleanup() {
        try {
            context.unregisterReceiver(installReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister receiver", e)
        }
        
        scope.cancel()
        
        // Clean up temporary files
        scope.launch {
            try {
                val apkDir = File(context.getExternalFilesDir(null), "built_apks")
                if (apkDir.exists()) {
                    apkDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("built_") || file.name.startsWith("signed_")) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cleanup temporary files", e)
            }
        }
    }
}