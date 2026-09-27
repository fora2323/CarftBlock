plugins {
    id("com.android.application")
}

android {
    namespace = "com.aurora"
    compileSdk = 37
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.aurora"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }

        externalNativeBuild {
            cmake {
                cppFlags '-std=c++17 -fexceptions -frtti'
                arguments '-DANDROID_STL=c++_shared'
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
            version = "3.31.0+"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../key-store-aurora.jks")
            storePassword = "aurora23"
            keyAlias = "Aurora"
            keyPassword = "aurora23"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildFeatures {
        prefab = true
    }
}

dependencies {
    implementation("dev.rikka.rikkax.core:core-ktx:1.4.1")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    implementation("com.google.android.material:material:1.14.0")
    
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    
    implementation("io.github.vvb2060.ndk:boringssl:20250114")
    implementation("org.conscrypt:conscrypt-android:2.5.2")
    implementation("org.lsposed.libcxx:libcxx:27.0.12077973")
}