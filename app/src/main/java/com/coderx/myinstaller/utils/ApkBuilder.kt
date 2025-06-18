package com.coderx.myinstaller.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkBuilder(private val context: Context) {

    companion object {
        private const val TAG = "ApkBuilder"
    }

    suspend fun buildMinimalApk(
        packageName: String,
        appName: String,
        versionName: String,
        versionCode: Int,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building minimal APK for $packageName")

            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                // Add AndroidManifest.xml
                addManifest(zos, packageName, appName, versionName, versionCode)
                
                // Add classes.dex (minimal)
                addClassesDex(zos)
                
                // Add resources.arsc (minimal)
                addResources(zos)
                
                // Add res/drawable/icon.png (placeholder)
                addIcon(zos)
            }

            Log.d(TAG, "APK built successfully: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build APK", e)
            false
        }
    }

    private fun addManifest(
        zos: ZipOutputStream,
        packageName: String,
        appName: String,
        versionName: String,
        versionCode: Int
    ) {
        try {
            val manifest = """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="$packageName"
                    android:versionCode="$versionCode"
                    android:versionName="$versionName">
                    
                    <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="35" />
                    
                    <application
                        android:label="$appName"
                        android:icon="@drawable/icon">
                        
                        <activity
                            android:name=".MainActivity"
                            android:exported="true">
                            <intent-filter>
                                <action android:name="android.intent.action.MAIN" />
                                <category android:name="android.intent.category.LAUNCHER" />
                            </intent-filter>
                        </activity>
                    </application>
                </manifest>
            """.trimIndent()

            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write(manifest.toByteArray())
            zos.closeEntry()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add manifest", e)
            throw e
        }
    }

    private fun addClassesDex(zos: ZipOutputStream) {
        try {
            // Minimal DEX file header (this is a simplified placeholder)
            val dexHeader = byteArrayOf(
                0x64, 0x65, 0x78, 0x0A, // dex\n
                0x30, 0x33, 0x39, 0x00, // 039\0 (updated version)
                // Add minimal DEX structure
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
            )
            
            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write(dexHeader)
            zos.closeEntry()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add classes.dex", e)
            throw e
        }
    }

    private fun addResources(zos: ZipOutputStream) {
        try {
            // Minimal resources.arsc (placeholder)
            val resourcesData = "MINIMAL_RESOURCES_ANDROID_15".toByteArray()
            
            zos.putNextEntry(ZipEntry("resources.arsc"))
            zos.write(resourcesData)
            zos.closeEntry()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add resources", e)
            throw e
        }
    }

    private fun addIcon(zos: ZipOutputStream) {
        try {
            // Add a minimal PNG icon (1x1 transparent pixel)
            val pngData = byteArrayOf(
                0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4,
                0x89.toByte(), 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
                0x78, 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05,
                0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4.toByte(), 0x00, 0x00,
                0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42,
                0x60, 0x82.toByte()
            )
            
            zos.putNextEntry(ZipEntry("res/drawable/icon.png"))
            zos.write(pngData)
            zos.closeEntry()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add icon", e)
            throw e
        }
    }
}