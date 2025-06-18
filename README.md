# APK Installer App

A modern Android application that allows users to install APK files with a clean, intuitive interface.

## Features

- **Modern UI**: Clean Material Design interface with card-based layout
- **Progress Tracking**: Real-time installation progress with notifications
- **Permission Management**: Automatic handling of required permissions
- **APK Validation**: Validates APK files before installation
- **Installation States**: Shows different states (Not Installed, Installing, Installed, Update Available, Failed)
- **Launch Apps**: Directly launch installed apps from the interface
- **Notification Support**: Progress notifications during installation

## Requirements

- Android 7.0 (API level 24) or higher
- Storage permissions for APK file access
- Install unknown apps permission (Android 8.0+)
- Notification permission (Android 13+)

## Permissions

The app requires the following permissions:

- `REQUEST_INSTALL_PACKAGES`: To install APK files
- `WRITE_EXTERNAL_STORAGE`: To access APK files (Android 10 and below)
- `READ_EXTERNAL_STORAGE`: To read APK files (Android 12 and below)
- `POST_NOTIFICATIONS`: To show installation progress notifications (Android 13+)

## Installation

1. Clone the repository
2. Open the project in Android Studio
3. Build and run the application
4. Grant the required permissions when prompted

## Usage

1. Launch the app
2. Grant necessary permissions
3. Browse the list of available apps
4. Tap "Install" to install an app
5. Monitor installation progress
6. Tap "Open" to launch installed apps

## Architecture

The app follows modern Android development practices:

- **MVVM Architecture**: Clean separation of concerns
- **LiveData**: Reactive UI updates
- **Coroutines**: Asynchronous operations
- **ViewBinding**: Type-safe view references
- **Material Design**: Modern UI components

## Key Components

- `InstallActivity`: Main activity with app list and installation UI
- `InstallationManager`: Handles APK installation logic
- `AppListAdapter`: RecyclerView adapter for app list
- `ApkSigner`: Handles APK signing (simplified implementation)
- `NotificationHelper`: Manages installation notifications
- `InstallReceiver`: Broadcast receiver for installation events

## Security

- APK validation before installation
- Basic APK signing (simplified for demo purposes)
- Permission checks and user consent
- Secure file provider for APK sharing

## Limitations

- This is a demo implementation with simplified APK signing
- For production use, implement proper APK signing with valid certificates
- Some features may require system-level permissions not available to regular apps

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is for educational purposes. Please ensure compliance with relevant laws and regulations when using APK installation functionality.

## Troubleshooting

### Common Issues

1. **Installation fails**: Ensure all permissions are granted
2. **APK not found**: Check if APK files are properly placed in assets
3. **Permission denied**: Enable "Install unknown apps" in device settings
4. **Notifications not showing**: Grant notification permission on Android 13+

### Debug Tips

- Check logcat for detailed error messages
- Verify APK file integrity
- Ensure proper file provider configuration
- Test on different Android versions