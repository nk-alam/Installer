# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep encryption classes
-keep class com.coderx.installer.utils.AssetEncryption { *; }
-keep class com.coderx.installer.utils.SecurityUtils { *; }
-keep class com.coderx.installer.utils.EncryptedStrings { *; }
-keep class com.coderx.installer.utils.DynamicApkSigner { *; }
-keep class com.coderx.installer.utils.DynamicApkSigner$* { *; }

# Keep MainActivity methods called by receiver
-keep class com.coderx.installer.MainActivity {
    public void updateInstallProgress(java.lang.String);
    public void handleInstallationSuccess();
    public void handleInstallationError(java.lang.String);
}

# Keep InstallReceiver
-keep class com.coderx.installer.InstallReceiver { *; }

# Keep crypto classes
-keep class javax.crypto.** { *; }
-keep class javax.crypto.spec.** { *; }
-keep class java.security.** { *; }
-keep class java.security.cert.** { *; }
-keep class javax.security.auth.x500.** { *; }

# Keep coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Obfuscate string constants (helps hide encryption keys)
-adaptclassstrings
-optimizations !code/simplification/string
-obfuscationdictionary proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt

# Additional security obfuscation
-repackageclasses 'o'
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively