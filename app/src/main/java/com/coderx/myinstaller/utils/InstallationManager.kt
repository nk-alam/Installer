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
import com.coderx.myinstaller.data.AppInfo
import com.coderx.myinstaller.data.InstallationState
import com.coderx.myinstaller.utils.ApkSigner
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

    fun getInstalledApps(): List<AppInfo> {
        // Return list of bundled apps that can be installed
        return listOf(
            AppInfo(
                packageName = "com.example.sampleapp",
                appName = "Sample App",
                versionName = "1.0.0",
                versionCode = 1,
                iconResId = android.R.drawable.ic_dialog_info,
                apkAssetPath = "sample_app.apk",
                description = "A sample application for testing the installer",
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
                iconResId = android.R.drawable.ic_dialog_alert,
                apkAssetPath = "demo_app.apk",
                description = "Demo application with advanced features",
                size = 5120000L,
                permissions = listOf(
                    "android.permission.CAMERA",
                    "android.permission.WRITE_EXTERNAL_STORAGE",
                    "android.permission.ACCESS_FINE_LOCATION"
                )
            )
        )
    }

    fun checkInstallationState(appInfo: AppInfo): InstallationState {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(appInfo.packageName, 0)
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
                // Step 1: Extract APK from assets (10%)
                _installationProgress.postValue(10)
                val extractedApk = extractApkFromAssets(appInfo.apkAssetPath)

                // Step 2: Sign APK (50%)
                _installationProgress.postValue(50)
                val signedApk = signApk(extractedApk, appInfo.packageName)

                // Step 3: Install APK (90%)
                _installationProgress.postValue(90)
                installApkFile(signedApk)

                // Step 4: Complete (100%)
                _installationProgress.postValue(100)

            } catch (e: Exception) {
                Log.e(TAG, "Installation failed", e)
                _installationState.postValue(InstallationState.FAILED)
            }
        }
    }

    private suspend fun extractApkFromAssets(assetPath: String): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.getExternalFilesDir(null), "apks")
        outputDir.mkdirs()

        val outputFile = File(outputDir, "temp_${System.currentTimeMillis()}.apk")

        context.assets.open(assetPath).use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytes = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead

                    // Update progress (0-10%)
                    val progress = (totalBytes * 10 / (totalBytes + 1000000)).toInt()
                    _installationProgress.postValue(progress)
                }
            }
        }

        outputFile
    }

    private suspend fun signApk(inputApk: File, packageName: String): File = withContext(Dispatchers.IO) {
        val signedApkFile = File(inputApk.parent, "signed_${packageName}.apk")

        val success = apkSigner.signApk(
            inputApk.absolutePath,
            signedApkFile.absolutePath,
            "keystore.p12" // Load from assets
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
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start installation", e)
            _installationState.postValue(InstallationState.FAILED)
        }
    }

    fun createSampleApks() {
        // This method creates sample APK files in assets for testing
        // In production, you would bundle real APK files
        val sampleApkContent = createMinimalApkContent()

        // Save to assets directory (this would be done during build process)
        // For demonstration purposes only
    }

    private fun createMinimalApkContent(): ByteArray {
        // This creates a minimal APK structure for testing
        // In production, use real APK files
        return "Sample APK Content".toByteArray()
    }

    suspend fun validateApk(apkFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            // Basic APK validation
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_ACTIVITIES
            )
            packageInfo != null
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
            // Check file size and basic structure
            apkFile.exists() && apkFile.length() > 0
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
    }

    fun cleanup() {
        scope.cancel()
    }
}