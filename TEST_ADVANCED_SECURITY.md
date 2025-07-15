# Testing Advanced Security Features

This document provides comprehensive testing instructions for the advanced security features including string encryption and dynamic APK signing.

## 🧪 Testing Checklist

### 1. Build Process Testing

**Test string encryption task:**
```bash
# Clean and test string encryption
./gradlew clean
./gradlew encryptStrings
./gradlew encryptAssets
./gradlew assembleDebug
```

**Verify encrypted source generation:**
- Check `app/build/encrypted-src/` directory exists
- Verify `EncryptedStrings.kt` is generated
- Confirm original strings are replaced with `EncryptedStrings.decrypt()` calls

### 2. String Encryption Verification

**Check encrypted strings in generated code:**
```bash
# Navigate to encrypted source directory
cd app/build/encrypted-src/

# Check for EncryptedStrings class
find . -name "EncryptedStrings.kt"

# Verify string replacements
grep -r "EncryptedStrings.decrypt" .
```

**Verify strings are encrypted in final APK:**
```bash
# Extract APK and check for original strings
unzip app-debug.apk -d extracted_apk/
strings extracted_apk/classes.dex | grep -E "(MySecretKey|StringKey|https://)"
```

### 3. Dynamic APK Signing Testing

**Test signing process:**
1. Install the app on a device/emulator
2. Grant installation permissions
3. Click "Update" button
4. Monitor logs for signing process

**Check signing logs:**
```bash
adb logcat | grep -E "(DynamicApkSigner|signing|certificate)"
```

### 4. Security Validation

**Expected log output for string encryption:**
```
StringEncryptionTask: Starting string encryption process
Encrypted string: MySecretKey12345 -> ENCRYPTED_A1B2C3D4
Encrypted string: com.example.app -> ENCRYPTED_E5F6G7H8
Generated EncryptedStrings.kt with X encrypted strings
StringEncryptionTask: String encryption completed
```

**Expected log output for dynamic signing:**
```
D/DynamicApkSigner: Starting dynamic APK signing process
D/DynamicApkSigner: Generating device-specific certificate
D/DynamicApkSigner: Signing APK with generated certificate
D/DynamicApkSigner: APK signed successfully: /data/data/.../cache/signed_xxx.apk
```

### 5. Runtime Testing

**Test encrypted string decryption:**
1. Install and run the app
2. Verify all functionality works normally
3. Check that encrypted strings are properly decrypted
4. Monitor for any decryption errors

**Test dynamic signing:**
1. Verify APK is signed before installation
2. Check that installation succeeds with signed APK
3. Confirm APK signature is device-specific

## 🔍 Detailed Verification Steps

### String Encryption Verification

**1. Check Build Output:**
```bash
# Look for encryption task output
./gradlew assembleDebug | grep -A 10 -B 10 "StringEncryptionTask"
```

**2. Verify Generated Class:**
```kotlin
// Check app/build/encrypted-src/com/coderx/installer/utils/EncryptedStrings.kt
// Should contain:
object EncryptedStrings {
    private val encryptedStrings = mapOf(
        "ENCRYPTED_XXXXX" to "base64_encrypted_data",
        // ... more entries
    )
    
    fun decrypt(key: String): String { ... }
}
```

**3. Check Source Replacement:**
```bash
# Original source should have replacements like:
grep -r "EncryptedStrings.decrypt" app/build/encrypted-src/
```

### Dynamic Signing Verification

**1. Monitor Installation Process:**
```bash
# Watch for signing-related logs
adb logcat -s DynamicApkSigner InstallationActivity | grep -E "(sign|certificate|key)"
```

**2. Verify Signed APK Creation:**
```bash
# Check for temporary signed APK files
adb shell "ls -la /data/data/com.coderx.installer/cache/ | grep signed_"
```

**3. Test Device Specificity:**
- Install app on Device A
- Extract signed APK from Device A
- Try to install extracted APK on Device B (should fail or behave differently)

## 🚨 Troubleshooting Guide

### String Encryption Issues

**Problem: EncryptedStrings class not generated**
```bash
# Check if task ran
./gradlew encryptStrings --info

# Verify input directory exists
ls -la app/src/main/java/
```

**Problem: Strings not being encrypted**
- Check regex patterns in `StringEncryptionTask.kt`
- Verify string format matches patterns
- Look for compilation errors

**Problem: Runtime decryption fails**
```bash
# Check for decryption errors
adb logcat | grep -E "(EncryptedStrings|decrypt|cipher)"
```

### Dynamic Signing Issues

**Problem: Signing fails**
```bash
# Check for signing errors
adb logcat | grep -E "(DynamicApkSigner|sign.*fail|certificate.*error)"
```

**Problem: Installation fails after signing**
- Verify APK signature validity
- Check device permissions
- Monitor for signature verification errors

**Problem: Performance issues**
- Monitor signing time in logs
- Check available storage space
- Verify memory usage during signing

## ✅ Success Criteria

### String Encryption Success
1. ✅ `encryptStrings` task completes without errors
2. ✅ `EncryptedStrings.kt` is generated with encrypted data
3. ✅ Original strings are replaced with decrypt calls
4. ✅ App functions normally with encrypted strings
5. ✅ Original strings are not found in final APK
6. ✅ Decryption works correctly at runtime

### Dynamic Signing Success
1. ✅ APK signing completes without errors
2. ✅ Device-specific certificate is generated
3. ✅ Signed APK is created successfully
4. ✅ Installation proceeds with signed APK
5. ✅ App functions normally after installation
6. ✅ Signature is unique per device

## 🔐 Security Testing

### Penetration Testing

**1. Static Analysis Resistance:**
```bash
# Try to find sensitive strings in APK
strings app-debug.apk | grep -iE "(secret|key|api|password|token)"

# Should return minimal or no results
```

**2. Dynamic Analysis Testing:**
```bash
# Monitor runtime string decryption
adb logcat | grep -E "(decrypt|cipher)" | head -20
```

**3. APK Tampering Test:**
- Modify signed APK
- Try to install modified version
- Should fail signature verification

### Performance Testing

**1. Build Time Impact:**
```bash
# Measure build time with encryption
time ./gradlew clean assembleDebug

# Compare with baseline build time
```

**2. Runtime Performance:**
- Monitor app startup time
- Check string decryption overhead
- Verify memory usage

**3. Installation Time:**
- Measure time from "Update" click to completion
- Monitor signing process duration

## 📊 Expected Results

### Build Output
```
> Task :app:encryptStrings
StringEncryptionTask: Starting string encryption process
Encrypted string: MySecretKey12345 -> ENCRYPTED_A1B2C3D4
Encrypted string: StringKey123456 -> ENCRYPTED_E5F6G7H8
Encrypted string: com.coderx.installer -> ENCRYPTED_I9J0K1L2
Generated EncryptedStrings.kt with 15 encrypted strings
StringEncryptionTask: String encryption completed

> Task :app:encryptAssets
AssetEncryptionTask: Starting encryption process
Encrypting with AES-GCM: app.apk
Successfully encrypted app.apk (2048576 -> 2048604 bytes)
AssetEncryptionTask: Encryption process completed

BUILD SUCCESSFUL in 45s
```

### Runtime Logs
```
D/InstallationActivity: Starting installation
D/AssetEncryption: Attempting to read encrypted asset: app.apk.enc
D/AssetEncryption: Read 2048604 bytes of encrypted data
D/DynamicApkSigner: Starting dynamic APK signing process
D/DynamicApkSigner: Generating device-specific certificate
D/DynamicApkSigner: Signing APK with generated certificate
D/DynamicApkSigner: APK signed successfully
I/InstallationActivity: Installing signed APK
```

This comprehensive testing approach ensures that both string encryption and dynamic APK signing work correctly and provide the intended security benefits.