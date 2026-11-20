plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.neos.weatherservice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.neos.weatherservice"
        minSdk = 21
        targetSdk = 35
        versionCode = 200
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        // See lint.xml: newer AndroidX releases require Android 6.0+.
        lintConfig = file("lint.xml")
        // The LiveData check bundled with lifecycle 2.9 crashes this lint version; no LiveData is used.
        disable += "NullSafeMutableLiveData"
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    // Android's org.json is a stub in local unit tests; use the real one there.
    testImplementation("org.json:json:20240303")
}
