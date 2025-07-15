plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Register the asset encryption task
val encryptAssetsTask = tasks.register<AssetEncryptionTask>("encryptAssets") {
    inputDir.set(file("src/main/assets"))
    outputDir.set(file("build/encrypted-assets"))
}

// Register the string encryption task
val encryptStringsTask = tasks.register<StringEncryptionTask>("encryptStrings") {
    inputDir.set(file("src/main/java"))
    outputDir.set(file("build/encrypted-src"))
}

android {
    namespace = "com.coderx.installer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.coderx.installer"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = false
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
    }
    
    sourceSets {
        getByName("main") {
            // Replace original assets with encrypted ones
            assets.srcDirs("build/encrypted-assets")
            // Replace original source with encrypted strings
            java.srcDirs("build/encrypted-src")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

// Make sure assets are encrypted before building
tasks.named("preBuild") {
    dependsOn(encryptAssetsTask)
    dependsOn(encryptStringsTask)
}

// Also run before generating resources
tasks.whenTaskAdded {
    if (name.contains("generate") && name.contains("Resources")) {
        dependsOn(encryptAssetsTask)
        dependsOn(encryptStringsTask)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}