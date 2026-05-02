plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)   // Required for kotlin { } block inside android { }
    alias(libs.plugins.kotlin.compose)   // Compose Compiler plugin
    alias(libs.plugins.ksp)              // Kotlin Symbol Processing (for Room + Hilt code generation)
    alias(libs.plugins.hilt)             // Hilt Dependency Injection plugin
}

android {
    namespace = "com.example.markettracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.markettracker"
        minSdk = 24
        targetSdk = 36
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
    kotlin {
        jvmToolchain(11)
    }

    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.animation)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.compose.shimmer)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.ui.graphics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.coil.compose)
    implementation(libs.logging.interceptor)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

// Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

// Lottie
    implementation(libs.lottie.compose)

// Navigation
    implementation(libs.androidx.navigation.compose)

// Hilt - Dependency Injection
// hilt-android: core Hilt runtime (annotations + component setup)
    implementation(libs.hilt.android)
// hilt-compiler: KSP processor that generates Hilt component classes at compile time
    ksp(libs.hilt.compiler)
// hilt-navigation-compose: provides hiltViewModel() for use inside @Composable functions
    implementation(libs.hilt.navigation.compose)

// Unit Testing
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}