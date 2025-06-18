package com.coderx.myinstaller.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.io.ByteArrayInputStream

class SecurityUtils(private val context: Context) {

    companion object {
        private const val TAG = "SecurityUtils"
    }

    fun verifyApkSignature(packageName: String): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )

            packageInfo.signatures?.isNotEmpty() == true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify APK signature", e)
            false
        }
    }

    fun getApkSignatureHash(packageName: String): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )

            packageInfo.signatures?.firstOrNull()?.let { signature ->
                val cert = CertificateFactory.getInstance("X.509")
                    .generateCertificate(ByteArrayInputStream(signature.toByteArray())) as X509Certificate

                val md = MessageDigest.getInstance("SHA-1")
                val digest = md.digest(cert.encoded)

                digest.joinToString(":") { "%02X".format(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get signature hash", e)
            null
        }
    }

    fun isSystemApp(packageName: String): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.applicationInfo?.let { appInfo ->
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            } ?: false
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found: $packageName", e)
            false
        }
    }
}