package com.coderx.myinstaller.utils

import android.content.Context
import android.util.Log
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.*
import java.security.cert.X509Certificate

class KeystoreManager(private val context: Context) {

    companion object {
        private const val TAG = "KeystoreManager"
        private const val KEYSTORE_PASSWORD = "installer123"
        private const val KEY_ALIAS = "apk_signer"

        init {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }
    }

    fun loadOrCreateKeystore(): KeyStore {
        return try {
            // Try to load from assets first
            loadKeystoreFromAssets()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load keystore from assets, creating new one", e)
            createNewKeystore()
        }
    }

    private fun loadKeystoreFromAssets(): KeyStore {
        val keystore = KeyStore.getInstance("PKCS12")
        try {
            context.assets.open("keystore.p12").use { inputStream ->
                keystore.load(inputStream, KEYSTORE_PASSWORD.toCharArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load keystore from assets", e)
            throw e
        }
        return keystore
    }

    private fun createNewKeystore(): KeyStore {
        val keystore = KeyStore.getInstance("PKCS12")
        keystore.load(null, null)

        try {
            // Generate RSA key pair
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair = keyPairGenerator.generateKeyPair()

            // Create self-signed certificate
            val certificateGenerator = CertificateGenerator()
            val certificate = certificateGenerator.generateSelfSignedCertificate(keyPair)

            // Store in keystore
            keystore.setKeyEntry(
                KEY_ALIAS,
                keyPair.private,
                KEYSTORE_PASSWORD.toCharArray(),
                arrayOf(certificate)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create keystore", e)
            throw e
        }

        return keystore
    }

    fun getSigningKey(keystore: KeyStore): Pair<PrivateKey, X509Certificate> {
        return try {
            val privateKey = keystore.getKey(KEY_ALIAS, KEYSTORE_PASSWORD.toCharArray()) as PrivateKey
            val certificate = keystore.getCertificate(KEY_ALIAS) as X509Certificate
            Pair(privateKey, certificate)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get signing key", e)
            throw e
        }
    }
}