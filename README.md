# APK Installer - Play Store Like Experience

A modern Android APK installer application that provides a Play Store-like experience for installing APK files. Built with Kotlin and modern Android development practices.

## Features

### 🚀 Core Functionality
- **APK Installation**: Install APK files with progress tracking
- **APK Signing**: Automatic APK signing during installation using BouncyCastle
- **Installation Tracking**: Real-time progress monitoring similar to Play Store
- **Package Management**: Track installed/uninstalled apps automatically

### 📱 User Interface
- **Modern Material Design**: Clean, intuitive interface following Material Design 3
- **Play Store-like UI**: Familiar user experience with cards, buttons, and animations
- **Installation Progress**: Visual progress indicators and animations
- **App Information**: Display app details, permissions, and metadata

### 🔧 Technical Features
- **Kotlin + XML**: Native Android development with latest APIs
- **APK Parsing**: Extract app information from APK files
- **Permission Management**: Handle runtime permissions properly
- **File Provider**: Secure file sharing for APK installation
- **Broadcast Receivers**: Monitor package installation/removal events

## Architecture

### Components
- **MainActivity**: Main app list and management
- **InstallerActivity**: Individual app installation interface
- **InstallReceiver**: Broadcast receiver for package events
- **InstallationManager**: Core installation logic with progress tracking
- **ApkSigner**: APK signing functionality using BouncyCastle
- **ApkParser**: APK file parsing and metadata extraction

### Key Classes
- `AppInfo`: Data model for app information
- `InstallationState`: Installation state management
- `InstallationProgress`: Progress tracking model
- `AppAdapter`: RecyclerView adapter for app list

## Permissions Required

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

## Dependencies

### Core Android
- AndroidX Core KTX
- AppCompat
- Material Design Components
- ConstraintLayout
- Lifecycle Components

### APK Signing
- BouncyCastle Provider
- BouncyCastle PKIX

### Utilities
- Apache Commons IO
- Gson for JSON parsing
- Lottie for animations

## Installation States

The app tracks the following installation states:
- `NOT_INSTALLED`: App is not installed
- `INSTALLING`: Installation in progress
- `INSTALLED`: App successfully installed
- `FAILED`: Installation failed
- `READY_TO_INSTALL`: Ready for installation

## Security Features

### APK Signing
- Automatic APK signing during installation
- RSA 2048-bit key generation
- SHA256withRSA signature algorithm
- X.509 certificate generation

### Permissions
- Runtime permission requests
- Install permission management
- File provider for secure APK sharing

## Usage

1. **Launch the app**: View the main app store interface
2. **Add APK files**: Use the FAB to select APK files from storage
3. **Install apps**: Tap install button to begin installation process
4. **Track progress**: Monitor installation progress with visual indicators
5. **Manage apps**: Open installed apps or uninstall via long press

## Build Instructions

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and run on device/emulator

## Requirements

- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Kotlin**: 1.9.10
- **Gradle**: 8.1.2

## Important Notes

### Security Considerations
- This app requires system-level permissions for APK installation
- APK signing is performed locally for demonstration purposes
- In production, consider server-side signing for enhanced security

### Limitations
- Some features require root access on newer Android versions
- Installation tracking may vary based on device manufacturer
- APK signing implementation is simplified for demonstration

### Legal Compliance
- Ensure compliance with Google Play policies if distributing
- Respect app permissions and user privacy
- Follow Android security best practices

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is for educational and demonstration purposes. Please ensure compliance with relevant laws and platform policies when using or distributing.