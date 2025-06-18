package com.apkinstaller.store.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.apkinstaller.store.R
import com.apkinstaller.store.adapter.AppAdapter
import com.apkinstaller.store.databinding.ActivityMainBinding
import com.apkinstaller.store.model.AppInfo
import com.apkinstaller.store.utils.ApkParser
import com.apkinstaller.store.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var appAdapter: AppAdapter
    private lateinit var apkParser: ApkParser
    
    private val packageStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.apkinstaller.store.PACKAGE_STATUS_CHANGED") {
                Log.d(TAG, "Package status changed, refreshing app list")
                loadSampleApps()
            }
        }
    }
    
    private val installationResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.apkinstaller.store.INSTALLATION_RESULT") {
                val packageName = intent.getStringExtra("package_name") ?: ""
                val status = intent.getIntExtra("status", -1)
                Log.d(TAG, "Installation result received for $packageName: $status")
                loadSampleApps()
            }
        }
    }
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleSelectedApk(it) }
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            checkInstallPermission()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (canRequestPackageInstalls()) {
            Toast.makeText(this, "Install permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Install permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.d(TAG, "Notification permission granted")
        } else {
            Log.d(TAG, "Notification permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        setupFab()
        checkPermissions()
        loadSampleApps()
        registerReceivers()
        
        // Handle APK files opened with this app
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) { // Changed from Intent? to Intent
        super.onNewIntent(intent)
        handleIncomingIntent(intent) // No longer need the null check 'intent?.let'
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_store)
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        apkParser = ApkParser(this)
        
        viewModel.apps.observe(this) { apps ->
            appAdapter.submitList(apps)
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            // Handle loading state if needed
            Log.d(TAG, "Loading state: $isLoading")
        }
    }
    
    private fun setupRecyclerView() {
        appAdapter = AppAdapter(
            onInstallClick = { appInfo -> installApp(appInfo) },
            onOpenClick = { appInfo -> openApp(appInfo) },
            onUninstallClick = { appInfo -> uninstallApp(appInfo) }
        )
        
        binding.recyclerViewApps.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun setupFab() {
        binding.fabAddApp.setOnClickListener {
            filePickerLauncher.launch("application/vnd.android.package-archive")
        }
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        // Storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            checkInstallPermission()
        }
        
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    private fun checkInstallPermission() {
        if (!canRequestPackageInstalls()) {
            showInstallPermissionDialog()
        }
    }
    
    private fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }
    
    private fun showInstallPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.install_permission_message)
            .setPositiveButton(R.string.grant_permission) { _, _ ->
                requestInstallPermission()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:$packageName")
            }
            installPermissionLauncher.launch(intent)
        }
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs storage permissions to function properly.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun loadSampleApps() {
        lifecycleScope.launch {
            try {
                val sampleApps = createSampleApps()
                viewModel.loadApps(sampleApps)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sample apps", e)
                Toast.makeText(this@MainActivity, "Error loading apps", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun createSampleApps(): List<AppInfo> {
        // In a real app, you would load APK files from assets or external storage
        return listOf(
            AppInfo(
                name = "Sample Calculator",
                packageName = "com.example.calculator",
                version = "1.0.0",
                versionCode = 1,
                size = 2 * 1024 * 1024, // 2MB
                description = "A simple calculator app for basic arithmetic operations",
                apkPath = "assets://calculator.apk",
                isInstalled = apkParser.isAppInstalled("com.example.calculator"),
                minSdkVersion = 21,
                targetSdkVersion = 35
            ),
            AppInfo(
                name = "Note Taking App",
                packageName = "com.example.notes",
                version = "2.1.0",
                versionCode = 21,
                size = 5 * 1024 * 1024, // 5MB
                description = "Take notes and organize your thoughts with this simple note-taking app",
                apkPath = "assets://notes.apk",
                isInstalled = apkParser.isAppInstalled("com.example.notes"),
                minSdkVersion = 23,
                targetSdkVersion = 35
            ),
            AppInfo(
                name = "Weather Widget",
                packageName = "com.example.weather",
                version = "1.5.2",
                versionCode = 152,
                size = 3 * 1024 * 1024, // 3MB
                description = "Get current weather information and forecasts for your location",
                apkPath = "assets://weather.apk",
                isInstalled = apkParser.isAppInstalled("com.example.weather"),
                minSdkVersion = 24,
                targetSdkVersion = 35
            )
        )
    }
    
    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    handleSelectedApk(uri)
                }
            }
        }
    }
    
    private fun handleSelectedApk(uri: Uri) {
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val tempFile = File(cacheDir, "temp_${System.currentTimeMillis()}.apk")
                
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val appInfo = apkParser.parseApk(tempFile.absolutePath)
                if (appInfo != null) {
                    val intent = Intent(this@MainActivity, InstallerActivity::class.java).apply {
                        putExtra("app_info", appInfo)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "Failed to parse APK file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling APK file", e)
                Toast.makeText(this@MainActivity, "Error handling APK file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun installApp(appInfo: AppInfo) {
        val intent = Intent(this, InstallerActivity::class.java).apply {
            putExtra("app_info", appInfo)
        }
        startActivity(intent)
    }
    
    private fun openApp(appInfo: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Cannot open ${appInfo.name}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app", e)
            Toast.makeText(this, "Error opening app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun uninstallApp(appInfo: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle("Uninstall ${appInfo.name}")
            .setMessage("Are you sure you want to uninstall this app?")
            .setPositiveButton("Uninstall") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.parse("package:${appInfo.packageName}")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error uninstalling app", e)
                    Toast.makeText(this, "Error uninstalling app: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun registerReceivers() {
        val packageFilter = IntentFilter("com.apkinstaller.store.PACKAGE_STATUS_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageStatusReceiver, packageFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(packageStatusReceiver, packageFilter)
        }
        
        val installationFilter = IntentFilter("com.apkinstaller.store.INSTALLATION_RESULT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(installationResultReceiver, installationFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(installationResultReceiver, installationFilter)
        }
    }
    
    private fun unregisterReceivers() {
        try {
            unregisterReceiver(packageStatusReceiver)
            unregisterReceiver(installationResultReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receivers", e)
        }
    }
}