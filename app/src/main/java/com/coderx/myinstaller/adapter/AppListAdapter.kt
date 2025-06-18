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
        try {
            holder.bind(getItem(position))
        } catch (e: Exception) {
            // Handle binding errors gracefully
        }
    }

    inner class AppViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appInfo: AppInfo) {
            try {
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
                            buttonAction.setBackgroundColor(
                                root.context.getColor(android.R.color.holo_blue_bright)
                            )
                            buttonAction.setOnClickListener { onInstallClick(appInfo) }
                        }

                        InstallationState.INSTALLED -> {
                            buttonAction.text = "Open"
                            buttonAction.isEnabled = true
                            buttonAction.setBackgroundColor(
                                root.context.getColor(android.R.color.holo_green_light)
                            )
                            buttonAction.setOnClickListener { onLaunchClick(appInfo) }
                        }

                        InstallationState.UPDATE_AVAILABLE -> {
                            buttonAction.text = "Update"
                            buttonAction.isEnabled = true
                            buttonAction.setBackgroundColor(
                                root.context.getColor(android.R.color.holo_orange_light)
                            )
                            buttonAction.setOnClickListener { onInstallClick(appInfo) }
                        }

                        InstallationState.INSTALLING -> {
                            buttonAction.text = "Installing..."
                            buttonAction.isEnabled = false
                            buttonAction.setBackgroundColor(
                                root.context.getColor(android.R.color.darker_gray)
                            )
                            buttonAction.setOnClickListener(null)
                        }

                        InstallationState.FAILED -> {
                            buttonAction.text = "Retry"
                            buttonAction.isEnabled = true
                            buttonAction.setBackgroundColor(
                                root.context.getColor(android.R.color.holo_red_light)
                            )
                            buttonAction.setOnClickListener { onInstallClick(appInfo) }
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle binding errors gracefully
            }
        }

        private fun formatFileSize(bytes: Long): String {
            return try {
                val mb = bytes / (1024.0 * 1024.0)
                "${mb.roundToInt()} MB"
            } catch (e: Exception) {
                "Unknown size"
            }
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