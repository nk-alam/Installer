package com.apkinstaller.store.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.apkinstaller.store.model.InstallationProgress
import com.apkinstaller.store.model.InstallationState

class InstallerViewModel : ViewModel() {
    
    private val _installationProgress = MutableLiveData<InstallationProgress>()
    val installationProgress: LiveData<InstallationProgress> = _installationProgress
    
    fun updateInstallationProgress(progress: InstallationProgress) {
        _installationProgress.value = progress
    }
    
    fun resetInstallation() {
        _installationProgress.value = InstallationProgress(InstallationState.READY_TO_INSTALL)
    }
}