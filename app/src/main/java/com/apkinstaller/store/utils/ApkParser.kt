package com.apkinstaller.store.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.apkinstaller.store.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Utility class for parsing APK files and retrieving installed app information
 */
class ApkParser(private val context: Context) {

    companion object {
        private const val TAG = "ApkParser"
        private const val BUFFER_SIZE = 8192
        private const val UNKNOWN_VERSION = "Unknown"
        private const val DEFAULT_SDK = 0
    }

    /**
     * Parses an APK file and returns its information
     * @param apkPath Path to the APK file
     * @return AppInfo object or null if parsing fails
     */
    suspend fun parseApk(apkPath: String): AppInfo? = withContext(Dispatchers.IO) {
        try {
            val file = File(apkPath)
            if (!file.exists() || !file.isFile) {
                Log.e(TAG, "Invalid APK path: $apkPath")
                return@withContext null
            }

            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(
                apkPath,
                PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA
            ) ?: return@withContext null

            packageInfo.applicationInfo?.let { appInfo ->
                appInfo.sourceDir = apkPath
                appInfo.publicSourceDir = apkPath

                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val packageName = packageInfo.packageName
                val version = packageInfo.versionName ?: UNKNOWN_VERSION
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }

                val icon: Drawable? = try {
                    packageManager.getApplicationIcon(appInfo)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get app icon for $apkPath", e)
                    null
                }

                val size = file.length()
                val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

                val minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appInfo.minSdkVersion
                } else {
                    DEFAULT_SDK
                }

                val targetSdkVersion = appInfo.targetSdkVersion

                val isInstalled = isAppInstalled(packageName)

                val signature = try {
                    generateApkSignature(apkPath)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to generate APK signature for $apkPath", e)
                    null
                }

                AppInfo(
                    name = appName,
                    packageName = packageName,
                    version = version,
                    versionCode = versionCode,
                    size = size,
                    description = generateDescription(appInfo),
                    icon = icon,
                    apkPath = apkPath,
                    isInstalled = isInstalled,
                    permissions = permissions,
                    minSdkVersion = minSdkVersion,
                    targetSdkVersion = targetSdkVersion,
                    signature = signature
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse APK: $apkPath", e)
            null
        }
    }

    /**
     * Checks if an app is installed on the device
     * @param packageName Package name to check
     * @return true if installed, false otherwise
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking installed app: $packageName", e)
            false
        }
    }

    /**
     * Retrieves information about an installed app
     * @param packageName Package name of the installed app
     * @return AppInfo object or null if not found or error occurs
     */
    fun getInstalledAppInfo(packageName: String): AppInfo? {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA
            ) ?: return null

            val appInfo = packageInfo.applicationInfo ?: return null
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val version = packageInfo.versionName ?: UNKNOWN_VERSION
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            val icon = try {
                packageManager.getApplicationIcon(appInfo)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get icon for $packageName", e)
                null
            }

            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

            val minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appInfo.minSdkVersion
            } else {
                DEFAULT_SDK
            }

            val installTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                packageInfo.firstInstallTime
            } else {
                0L
            }

            val lastUpdateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                packageInfo.lastUpdateTime
            } else {
                0L
            }

            AppInfo(
                name = appName,
                packageName = packageName,
                version = version,
                versionCode = versionCode,
                size = File(appInfo.sourceDir).length(),
                description = generateDescription(appInfo),
                icon = icon,
                apkPath = appInfo.sourceDir,
                isInstalled = true,
                permissions = permissions,
                minSdkVersion = minSdkVersion,
                targetSdkVersion = appInfo.targetSdkVersion,
                installTime = installTime,
                lastUpdateTime = lastUpdateTime,
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get installed app info: $packageName", e)
            null
        }
    }

    /**
     * Generates a description string based on app characteristics
     * @param appInfo ApplicationInfo object
     * @return Formatted description string
     */
    private fun generateDescription(appInfo: ApplicationInfo): String {
        val features = mutableListOf<String>()

        if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
            features.add("System app")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appInfo.category != ApplicationInfo.CATEGORY_UNDEFINED) {
                val categoryName = when (appInfo.category) {
                    ApplicationInfo.CATEGORY_GAME -> "Game"
                    ApplicationInfo.CATEGORY_AUDIO -> "Audio"
                    ApplicationInfo.CATEGORY_VIDEO -> "Video"
                    ApplicationInfo.CATEGORY_IMAGE -> "Image"
                    ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                    ApplicationInfo.CATEGORY_NEWS -> "News"
                    ApplicationInfo.CATEGORY_MAPS -> "Maps"
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                    else -> "App"
                }
                features.add(categoryName)
            }
        }

        return if (features.isNotEmpty()) {
            features.joinToString(" • ")
        } else {
            "Android application"
        }
    }

    /**
     * Generates SHA-256 signature of an APK file
     * @param apkPath Path to the APK file
     * @return Hex string of signature or empty string on error
     */
    private suspend fun generateApkSignature(apkPath: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(apkPath)
            if (!file.exists()) return@withContext ""

            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(BUFFER_SIZE)

            file.inputStream().use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: IOException) {
            Log.w(TAG, "IO error while generating APK signature for $apkPath", e)
            ""
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate APK signature for $apkPath", e)
            ""
        }
    }

    /**
     * Returns the current device's SDK version
     * @return SDK version integer
     */
    fun getCurrentDeviceSdk(): Int {
        return Build.VERSION.SDK_INT
    }
}