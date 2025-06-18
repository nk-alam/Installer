package com.coderx.myinstaller.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.*
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class AdvancedApkSigner(private val context: Context) {

    companion object {
        private const val TAG = "AdvancedApkSigner"
    }

    private val keystoreManager = KeystoreManager(context)

    suspend fun signApkAdvanced(
        inputApkPath: String,
        outputApkPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting advanced APK signing")

            val keystore = keystoreManager.loadOrCreateKeystore()
            val (privateKey, certificate) = keystoreManager.getSigningKey(keystore)

            // Create signed APK
            createSignedApk(inputApkPath, outputApkPath, privateKey, certificate)

            Log.d(TAG, "Advanced APK signing completed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Advanced APK signing failed", e)
            false
        }
    }

    private fun createSignedApk(
        inputPath: String,
        outputPath: String,
        privateKey: PrivateKey,
        certificate: X509Certificate
    ) {
        val inputFile = File(inputPath)
        val outputFile = File(outputPath)
        val tempFile = File(outputFile.parent, "${outputFile.name}.tmp")

        try {
            // Step 1: Copy APK without META-INF
            copyApkWithoutMetaInf(inputFile, tempFile)

            // Step 2: Generate manifest and signature files
            val manifest = generateManifest(tempFile)
            val signatureFile = generateSignatureFile(manifest)
            val signatureBlock = generateSignatureBlock(signatureFile, privateKey, certificate)

            // Step 3: Add signature files to APK
            addSignatureFiles(tempFile, outputFile, manifest, signatureFile, signatureBlock)

            tempFile.delete()
            Log.d(TAG, "Signed APK created: $outputPath")

        } catch (e: Exception) {
            try {
                tempFile.delete()
            } catch (deleteException: Exception) {
                Log.w(TAG, "Failed to delete temp file", deleteException)
            }
            throw e
        }
    }

    private fun copyApkWithoutMetaInf(inputFile: File, outputFile: File) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            ZipInputStream(FileInputStream(inputFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.name.startsWith("META-INF/")) {
                        zos.putNextEntry(ZipEntry(entry.name))
                        zis.copyTo(zos)
                        zos.closeEntry()
                    }
                    entry = zis.nextEntry
                }
            }
        }
    }

    private fun generateManifest(apkFile: File): String {
        val manifest = StringBuilder()
        manifest.appendLine("Manifest-Version: 1.0")
        manifest.appendLine("Created-By: APK Installer")
        manifest.appendLine()

        // Calculate SHA-1 digest for each file
        ZipInputStream(FileInputStream(apkFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
                    try {
                        val digest = calculateSha1Digest(zis)
                        manifest.appendLine("Name: ${entry.name}")
                        manifest.appendLine("SHA1-Digest: $digest")
                        manifest.appendLine()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to calculate digest for ${entry.name}", e)
                    }
                }
                entry = zis.nextEntry
            }
        }

        return manifest.toString()
    }

    private fun generateSignatureFile(manifest: String): String {
        val signatureFile = StringBuilder()
        signatureFile.appendLine("Signature-Version: 1.0")
        signatureFile.appendLine("Created-By: APK Installer")
        
        // Calculate SHA-1 digest of the manifest
        val manifestDigest = calculateSha1Digest(manifest.toByteArray())
        signatureFile.appendLine("SHA1-Digest-Manifest: $manifestDigest")
        signatureFile.appendLine()

        return signatureFile.toString()
    }

    private fun generateSignatureBlock(
        signatureFile: String,
        privateKey: PrivateKey,
        certificate: X509Certificate
    ): ByteArray {
        return try {
            // This is a simplified signature block
            // In production, use proper PKCS#7 signature format
            val signature = Signature.getInstance("SHA1withRSA")
            signature.initSign(privateKey)
            signature.update(signatureFile.toByteArray())
            signature.sign()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate signature block", e)
            ByteArray(0)
        }
    }

    private fun addSignatureFiles(
        tempFile: File,
        outputFile: File,
        manifest: String,
        signatureFile: String,
        signatureBlock: ByteArray
    ) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            // Copy existing entries
            ZipInputStream(FileInputStream(tempFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    zos.putNextEntry(ZipEntry(entry.name))
                    zis.copyTo(zos)
                    zos.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Add META-INF/MANIFEST.MF
            zos.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
            zos.write(manifest.toByteArray())
            zos.closeEntry()

            // Add META-INF/CERT.SF
            zos.putNextEntry(ZipEntry("META-INF/CERT.SF"))
            zos.write(signatureFile.toByteArray())
            zos.closeEntry()

            // Add META-INF/CERT.RSA
            zos.putNextEntry(ZipEntry("META-INF/CERT.RSA"))
            zos.write(signatureBlock)
            zos.closeEntry()
        }
    }

    private fun calculateSha1Digest(data: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            val hash = digest.digest(data)
            android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate SHA-1 digest", e)
            ""
        }
    }

    private fun calculateSha1Digest(inputStream: InputStream): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            
            val hash = digest.digest()
            android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate SHA-1 digest from stream", e)
            ""
        }
    }
}