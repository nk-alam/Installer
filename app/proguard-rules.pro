# Add project specific ProGuard rules here.

# Keep BouncyCastle classes
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep APK installer classes
-keep class com.apkinstaller.store.** { *; }

# Keep model classes and their members
-keepclassmembers class com.apkinstaller.store.model.** {
    *;
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep ViewBinding classes
-keep class * extends androidx.viewbinding.ViewBinding {
    *;
}

# Keep Gson related classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Keep Lottie animations
-keep class com.airbnb.lottie.** { *; }

# Keep coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Apache Commons IO
-keep class org.apache.commons.io.** { *; }
-dontwarn org.apache.commons.io.**