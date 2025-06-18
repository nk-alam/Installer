package com.coderx.myinstaller.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import java.io.*
import java.security.*
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher

class ApkSigner(private val context: Context) {

    companion object {
        private const val TAG = "ApkSigner"

        init {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    suspend fun signApk(
        inputApkPath: String,
        outputApkPath: String,
        keystorePath: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting APK signing process")

            // Load keystore from assets or create a temporary one
            val keyStore = loadOrCreateKeyStore(keystorePath)

            // Get private key and certificate
            val alias = keyStore.aliases().nextElement()
            val privateKey = keyStore.getKey(alias, "password".toCharArray()) as PrivateKey
            val certificate = keyStore.getCertificate(alias) as X509Certificate

            // Sign the APK
            signApkWithKey(inputApkPath, outputApkPath, privateKey, certificate)

            Log.d(TAG, "APK signing completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "APK signing failed", e)
            false
        }
    }

    private fun loadOrCreateKeyStore(keystorePath: String?): KeyStore {
        return try {
            if (keystorePath != null) {
                // Load from assets
                val inputStream = context.assets.open(keystorePath)
                val keyStore = KeyStore.getInstance("PKCS12")
                keyStore.load(inputStream, "password".toCharArray())
                inputStream.close()
                keyStore
            } else {
                // Create temporary keystore
                createTemporaryKeyStore()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load keystore, creating temporary one", e)
            createTemporaryKeyStore()
        }
    }

    private fun createTemporaryKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, null)

        // Generate key pair
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        // Create self-signed certificate (simplified)
        val certificate = createSelfSignedCertificate(keyPair)

        // Store in keystore
        keyStore.setKeyEntry(
            "apk_signer",
            keyPair.private,
            "password".toCharArray(),
            arrayOf(certificate)
        )

        return keyStore
    }

    private fun createSelfSignedCertificate(keyPair: KeyPair): X509Certificate {
        // This is a simplified self-signed certificate creation
        // In production, use proper certificate generation
        return try {
            val converter = JcaX509CertificateConverter()
            val x500Name = X500Name("CN=APK Installer")

            // Create a basic certificate (this is simplified)
            // You would need proper certificate builder in production
            val certHolder = X509CertificateHolder(ByteArray(0)) // Placeholder
            converter.getCertificate(certHolder)
        } catch (e: Exception) {
            // Fallback: create a minimal certificate structure
            throw RuntimeException("Certificate creation failed", e)
        }
    }

    private fun signApkWithKey(
        inputPath: String,
        outputPath: String,
        privateKey: PrivateKey,
        certificate: X509Certificate
    ) {
        // This is a simplified APK signing process
        // In production, you would need proper APK v2/v3 signing

        val inputFile = File(inputPath)
        val outputFile = File(outputPath)

        // Copy APK and add signature (simplified)
        inputFile.copyTo(outputFile, overwrite = true)

        // Add META-INF signing files (simplified approach)
        addSignatureFiles(outputFile, privateKey, certificate)
    }

    private fun addSignatureFiles(
        apkFile: File,
        privateKey: PrivateKey,
        certificate: X509Certificate
    ) {
        // This is a simplified signature addition
        // In production, use proper APK signing tools
        val tempFile = File(apkFile.parent, "${apkFile.name}.tmp")

        ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
            ZipInputStream(FileInputStream(apkFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.name.startsWith("META-INF/")) {
                        zos.putNextEntry(ZipEntry(entry.name))
                        zis.copyTo(zos)
                        zos.closeEntry()
                    }
                    entry = zis.nextEntry
                }

                // Add signature files (simplified)
                addMetaInfFiles(zos, privateKey, certificate)
            }
        }

        tempFile.renameTo(apkFile)
    }

    private fun addMetaInfFiles(
        zos: ZipOutputStream,
        privateKey: PrivateKey,
        certificate: X509Certificate
    ) {
        // Add MANIFEST.MF
        zos.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
        zos.write("Manifest-Version: 1.0\nCreated-By: APK Installer\n".toByteArray())
        zos.closeEntry()

        // Add CERT.SF (simplified)
        zos.putNextEntry(ZipEntry("META-INF/CERT.SF"))
        zos.write("Signature-Version: 1.0\nCreated-By: APK Installer\n".toByteArray())
        zos.closeEntry()

        // Add CERT.RSA (simplified)
        zos.putNextEntry(ZipEntry("META-INF/CERT.RSA"))
        zos.write(certificate.encoded)
        zos.closeEntry()
    }
}