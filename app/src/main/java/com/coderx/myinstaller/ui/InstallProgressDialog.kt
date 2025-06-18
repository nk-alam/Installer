package com.coderx.myinstaller.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.coderx.myinstaller.databinding.DialogInstallProgressBinding

class InstallProgressDialog(
    context: Context,
    private val appName: String
) : Dialog(context) {

    private lateinit var binding: DialogInstallProgressBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogInstallProgressBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        setCancelable(false)
        setCanceledOnTouchOutside(false)

        binding.textAppName.text = "Installing $appName..."
        binding.progressBar.max = 100
        binding.progressBar.progress = 0
        binding.textProgress.text = "0%"
        binding.textStatus.text = "Preparing..."
    }

    fun updateProgress(progress: Int) {
        binding.progressBar.progress = progress
        binding.textProgress.text = "$progress%"

        binding.textStatus.text = when {
            progress < 20 -> "Building APK..."
            progress < 50 -> "Signing APK..."
            progress < 80 -> "Validating APK..."
            progress < 95 -> "Installing..."
            else -> "Finalizing..."
        }
    }

    fun setCompleted(success: Boolean) {
        binding.textStatus.text = if (success) {
            "Installation completed!"
        } else {
            "Installation failed!"
        }
    }
}