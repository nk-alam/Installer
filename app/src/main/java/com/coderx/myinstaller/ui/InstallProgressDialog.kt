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

        binding.textAppName.text = "Installing $appName..."
        binding.progressBar.max = 100
    }

    fun updateProgress(progress: Int) {
        binding.progressBar.progress = progress
        binding.textProgress.text = "$progress%"

        when {
            progress < 20 -> binding.textStatus.text = "Extracting APK..."
            progress < 60 -> binding.textStatus.text = "Signing APK..."
            progress < 90 -> binding.textStatus.text = "Preparing installation..."
            else -> binding.textStatus.text = "Finalizing..."
        }
    }
}