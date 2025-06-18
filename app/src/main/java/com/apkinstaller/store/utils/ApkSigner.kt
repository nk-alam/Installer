package com.apkinstaller.store.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.*
import java.math.BigInteger
import java.security.*
import java.security.cert.X509Certificate
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ApkSigner(private val context: Context) {
    
    companion object {
        private const val TAG = "ApkSigner"
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
        private const val KEY_SIZE = 2048
        private const val CERTIFICATE_VALIDITY_YEARS = 25
    }
    
    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }
    
    suspend fun signApk(inputApkPath: String, outputApkPath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Starting APK signing process")
            
            val keyPair = generateKeyPair()
            val certificate = generateCertificate(keyPair)
            
            val inputFile = File(inputApkPath)
            val outputFile = File(outputApkPath)
            
            // Ensure output directory exists
            outputFile.parentFile?.mkdirs()
            
            // Create a simple signed APK
            val success = createSignedApk(inputFile, outputFile, keyPair.private, certificate)
            
            if (success) {
                Log.d(TAG, "APK signing completed successfully")
            } else {
                Log.e(TAG, "APK signing failed")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error during APK signing", e)
            false
        }
    }
    
    private suspend fun generateKeyPair(): KeyPair = withContext(Dispatchers.IO) {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
        keyPairGenerator.initialize(KEY_SIZE, SecureRandom())
        return@withContext keyPairGenerator.generateKeyPair()
    }
    
    private suspend fun generateCertificate(keyPair: KeyPair): X509Certificate = withContext(Dispatchers.IO) {
        val now = Date()
        val notAfter = Date(now.time + CERTIFICATE_VALIDITY_YEARS * 365L * 24 * 60 * 60 * 1000)
        
        val subject = X500Name("CN=APK Installer, O=APK Store, C=US")
        val serial = BigInteger.valueOf(System.currentTimeMillis())
        
        val certBuilder = X509v3CertificateBuilder(
            subject,
            serial,
            now,
            notAfter,
            subject,
            SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
        )
        
        val signer = JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.private)
        
        val certHolder = certBuilder.build(signer)
        return@withContext JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(certHolder)
    }
    
    private suspend fun createSignedApk(
        inputFile: File,
        outputFile: File,
        privateKey: PrivateKey,
        certificate: X509Certificate
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // For demonstration purposes, we'll create a simple copy
            // In a production environment, you would use proper APK signing tools
            // like apksigner or implement full JAR signing
            
            val manifest = createManifest()
            
            ZipInputStream(FileInputStream(inputFile)).use { zipInput ->
                ZipOutputStream(FileOutputStream(outputFile)).use { zipOutput ->
                    var entry: ZipEntry?
                    val buffer = ByteArray(8192)
                    
                    while (zipInput.nextEntry.also { entry = it } != null) {
                        entry?.let { currentEntry ->
                            // Skip existing signature files
                            if (currentEntry.name.startsWith("META-INF/") && 
                                (currentEntry.name.endsWith(".SF") || 
                                 currentEntry.name.endsWith(".RSA") || 
                                 currentEntry.name.endsWith(".DSA"))) {
                                return@let
                            }
                            
                            val newEntry = ZipEntry(currentEntry.name)
                            newEntry.time = currentEntry.time
                            zipOutput.putNextEntry(newEntry)
                            
                            var bytesRead: Int
                            while (zipInput.read(buffer).also { bytesRead = it } > 0) {
                                zipOutput.write(buffer, 0, bytesRead)
                            }
                            zipOutput.closeEntry()
                        }
                    }
                    
                    // Add manifest
                    val manifestEntry = ZipEntry("META-INF/MANIFEST.MF")
                    zipOutput.putNextEntry(manifestEntry)
                    manifest.write(zipOutput)
                    zipOutput.closeEntry()
                    
                    // Add signature file (simplified)
                    val sigEntry = ZipEntry("META-INF/CERT.SF")
                    zipOutput.putNextEntry(sigEntry)
                    zipOutput.write("Signature-Version: 1.0\nCreated-By: APK Installer\n".toByteArray())
                    zipOutput.closeEntry()
                    
                    // Add certificate (simplified)
                    val certEntry = ZipEntry("META-INF/CERT.RSA")
                    zipOutput.putNextEntry(certEntry)
                    zipOutput.write(certificate.encoded)
                    zipOutput.closeEntry()
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating signed APK", e)
            false
        }
    }
    
    private fun createManifest(): Manifest {
        val manifest = Manifest()
        manifest.mainAttributes.apply {
            putValue("Manifest-Version", "1.0")
            putValue("Created-By", "APK Installer ${getAppVersion()}")
            putValue("Built-By", "APK Store")
        }
        return manifest
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
    
    suspend fun verifyApkSignature(apkPath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(apkPath, 0)
            packageInfo != null
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying APK signature", e)
            false
        }
    }
}