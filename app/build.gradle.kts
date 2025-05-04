plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.finalyearproject"
    compileSdk = 35
    packaging {
        resources.excludes.add("META-INF/INDEX.LIST")
        resources.excludes.add("META-INF/DEPENDENCIES")

    }


    defaultConfig {
        applicationId = "com.example.finalyearproject"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.firestore.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation (libs.androidx.material.icons.extended)
    implementation ("com.google.firebase:firebase-auth:23.2.0")
    // WorkManager for background tasks
    implementation (libs.androidx.work.runtime.ktx)
    implementation ("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")
// Notification support
    implementation (libs.androidx.core.ktx)
    implementation ("com.google.api-client:google-api-client-android:1.35.0")
    implementation ("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20240521-2.0.0")
    implementation ("com.google.http-client:google-http-client-gson:1.44.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")
    implementation ("com.google.auth:google-auth-library-oauth2-http:1.23.0")
    implementation ("com.google.accompanist:accompanist-swiperefresh:0.29.1-alpha")
    implementation ("androidx.compose.foundation:foundation:1.8.0")  // Or the latest stable version
    implementation ("androidx.compose.material3:material3:1.3.2")
    implementation ("com.google.firebase:firebase-messaging:24.1.1")
    implementation ("androidx.work:work-runtime-ktx:2.10.1")
}