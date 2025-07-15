package com.coderx.installer.utils

import android.content.Context
import android.provider.Settings
import android.util.Log
import java.io.*
import java.security.*
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.security.auth.x500.X500Principal
import kotlin.random.Random

object DynamicApkSigner {
    private const val TAG = "DynamicApkSigner"
    
    /**
     * Signs an APK file dynamically based on device-specific information
     */
    fun signApk(context: Context, inputApkFile: File): File {
        Log.d(TAG, "Starting dynamic APK signing process")
        
        val outputApkFile = File(context.cacheDir, "signed_${System.currentTimeMillis()}.apk")
        
        try {
            // Generate device-specific certificate and key
            val (certificate, privateKey) = generateDeviceSpecificCertificate(context)
            
            // Create signed APK
            signApkWithCertificate(inputApkFile, outputApkFile, certificate, privateKey)
            
            Log.d(TAG, "APK signed successfully: ${outputApkFile.absolutePath}")
            return outputApkFile
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sign APK", e)
            throw e
        }
    }
    
    /**
     * Generates a device-specific certificate and private key
     */
    private fun generateDeviceSpecificCertificate(context: Context): Pair<X509Certificate, PrivateKey> {
        Log.d(TAG, "Generating device-specific certificate")
        
        // Get device-specific information
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val packageName = context.packageName
        val timestamp = System.currentTimeMillis()
        
        // Create device-specific seed
        val deviceSeed = "$deviceId:$packageName:$timestamp".toByteArray()
        val secureRandom = SecureRandom.getInstance("SHA1PRNG")
        secureRandom.setSeed(deviceSeed)
        
        // Generate RSA key pair
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048, secureRandom)
        val keyPair = keyPairGenerator.generateKeyPair()
        
        // Create self-signed certificate
        val certificate = createSelfSignedCertificate(keyPair, deviceId, packageName)
        
        return Pair(certificate, keyPair.private)
    }
    
    /**
     * Creates a self-signed X509 certificate
     */
    private fun createSelfSignedCertificate(
        keyPair: KeyPair,
        deviceId: String,
        packageName: String
    ): X509Certificate {
        
        // For simplicity, we'll create a basic certificate structure
        // In production, you might want to use BouncyCastle for more advanced certificate generation
        
        val certBuilder = CertificateBuilder()
        return certBuilder.createCertificate(keyPair, deviceId, packageName)
    }
    
    /**
     * Signs the APK with the provided certificate and private key
     */
    private fun signApkWithCertificate(
        inputApk: File,
        outputApk: File,
        certificate: X509Certificate,
        privateKey: PrivateKey
    ) {
        Log.d(TAG, "Signing APK with generated certificate")
        
        // Create a temporary unsigned APK
        val tempApk = File(inputApk.parent, "temp_${System.currentTimeMillis()}.apk")
        
        try {
            // Copy and prepare APK for signing
            prepareApkForSigning(inputApk, tempApk)
            
            // Sign the APK
            performApkSigning(tempApk, outputApk, certificate, privateKey)
            
        } finally {
            // Clean up temporary file
            if (tempApk.exists()) {
                tempApk.delete()
            }
        }
    }
    
    /**
     * Prepares APK for signing by removing existing signatures
     */
    private fun prepareApkForSigning(inputApk: File, outputApk: File) {
        Log.d(TAG, "Preparing APK for signing")
        
        ZipInputStream(FileInputStream(inputApk)).use { zipIn ->
            ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->
                
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    val entryName = entry!!.name
                    
                    // Skip existing signature files
                    if (entryName.startsWith("META-INF/") && 
                        (entryName.endsWith(".SF") || 
                         entryName.endsWith(".RSA") || 
                         entryName.endsWith(".DSA") ||
                         entryName == "META-INF/MANIFEST.MF")) {
                        continue
                    }
                    
                    // Copy other files
                    val newEntry = ZipEntry(entryName)
                    newEntry.time = entry!!.time
                    zipOut.putNextEntry(newEntry)
                    
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                        zipOut.write(buffer, 0, bytesRead)
                    }
                    
                    zipOut.closeEntry()
                    zipIn.closeEntry()
                }
            }
        }
    }
    
    /**
     * Performs the actual APK signing process
     */
    private fun performApkSigning(
        inputApk: File,
        outputApk: File,
        certificate: X509Certificate,
        privateKey: PrivateKey
    ) {
        Log.d(TAG, "Performing APK signing")
        
        // Create manifest and signature files
        val manifest = createManifest(inputApk)
        val signatureFile = createSignatureFile(manifest, certificate)
        val signatureBlock = createSignatureBlock(signatureFile, privateKey, certificate)
        
        // Create signed APK
        ZipInputStream(FileInputStream(inputApk)).use { zipIn ->
            ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->
                
                // Copy all original files
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    val newEntry = ZipEntry(entry!!.name)
                    newEntry.time = entry!!.time
                    zipOut.putNextEntry(newEntry)
                    
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                        zipOut.write(buffer, 0, bytesRead)
                    }
                    
                    zipOut.closeEntry()
                    zipIn.closeEntry()
                }
                
                // Add signature files
                addSignatureFiles(zipOut, manifest, signatureFile, signatureBlock)
            }
        }
    }
    
    /**
     * Creates manifest file for signing
     */
    private fun createManifest(apkFile: File): ByteArray {
        val manifest = StringBuilder()
        manifest.appendLine("Manifest-Version: 1.0")
        manifest.appendLine("Created-By: DynamicApkSigner")
        manifest.appendLine()
        
        // Add entries for each file in the APK
        ZipInputStream(FileInputStream(apkFile)).use { zipIn ->
            var entry: ZipEntry?
            while (zipIn.nextEntry.also { entry = it } != null) {
                if (!entry!!.isDirectory && !entry!!.name.startsWith("META-INF/")) {
                    val digest = calculateSHA256Digest(zipIn)
                    manifest.appendLine("Name: ${entry!!.name}")
                    manifest.appendLine("SHA-256-Digest: $digest")
                    manifest.appendLine()
                }
                zipIn.closeEntry()
            }
        }
        
        return manifest.toString().toByteArray()
    }
    
    /**
     * Creates signature file
     */
    private fun createSignatureFile(manifest: ByteArray, certificate: X509Certificate): ByteArray {
        val signatureFile = StringBuilder()
        signatureFile.appendLine("Signature-Version: 1.0")
        signatureFile.appendLine("Created-By: DynamicApkSigner")
        signatureFile.appendLine("SHA-256-Digest-Manifest: ${calculateSHA256Digest(manifest)}")
        signatureFile.appendLine()
        
        return signatureFile.toString().toByteArray()
    }
    
    /**
     * Creates signature block
     */
    private fun createSignatureBlock(
        signatureFile: ByteArray,
        privateKey: PrivateKey,
        certificate: X509Certificate
    ): ByteArray {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(signatureFile)
        
        return signature.sign()
    }
    
    /**
     * Adds signature files to the APK
     */
    private fun addSignatureFiles(
        zipOut: ZipOutputStream,
        manifest: ByteArray,
        signatureFile: ByteArray,
        signatureBlock: ByteArray
    ) {
        // Add MANIFEST.MF
        zipOut.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
        zipOut.write(manifest)
        zipOut.closeEntry()
        
        // Add CERT.SF
        zipOut.putNextEntry(ZipEntry("META-INF/CERT.SF"))
        zipOut.write(signatureFile)
        zipOut.closeEntry()
        
        // Add CERT.RSA
        zipOut.putNextEntry(ZipEntry("META-INF/CERT.RSA"))
        zipOut.write(signatureBlock)
        zipOut.closeEntry()
    }
    
    /**
     * Calculates SHA-256 digest
     */
    private fun calculateSHA256Digest(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }
    
    /**
     * Calculates SHA-256 digest from InputStream
     */
    private fun calculateSHA256Digest(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        
        val hash = digest.digest()
        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }
}

/**
 * Helper class for certificate creation
 */
private class CertificateBuilder {
    
    fun createCertificate(keyPair: KeyPair, deviceId: String, packageName: String): X509Certificate {
        // This is a simplified certificate creation
        // In production, use BouncyCastle for proper X.509 certificate generation
        
        return SimpleCertificate(keyPair.public, deviceId, packageName)
    }
}

/**
 * Simplified X509Certificate implementation for demonstration
 * In production, use proper certificate libraries like BouncyCastle
 */
private class SimpleCertificate(
    private val publicKey: PublicKey,
    private val deviceId: String,
    private val packageName: String
) : X509Certificate() {
    
    override fun getPublicKey(): PublicKey = publicKey
    
    override fun getSubjectDN(): Principal = X500Principal("CN=$deviceId, O=$packageName")
    
    override fun getIssuerDN(): Principal = subjectDN
    
    override fun getSerialNumber(): java.math.BigInteger = 
        java.math.BigInteger.valueOf(System.currentTimeMillis())
    
    override fun getNotBefore(): java.util.Date = java.util.Date()
    
    override fun getNotAfter(): java.util.Date = 
        java.util.Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000) // 1 year
    
    override fun getSigAlgName(): String = "SHA256withRSA"
    
    override fun getSigAlgOID(): String = "1.2.840.113549.1.1.11"
    
    override fun getSigAlgParams(): ByteArray? = null
    
    override fun getIssuerUniqueID(): BooleanArray? = null
    
    override fun getSubjectUniqueID(): BooleanArray? = null
    
    override fun getKeyUsage(): BooleanArray? = null
    
    override fun getBasicConstraints(): Int = -1
    
    override fun getEncoded(): ByteArray = ByteArray(0) // Simplified
    
    override fun verify(key: PublicKey?) {}
    
    override fun verify(key: PublicKey?, sigProvider: String?) {}
    
    override fun toString(): String = "SimpleCertificate[subject=$subjectDN]"
    
    override fun hasUnsupportedCriticalExtension(): Boolean = false
    
    override fun getCriticalExtensionOIDs(): Set<String>? = null
    
    override fun getNonCriticalExtensionOIDs(): Set<String>? = null
    
    override fun getExtensionValue(oid: String?): ByteArray? = null
    
    override fun checkValidity() {}
    
    override fun checkValidity(date: java.util.Date?) {}
    
    override fun getVersion(): Int = 3
    
    override fun getSignature(): ByteArray = ByteArray(0)
    
    override fun getTBSCertificate(): ByteArray = ByteArray(0)
}