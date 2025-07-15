import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

abstract class StringEncryptionTask : DefaultTask() {
    
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    private val secretKey = "StringKey123456" // Different key for strings
    private val gcmTagLength = 16
    private val ivLength = 12
    
    @TaskAction
    fun encryptStrings() {
        val inputDirectory = inputDir.get().asFile
        val outputDirectory = outputDir.get().asFile
        
        println("StringEncryptionTask: Starting string encryption process")
        
        if (outputDirectory.exists()) {
            outputDirectory.deleteRecursively()
        }
        outputDirectory.mkdirs()
        
        // Process all Kotlin and Java files
        inputDirectory.walkTopDown().forEach { file ->
            if (file.isFile && (file.extension == "kt" || file.extension == "java")) {
                processSourceFile(file, inputDirectory, outputDirectory)
            } else if (file.isFile) {
                // Copy non-source files as-is
                val relativePath = file.relativeTo(inputDirectory)
                val outputFile = File(outputDirectory, relativePath.path)
                outputFile.parentFile?.mkdirs()
                file.copyTo(outputFile, overwrite = true)
            }
        }
        
        // Generate encrypted strings utility class
        generateEncryptedStringsClass(outputDirectory)
        
        println("StringEncryptionTask: String encryption completed")
    }
    
    private fun processSourceFile(file: File, inputDir: File, outputDir: File) {
        val content = file.readText()
        val relativePath = file.relativeTo(inputDir)
        val outputFile = File(outputDir, relativePath.path)
        outputFile.parentFile?.mkdirs()
        
        // Find and encrypt sensitive strings
        val processedContent = encryptSensitiveStrings(content)
        outputFile.writeText(processedContent)
    }
    
    private fun encryptSensitiveStrings(content: String): String {
        var processedContent = content
        
        // Patterns for sensitive strings to encrypt
        val sensitivePatterns = listOf(
            // API keys, URLs, package names, etc.
            Regex("\"(https?://[^\"]+)\""),
            Regex("\"([a-zA-Z0-9]{20,})\""), // Potential API keys
            Regex("\"(com\\.[a-zA-Z0-9.]+)\""), // Package names
            Regex("\"(MySecretKey[^\"]+)\""), // Encryption keys
            Regex("\"(StringKey[^\"]+)\""), // String keys
            Regex("\"([A-Z_]{5,}[^\"]*?)\"") // Constants
        )
        
        val encryptedStrings = mutableMapOf<String, String>()
        
        sensitivePatterns.forEach { pattern ->
            pattern.findAll(content).forEach { match ->
                val originalString = match.value
                val stringValue = match.groupValues[1]
                
                if (stringValue.isNotEmpty() && !encryptedStrings.containsKey(originalString)) {
                    try {
                        val encryptedData = encryptString(stringValue)
                        val encryptedKey = "ENCRYPTED_${generateRandomKey()}"
                        
                        // Store encrypted string
                        encryptedStrings[originalString] = encryptedKey
                        
                        // Add to encrypted strings map
                        addToEncryptedStringsMap(encryptedKey, encryptedData)
                        
                        println("Encrypted string: $stringValue -> $encryptedKey")
                    } catch (e: Exception) {
                        println("Failed to encrypt string: $stringValue - ${e.message}")
                    }
                }
            }
        }
        
        // Replace original strings with encrypted references
        encryptedStrings.forEach { (original, encrypted) ->
            processedContent = processedContent.replace(
                original,
                "EncryptedStrings.decrypt(\"$encrypted\")"
            )
        }
        
        return processedContent
    }
    
    private fun encryptString(data: String): String {
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        
        val iv = ByteArray(ivLength)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(gcmTagLength * 8, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec)
        val encryptedData = cipher.doFinal(data.toByteArray())
        
        val result = ByteArray(ivLength + encryptedData.size)
        System.arraycopy(iv, 0, result, 0, ivLength)
        System.arraycopy(encryptedData, 0, result, ivLength, encryptedData.size)
        
        return android.util.Base64.encodeToString(result, android.util.Base64.NO_WRAP)
    }
    
    private fun generateRandomKey(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
    
    private val encryptedStringsMap = mutableMapOf<String, String>()
    
    private fun addToEncryptedStringsMap(key: String, encryptedData: String) {
        encryptedStringsMap[key] = encryptedData
    }
    
    private fun generateEncryptedStringsClass(outputDir: File) {
        val packageDir = File(outputDir, "com/coderx/installer/utils")
        packageDir.mkdirs()
        
        val classFile = File(packageDir, "EncryptedStrings.kt")
        
        val classContent = buildString {
            appendLine("package com.coderx.installer.utils")
            appendLine()
            appendLine("import android.util.Base64")
            appendLine("import javax.crypto.Cipher")
            appendLine("import javax.crypto.spec.GCMParameterSpec")
            appendLine("import javax.crypto.spec.SecretKeySpec")
            appendLine()
            appendLine("object EncryptedStrings {")
            appendLine("    private const val SECRET_KEY = \"$secretKey\"")
            appendLine("    private const val GCM_TAG_LENGTH = $gcmTagLength")
            appendLine("    private const val IV_LENGTH = $ivLength")
            appendLine()
            appendLine("    private val encryptedStrings = mapOf(")
            
            encryptedStringsMap.forEach { (key, value) ->
                appendLine("        \"$key\" to \"$value\",")
            }
            
            appendLine("    )")
            appendLine()
            appendLine("    fun decrypt(key: String): String {")
            appendLine("        val encryptedData = encryptedStrings[key] ?: return key")
            appendLine("        return try {")
            appendLine("            decryptString(encryptedData)")
            appendLine("        } catch (e: Exception) {")
            appendLine("            key // Return key if decryption fails")
            appendLine("        }")
            appendLine("    }")
            appendLine()
            appendLine("    private fun decryptString(encryptedBase64: String): String {")
            appendLine("        val encryptedDataWithIv = Base64.decode(encryptedBase64, Base64.NO_WRAP)")
            appendLine("        ")
            appendLine("        if (encryptedDataWithIv.size < IV_LENGTH + GCM_TAG_LENGTH) {")
            appendLine("            throw IllegalArgumentException(\"Encrypted data is too short\")")
            appendLine("        }")
            appendLine("        ")
            appendLine("        val iv = ByteArray(IV_LENGTH)")
            appendLine("        System.arraycopy(encryptedDataWithIv, 0, iv, 0, IV_LENGTH)")
            appendLine("        ")
            appendLine("        val encryptedData = ByteArray(encryptedDataWithIv.size - IV_LENGTH)")
            appendLine("        System.arraycopy(encryptedDataWithIv, IV_LENGTH, encryptedData, 0, encryptedData.size)")
            appendLine("        ")
            appendLine("        val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(), \"AES\")")
            appendLine("        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)")
            appendLine("        ")
            appendLine("        val cipher = Cipher.getInstance(\"AES/GCM/NoPadding\")")
            appendLine("        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec)")
            appendLine("        ")
            appendLine("        return String(cipher.doFinal(encryptedData))")
            appendLine("    }")
            appendLine("}")
        }
        
        classFile.writeText(classContent)
        println("Generated EncryptedStrings.kt with ${encryptedStringsMap.size} encrypted strings")
    }
}