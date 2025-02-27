plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.copypastemanager" // Assure-toi que c'est le bon namespace
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.copypastemanager" // Doit correspondre au namespace
        minSdk = 33
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true // Cette ligne est CRUCIALE
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)

    // Ajoute cette dépendance dans ton bloc "dependencies" de build.gradle.kts:
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

// Ajoute aussi celle-ci si tu utilises LiveData:
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

// Si tu utilises des coroutines explicitement, ajoute aussi:
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}