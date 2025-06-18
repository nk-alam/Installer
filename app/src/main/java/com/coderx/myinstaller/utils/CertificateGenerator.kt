package com.coderx.myinstaller.utils

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.Security
import java.security.cert.X509Certificate
import java.util.*

class CertificateGenerator {

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun generateSelfSignedCertificate(keyPair: KeyPair): X509Certificate {
        val now = Date()
        val validity = Date(now.time + 365L * 24 * 60 * 60 * 1000) // 1 year

        val issuer = X500Name("CN=APK Installer,O=CoderX,C=US")
        val subject = issuer

        val certBuilder = JcaX509v3CertificateBuilder(
            issuer,
            BigInteger.valueOf(System.currentTimeMillis()),
            now,
            validity,
            subject,
            keyPair.public
        )

        // Add extensions
        certBuilder.addExtension(
            Extension.basicConstraints,
            false,
            BasicConstraints(false)
        )

        certBuilder.addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment)
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSA")
            .setProvider("BC")
            .build(keyPair.private)

        val certHolder = certBuilder.build(signer)

        return JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certHolder)
    }
}