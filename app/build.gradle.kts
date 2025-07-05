plugins {
    alias(libs.plugins.android.application) // Your existing plugin
    alias(libs.plugins.kotlin.android)    // Your existing plugin
    alias(libs.plugins.kotlin.compose)    // Your existing plugin
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" // KSP for Room
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
}

android {
    namespace = "com.MyApps.myprescription"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.MyApps.myprescription"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "22.06.25"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // It's good practice to define vector drawable support
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            // FIX: Enabled minification for security and smaller app size.
            isMinifyEnabled = true
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
        compose = true
    }
    // Compose options are often configured here as well, if not using the compose plugin directly
    // composeOptions {
    //     kotlinCompilerExtensionVersion = "1.5.11" // Ensure this matches your Compose BOM
    // }
    packagingOptions { // Added packagingOptions, often needed with multiple libraries
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.google.api-client:google-api-client-android:1.30.9")
    implementation("com.google.apis:google-api-services-drive:v3-rev136-1.25.0")
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.http-client:google-http-client-gson:1.47.1")
    implementation("com.google.http-client:google-http-client:1.47.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")


    // FIX: ADD the new Credential Manager libraries
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    // Coroutines for background tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation(libs.googleid)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Room Persistence Library
    val roomVersion = "2.6.1" // Ensure this is the latest stable version
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion") // KSP for Room's annotation processor

    // FIX: Added Room KTX for Kotlin Extensions and Coroutines support
    implementation("androidx.room:room-ktx:$roomVersion")

    // Android Core KTX
    implementation("androidx.core:core-ktx:1.13.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}