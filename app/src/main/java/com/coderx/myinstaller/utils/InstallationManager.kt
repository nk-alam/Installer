package com.coderx.myinstaller.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.coderx.myinstaller.R
import com.coderx.myinstaller.data.AppInfo
import com.coderx.myinstaller.data.InstallationState
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class InstallationManager(private val context: Context) {

    companion object {
        private const val TAG = "InstallationManager"
        private const val INSTALL_REQUEST_CODE = 1001
    }

    private val _installationProgress = MutableLiveData<Int>()
    val installationProgress: LiveData<Int> = _installationProgress

    private val _installationState = MutableLiveData<InstallationState>()
    val installationState: LiveData<InstallationState> = _installationState

    private val _currentApp = MutableLiveData<AppInfo?>()
    val currentApp: LiveData<AppInfo?> = _currentApp

    private val apkSigner = ApkSigner(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val notificationHelper = NotificationHelper(context)

    fun getInstalledApps(): List<AppInfo> {
        // Return list of bundled apps that can be installed
        return listOf(
            AppInfo(
                packageName = "com.example.sampleapp",
                appName = "Sample App",
                versionName = "1.0.0",
                versionCode = 1,
                iconResId = R.drawable.ic_app_default,
                apkAssetPath = "sample_app.apk",
                description = "A sample application for testing the installer functionality. This app demonstrates basic Android features.",
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
                description = "Advanced demo application with camera, storage, and location features for comprehensive testing.",
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
                // Step 1: Extract APK from assets (20%)
                updateProgress(10, "Extracting APK...")
                val extractedApk = extractApkFromAssets(appInfo.apkAssetPath)

                // Step 2: Validate APK (40%)
                updateProgress(30, "Validating APK...")
                if (!validateApk(extractedApk)) {
                    throw RuntimeException("APK validation failed")
                }

                // Step 3: Sign APK (70%)
                updateProgress(50, "Signing APK...")
                val signedApk = signApk(extractedApk, appInfo.packageName)

                // Step 4: Prepare installation (90%)
                updateProgress(80, "Preparing installation...")
                
                // Step 5: Install APK (100%)
                updateProgress(95, "Installing...")
                installApkFile(signedApk)

                updateProgress(100, "Installation complete!")
                
                // Show completion notification
                notificationHelper.showInstallationComplete(appInfo.appName, true)

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
    }

    private suspend fun extractApkFromAssets(assetPath: String): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.getExternalFilesDir(null), "apks")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val outputFile = File(outputDir, "temp_${System.currentTimeMillis()}.apk")

        try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L
                    val availableBytes = input.available().toLong()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead

                        // Update progress (0-20%)
                        if (availableBytes > 0) {
                            val progress = ((totalBytes * 20) / availableBytes).toInt()
                            _installationProgress.postValue(minOf(progress, 20))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract APK from assets", e)
            throw RuntimeException("Failed to extract APK: ${e.message}")
        }

        outputFile
    }

    private suspend fun signApk(inputApk: File, packageName: String): File = withContext(Dispatchers.IO) {
        val signedApkFile = File(inputApk.parent, "signed_${packageName}_${System.currentTimeMillis()}.apk")

        val success = apkSigner.signApk(
            inputApk.absolutePath,
            signedApkFile.absolutePath,
            "keystore.p12"
        )

        if (!success) {
            throw RuntimeException("APK signing failed")
        }

        // Clean up original file
        inputApk.delete()

        signedApkFile
    }

    private suspend fun installApkFile(apkFile: File) = withContext(Dispatchers.Main) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                
                // For Android 14+ (API 34+), add additional flags
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
                }
            }

            // Check if there's an activity that can handle this intent
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

    suspend fun validateApk(apkFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                Log.e(TAG, "APK file does not exist or is empty")
                return@withContext false
            }

            // Try to get package info from the APK
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_ACTIVITIES or PackageManager.GET_PERMISSIONS
            )
            
            if (packageInfo == null) {
                Log.e(TAG, "Could not parse APK file")
                return@withContext false
            }

            Log.d(TAG, "APK validation successful: ${packageInfo.packageName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "APK validation failed", e)
            false
        }
    }

    fun getAppPermissions(appInfo: AppInfo): List<String> {
        return appInfo.permissions
    }

    suspend fun checkApkIntegrity(apkFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            apkFile.exists() && apkFile.length() > 1000 // At least 1KB
        } catch (e: Exception) {
            false
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

    fun onInstallationResult(success: Boolean) {
        _installationState.value = if (success) {
            InstallationState.INSTALLED
        } else {
            InstallationState.FAILED
        }
        
        if (success) {
            notificationHelper.cancelInstallationNotification()
        }
    }

    fun cleanup() {
        scope.cancel()
        
        // Clean up temporary files
        scope.launch {
            try {
                val apkDir = File(context.getExternalFilesDir(null), "apks")
                if (apkDir.exists()) {
                    apkDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("temp_") || file.name.startsWith("signed_")) {
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