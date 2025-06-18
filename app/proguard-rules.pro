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

# BouncyCastle ProGuard rules
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**
-dontwarn java.awt.**
-dontwarn sun.security.**

# Keep JCA provider classes
-keep class * extends java.security.Provider

# Keep certificate and key classes
-keep class * extends java.security.cert.Certificate
-keep class * extends java.security.Key
-keep class * extends java.security.PublicKey
-keep class * extends java.security.PrivateKey

# Keep signature classes
-keep class * extends java.security.Signature

# Prevent obfuscation of BouncyCastle provider registration
-keepnames class org.bouncycastle.jce.provider.BouncyCastleProvider
-keepnames class org.bouncycastle.jcajce.provider.config.ProviderConfiguration