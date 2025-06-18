package com.apkinstaller.store.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.apkinstaller.store.R
import com.apkinstaller.store.databinding.ItemAppBinding
import com.apkinstaller.store.model.AppInfo

class AppAdapter(
    private val onInstallClick: (AppInfo) -> Unit,
    private val onOpenClick: (AppInfo) -> Unit,
    private val onUninstallClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppAdapter.AppViewHolder>(AppDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class AppViewHolder(private val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(appInfo: AppInfo) {
            binding.apply {
                textViewName.text = appInfo.name
                textViewDescription.text = appInfo.description
                textViewSize.text = appInfo.getFormattedSize()
                
                // Set app icon
                if (appInfo.icon != null) {
                    imageViewIcon.setImageDrawable(appInfo.icon)
                } else {
                    imageViewIcon.setImageResource(R.drawable.ic_android)
                }
                
                // Set version info
                textViewVersion.text = "v${appInfo.version}"
                
                // Check compatibility
                val context = binding.root.context
                val deviceSdk = android.os.Build.VERSION.SDK_INT
                val isCompatible = appInfo.isCompatible(deviceSdk)
                
                if (!isCompatible) {
                    buttonAction.text = "Incompatible"
                    buttonAction.isEnabled = false
                    textViewCompatibility.text = "Requires Android API ${appInfo.minSdkVersion}+"
                    textViewCompatibility.visibility = android.view.View.VISIBLE
                } else {
                    textViewCompatibility.visibility = android.view.View.GONE
                    buttonAction.isEnabled = true
                    
                    // Set button text and action based on installation status
                    when {
                        appInfo.isInstalled -> {
                            buttonAction.text = "Open"
                            buttonAction.setOnClickListener { onOpenClick(appInfo) }
                        }
                        else -> {
                            buttonAction.text = "Install"
                            buttonAction.setOnClickListener { onInstallClick(appInfo) }
                        }
                    }
                }
                
                // Handle long click for uninstall
                root.setOnLongClickListener {
                    if (appInfo.isInstalled && isCompatible) {
                        onUninstallClick(appInfo)
                    }
                    true
                }
                
                // Set additional info
                val additionalInfo = buildString {
                    if (appInfo.targetSdkVersion > 0) {
                        append("Target SDK: ${appInfo.targetSdkVersion}")
                    }
                    if (appInfo.permissions.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append("${appInfo.permissions.size} permissions")
                    }
                }
                
                if (additionalInfo.isNotEmpty()) {
                    textViewAdditionalInfo.text = additionalInfo
                    textViewAdditionalInfo.visibility = android.view.View.VISIBLE
                } else {
                    textViewAdditionalInfo.visibility = android.view.View.GONE
                }
            }
        }
    }
    
    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }
        
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}