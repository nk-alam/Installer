package com.coderx.myinstaller.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.coderx.myinstaller.data.AppInfo
import com.coderx.myinstaller.data.InstallationState
import com.coderx.myinstaller.databinding.ItemAppBinding
import kotlin.math.roundToInt

class AppListAdapter(
    private val onInstallClick: (AppInfo) -> Unit,
    private val onLaunchClick: (AppInfo) -> Unit,
    private val checkInstallationState: (AppInfo) -> InstallationState
) : ListAdapter<AppInfo, AppListAdapter.AppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appInfo: AppInfo) {
            binding.apply {
                textAppName.text = appInfo.appName
                textAppVersion.text = "v${appInfo.versionName}"
                textAppDescription.text = appInfo.description
                textAppSize.text = formatFileSize(appInfo.size)
                imageAppIcon.setImageResource(appInfo.iconResId)

                val installationState = checkInstallationState(appInfo)

                when (installationState) {
                    InstallationState.NOT_INSTALLED -> {
                        buttonAction.text = "Install"
                        buttonAction.isEnabled = true
                        buttonAction.setOnClickListener { onInstallClick(appInfo) }
                    }

                    InstallationState.INSTALLED -> {
                        buttonAction.text = "Open"
                        buttonAction.isEnabled = true
                        buttonAction.setOnClickListener { onLaunchClick(appInfo) }
                    }

                    InstallationState.UPDATE_AVAILABLE -> {
                        buttonAction.text = "Update"
                        buttonAction.isEnabled = true
                        buttonAction.setOnClickListener { onInstallClick(appInfo) }
                    }

                    InstallationState.INSTALLING -> {
                        buttonAction.text = "Installing..."
                        buttonAction.isEnabled = false
                        buttonAction.setOnClickListener(null)
                    }

                    InstallationState.FAILED -> {
                        buttonAction.text = "Retry"
                        buttonAction.isEnabled = true
                        buttonAction.setOnClickListener { onInstallClick(appInfo) }
                    }
                }
            }
        }

        private fun formatFileSize(bytes: Long): String {
            val mb = bytes / (1024.0 * 1024.0)
            return "${mb.roundToInt()} MB"
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}