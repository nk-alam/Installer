package com.apkinstaller.store.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class InstallationState {
    NOT_INSTALLED,
    PREPARING,
    SIGNING,
    INSTALLING,
    INSTALLED,
    FAILED,
    CANCELLED,
    READY_TO_INSTALL,
    UPDATING,
    UNINSTALLING
}

@Parcelize
data class InstallationProgress(
    val state: InstallationState,
    val progress: Int = 0,
    val message: String = "",
    val error: String? = null,
    val packageName: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    
    fun isInProgress(): Boolean {
        return state in listOf(
            InstallationState.PREPARING,
            InstallationState.SIGNING,
            InstallationState.INSTALLING,
            InstallationState.UPDATING,
            InstallationState.UNINSTALLING
        )
    }
    
    fun isCompleted(): Boolean {
        return state == InstallationState.INSTALLED
    }
    
    fun isFailed(): Boolean {
        return state == InstallationState.FAILED
    }
}

@Parcelize
data class InstallationResult(
    val success: Boolean,
    val packageName: String,
    val message: String,
    val errorCode: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable