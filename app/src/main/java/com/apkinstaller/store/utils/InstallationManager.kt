package com.apkinstaller.store.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.apkinstaller.store.model.InstallationProgress
import com.apkinstaller.store.model.InstallationResult
import com.apkinstaller.store.model.InstallationState
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream

class InstallationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "InstallationManager"
        private const val INSTALLATION_SESSION_NAME = "APK_INSTALLER_SESSION"
    }
    
    private val _installationProgress = MutableLiveData<InstallationProgress>()
    val installationProgress: LiveData<InstallationProgress> = _installationProgress
    
    private val _installationResult = MutableLiveData<InstallationResult>()
    val installationResult: LiveData<InstallationResult> = _installationResult
    
    private val apkSigner = ApkSigner(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun installApk(apkPath: String, packageName: String) {
        scope.launch {
            try {
                Log.d(TAG, "Starting installation for package: $packageName")
                
                updateProgress(InstallationState.PREPARING, 0, "Preparing installation...", packageName)
                delay(500)
                
                // Step 1: Verify APK file
                val apkFile = File(apkPath)
                if (!apkFile.exists() || !apkFile.canRead()) {
                    throw IllegalArgumentException("APK file not found or not readable: $apkPath")
                }
                
                updateProgress(InstallationState.SIGNING, 25, "Signing APK...", packageName)
                
                // Step 2: Sign the APK
                val signedApkPath = "${context.cacheDir}/signed_${System.currentTimeMillis()}_${apkFile.name}"
                val signingSuccess = apkSigner.signApk(apkPath, signedApkPath)
                
                if (!signingSuccess) {
                    throw Exception("Failed to sign APK")
                }
                
                updateProgress(InstallationState.INSTALLING, 50, "Installing APK...", packageName)
                delay(500)
                
                // Step 3: Install the signed APK
                val installSuccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    installWithPackageInstaller(signedApkPath, packageName)
                } else {
                    installWithIntent(signedApkPath, packageName)
                }
                
                if (installSuccess) {
                    updateProgress(InstallationState.INSTALLED, 100, "Installation completed successfully", packageName)
                    _installationResult.postValue(
                        InstallationResult(true, packageName, "Installation completed successfully")
                    )
                } else {
                    throw Exception("Installation failed")
                }
                
                // Clean up temporary signed APK
                try {
                    File(signedApkPath).delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete temporary signed APK", e)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Installation failed for package: $packageName", e)
                updateProgress(InstallationState.FAILED, 0, "Installation failed", packageName, e.message)
                _installationResult.postValue(
                    InstallationResult(false, packageName, e.message ?: "Unknown error")
                )
            }
        }
    }
    
    private suspend fun installWithPackageInstaller(apkPath: String, packageName: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setAppPackageName(packageName)
            
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            
            val apkFile = File(apkPath)
            val inputStream = FileInputStream(apkFile)
            val outputStream = session.openWrite(INSTALLATION_SESSION_NAME, 0, apkFile.length())
            
            try {
                inputStream.use { input ->
                    outputStream.use { output ->
                        val buffer = ByteArray(8192)
                        var totalBytes = 0L
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } > 0) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                            
                            val progress = ((totalBytes * 100) / apkFile.length()).toInt()
                            val adjustedProgress = 50 + (progress * 40 / 100) // Map to 50-90%
                            updateProgress(InstallationState.INSTALLING, adjustedProgress, "Installing APK...", packageName)
                        }
                    }
                }
                
                session.fsync(outputStream)
                
                val intent = Intent(context, InstallationReceiver::class.java).apply {
                    putExtra("package_name", packageName)
                }
                
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context, 
                    sessionId, 
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        android.app.PendingIntent.FLAG_IMMUTABLE
                    } else {
                        0
                    }
                )
                
                session.commit(pendingIntent.intentSender)
                
                updateProgress(InstallationState.INSTALLING, 90, "Finalizing installation...", packageName)
                
                true
            } finally {
                session.close()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error installing with PackageInstaller", e)
            false
        }
    }
    
    private suspend fun installWithIntent(apkPath: String, packageName: String): Boolean = withContext(Dispatchers.Main) {
        return@withContext try {
            val apkFile = File(apkPath)
            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            } else {
                Uri.fromFile(apkFile)
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra("package_name", packageName)
            }
            
            context.startActivity(intent)
            
            // For intent-based installation, we assume success
            // The actual result will be handled by broadcast receivers
            delay(1000)
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error installing with Intent", e)
            false
        }
    }
    
    fun openApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } else {
                Log.w(TAG, "No launch intent found for package: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app: $packageName", e)
            false
        }
    }
    
    fun uninstallApp(packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error uninstalling app: $packageName", e)
            false
        }
    }
    
    fun cancelInstallation(packageName: String) {
        scope.coroutineContext.cancelChildren()
        updateProgress(InstallationState.CANCELLED, 0, "Installation cancelled", packageName)
    }
    
    private fun updateProgress(
        state: InstallationState, 
        progress: Int, 
        message: String, 
        packageName: String, 
        error: String? = null
    ) {
        val installationProgress = InstallationProgress(
            state = state,
            progress = progress,
            message = message,
            error = error,
            packageName = packageName
        )
        _installationProgress.postValue(installationProgress)
        Log.d(TAG, "Installation progress: $state - $progress% - $message")
    }
    
    fun cleanup() {
        scope.cancel()
    }
}

class InstallationReceiver : android.content.BroadcastReceiver() {
    
    companion object {
        private const val TAG = "InstallationReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val packageName = intent.getStringExtra("package_name") ?: ""
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""
        
        Log.d(TAG, "Installation result: status=$status, package=$packageName, message=$message")
        
        val resultIntent = Intent("com.apkinstaller.store.INSTALLATION_RESULT").apply {
            putExtra("status", status)
            putExtra("package_name", packageName)
            putExtra("message", message)
        }
        
        context.sendBroadcast(resultIntent)
        
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "Installation successful for package: $packageName")
            }
            PackageInstaller.STATUS_FAILURE -> {
                Log.e(TAG, "Installation failed for package: $packageName - $message")
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                Log.w(TAG, "Installation aborted for package: $packageName")
            }
            PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                Log.w(TAG, "Installation blocked for package: $packageName")
            }
            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                Log.w(TAG, "Installation conflict for package: $packageName")
            }
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                Log.w(TAG, "Installation incompatible for package: $packageName")
            }
            PackageInstaller.STATUS_FAILURE_INVALID -> {
                Log.w(TAG, "Installation invalid for package: $packageName")
            }
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                Log.w(TAG, "Installation storage error for package: $packageName")
            }
        }
    }
}