plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
}

android {
    namespace = "com.coderx.myinstaller"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.coderx.myinstaller"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
            
            // Fix BouncyCastle packaging conflicts
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "/META-INF/versions/*/OSGI-INF/MANIFEST.MF"
            excludes += "/META-INF/MANIFEST.MF"
            excludes += "/OSGI-INF/MANIFEST.MF"
            
            // Additional BouncyCastle exclusions
            excludes += "/META-INF/BC1024KE.SF"
            excludes += "/META-INF/BC1024KE.DSA"
            excludes += "/META-INF/BC2048KE.SF"
            excludes += "/META-INF/BC2048KE.DSA"
            
            // JSpecify exclusions
            excludes += "/META-INF/jspecify_annotations.kotlin_module"
            
            // Merge duplicate files instead of excluding if needed
            pickFirsts += "/META-INF/services/java.security.Provider"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.fragment:fragment-ktx:1.8.8")

    // For APK signing (BouncyCastle) - Using specific versions to avoid conflicts
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1") {
        exclude(group = "org.jspecify", module = "jspecify")
    }
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1") {
        exclude(group = "org.jspecify", module = "jspecify")
    }

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // JSON handling
    implementation("com.google.code.gson:gson:2.11.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // CardView
    implementation("androidx.cardview:cardview:1.0.0")
}