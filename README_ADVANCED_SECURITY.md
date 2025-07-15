# Advanced Security Implementation

This document describes the advanced security features implemented in the APK installer app, including build-time string encryption and dynamic APK signing.

## 🔐 Features Overview

### 1. Build-Time String Encryption
- **Automatic Detection**: Scans source code for sensitive strings during build
- **AES-GCM Encryption**: Uses authenticated encryption for string protection
- **Runtime Decryption**: Strings are decrypted transparently at runtime
- **Pattern-Based**: Encrypts API keys, URLs, package names, and constants

### 2. Dynamic APK Signing
- **Device-Specific**: Each device gets a uniquely signed APK
- **Runtime Signing**: APK is signed just before installation
- **Self-Signed Certificates**: Generates certificates based on device information
- **Tamper Protection**: Prevents APK modification and reuse

## 🏗️ Build Process

### String Encryption Process
1. **Source Scanning**: `StringEncryptionTask` scans all `.kt` and `.java` files
2. **Pattern Matching**: Identifies sensitive strings using regex patterns
3. **Encryption**: Encrypts found strings using AES-GCM with random IVs
4. **Code Generation**: Creates `EncryptedStrings.kt` utility class
5. **Source Replacement**: Replaces original strings with decryption calls

### Build Tasks Order
```
preBuild
├── encryptStrings (new)
├── encryptAssets (existing)
└── compile
```

## 🔧 Implementation Details

### String Encryption Patterns
The system automatically encrypts strings matching these patterns:

```kotlin
// URLs
"https://api.example.com/endpoint"

// API Keys (20+ characters)
"sk_live_abcdef123456789012345"

// Package Names
"com.example.myapp"

// Encryption Keys
"MySecretKey12345"

// Constants (5+ uppercase chars)
"API_BASE_URL"
```

### Dynamic APK Signing Process
1. **Device Fingerprinting**: Uses Android ID + package name + timestamp
2. **Key Generation**: Creates RSA 2048-bit key pair with device-specific seed
3. **Certificate Creation**: Generates self-signed X.509 certificate
4. **APK Preparation**: Removes existing signatures from APK
5. **Signing**: Creates MANIFEST.MF, CERT.SF, and CERT.RSA files
6. **Installation**: Uses the freshly signed APK

## 📁 File Structure

```
app/
├── build/
│   ├── encrypted-src/           # Generated encrypted source code
│   └── encrypted-assets/        # Encrypted assets (existing)
├── src/main/java/com/coderx/installer/utils/
│   ├── EncryptedStrings.kt      # Generated at build time
│   ├── DynamicApkSigner.kt      # APK signing utility
│   ├── AssetEncryption.kt       # Asset encryption (existing)
│   └── SecurityUtils.kt         # Security utilities (existing)
└── buildSrc/src/main/kotlin/
    ├── StringEncryptionTask.kt   # Build task for string encryption
    └── AssetEncryptionTask.kt    # Build task for asset encryption (existing)
```

## 🔒 Security Benefits

### String Encryption
- **Static Analysis Protection**: Encrypted strings can't be found in APK
- **Reverse Engineering Resistance**: Strings are meaningless without decryption
- **Runtime Protection**: Decryption happens in memory only
- **Key Obfuscation**: Encryption keys are obfuscated by ProGuard

### Dynamic APK Signing
- **Device Binding**: APK only works on the device it was signed for
- **Replay Attack Prevention**: Each installation gets a unique signature
- **Tamper Detection**: Modified APKs will fail signature verification
- **Distribution Control**: Prevents APK sharing between devices

## 🚀 Usage Examples

### Encrypted Strings Usage
Before encryption:
```kotlin
val apiUrl = "https://api.myservice.com/v1/data"
val apiKey = "sk_live_1234567890abcdef"
```

After encryption (automatic):
```kotlin
val apiUrl = EncryptedStrings.decrypt("ENCRYPTED_A1B2C3D4")
val apiKey = EncryptedStrings.decrypt("ENCRYPTED_E5F6G7H8")
```

### Dynamic Signing Usage
```kotlin
// In InstallationActivity
val signedApkFile = DynamicApkSigner.signApk(context, originalApkFile)
startInstantInstallation(signedApkFile)
```

## ⚙️ Configuration

### Adding String Patterns
Edit `StringEncryptionTask.kt` to add new patterns:

```kotlin
val sensitivePatterns = listOf(
    Regex("\"(your_new_pattern)\""),
    // ... existing patterns
)
```

### Customizing Signing
Modify `DynamicApkSigner.kt` for different signing algorithms:

```kotlin
// Change key size
keyPairGenerator.initialize(4096, secureRandom) // RSA 4096

// Change signature algorithm
val signature = Signature.getInstance("SHA512withRSA")
```

## 🧪 Testing

### Build Testing
```bash
# Test string encryption
./gradlew encryptStrings

# Test asset encryption
./gradlew encryptAssets

# Full build with both
./gradlew assembleDebug
```

### Runtime Testing
1. Install the app on a device
2. Check logs for encryption/decryption messages
3. Verify APK signing process in installation logs
4. Test that strings are properly decrypted

### Security Validation
```bash
# Check that strings are encrypted in APK
unzip -l app-debug.apk
strings app-debug.apk | grep -i "api\|key\|secret"

# Verify dynamic signing
adb logcat | grep -E "(DynamicApkSigner|InstallationActivity)"
```

## 🔧 Troubleshooting

### Build Issues
- **String encryption fails**: Check regex patterns and source file encoding
- **Missing EncryptedStrings class**: Ensure `encryptStrings` task runs before compilation
- **ProGuard issues**: Verify keep rules for encryption classes

### Runtime Issues
- **String decryption fails**: Check encryption key consistency
- **APK signing fails**: Verify device permissions and storage space
- **Installation fails**: Check APK signature validity

### Performance Considerations
- **Build Time**: String encryption adds ~10-30 seconds to build time
- **APK Size**: Minimal increase due to encryption overhead
- **Runtime**: Negligible performance impact for string decryption
- **Signing Time**: APK signing adds ~5-15 seconds to installation

## 🛡️ Security Recommendations

### Production Deployment
1. **Use NDK**: Store encryption keys in native code
2. **Key Rotation**: Implement periodic key rotation
3. **Certificate Pinning**: Add network security for remote validation
4. **Root Detection**: Add anti-tampering measures
5. **Code Obfuscation**: Use advanced ProGuard/R8 configurations

### Enhanced Security
```kotlin
// Example: Enhanced device-specific key derivation
val deviceFingerprint = SecurityUtils.getDeviceFingerprint(context)
val appSignature = SecurityUtils.getAppSignature(context)
val hardwareInfo = SecurityUtils.getHardwareInfo()

val enhancedKey = SecurityUtils.deriveEnhancedKey(
    baseKey = "your_base_key",
    deviceFingerprint = deviceFingerprint,
    appSignature = appSignature,
    hardwareInfo = hardwareInfo
)
```

This advanced security implementation provides multiple layers of protection while maintaining the app's functionality and user experience.