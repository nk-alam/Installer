package com.coderx.myinstaller.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val iconResId: Int,
    val apkAssetPath: String,
    val description: String,
    val size: Long,
    val permissions: List<String> = emptyList()
) : Parcelable