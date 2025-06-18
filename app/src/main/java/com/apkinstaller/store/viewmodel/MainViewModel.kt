package com.apkinstaller.store.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.apkinstaller.store.model.AppInfo

class MainViewModel : ViewModel() {
    
    private val _apps = MutableLiveData<List<AppInfo>>()
    val apps: LiveData<List<AppInfo>> = _apps
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun loadApps(appList: List<AppInfo>) {
        _isLoading.value = true
        _apps.value = appList
        _isLoading.value = false
    }
    
    fun updateAppStatus(packageName: String, isInstalled: Boolean) {
        val currentApps = _apps.value?.toMutableList() ?: return
        val index = currentApps.indexOfFirst { it.packageName == packageName }
        
        if (index != -1) {
            currentApps[index] = currentApps[index].copy(isInstalled = isInstalled)
            _apps.value = currentApps
        }
    }
}