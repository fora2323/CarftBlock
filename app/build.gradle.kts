import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
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
        versionName = "1.0-alpha2"

        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86"))
        }

        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=none")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
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
        
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JVM_17)
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildFeatures {
        prefab = true
        compose = true 
    }

    applicationVariants.all {
        val variant = this
        val commitHash = try {
            Runtime.getRuntime().exec("git rev-parse --short HEAD").inputStream.reader().readText().trim()
        } catch (e: Exception) {
            "unknown"
        }
        
        variant.outputs.all {
            val output = this as BaseVariantOutputImpl
            output.outputFileName = "app-${variant.buildType.name}-$commitHash.apk"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    
    implementation("com.google.android.material:material:1.14.0")
    
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.80")
    
    implementation("io.github.vvb2060.ndk:boringssl:20250114")
    
    implementation("com.airbnb.android:lottie:6.7.1")
    
    implementation("org.lsposed.libcxx:libcxx:27.0.12077973")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    
    compileOnly("dev.rikka.hidden:stub:4.4.0")
    
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3") 
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
}