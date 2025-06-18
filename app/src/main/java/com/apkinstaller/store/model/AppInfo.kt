package com.apkinstaller.store.model

import android.graphics.drawable.Drawable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class AppInfo(
    val name: String,
    val packageName: String,
    val version: String,
    val versionCode: Long,
    val size: Long,
    val description: String,
    val icon: @RawValue Drawable? = null,
    val apkPath: String,
    val isInstalled: Boolean = false,
    val permissions: List<String> = emptyList(),
    val minSdkVersion: Int = 0,
    val targetSdkVersion: Int = 0,
    val installTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val isSystemApp: Boolean = false,
    val signature: String? = null
) : Parcelable {
    
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "${size} B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    fun getFormattedVersion(): String {
        return if (versionCode > 0) {
            "$version ($versionCode)"
        } else {
            version
        }
    }
    
    fun isCompatible(deviceSdk: Int): Boolean {
        return minSdkVersion <= deviceSdk
    }
}