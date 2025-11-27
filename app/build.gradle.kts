plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    id("com.google.gms.google-services")
    id("kotlin-kapt")

}

android {
    namespace = "com.example.lingogo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lingogo"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.activity.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


    // BOM de Firebase (Bill of Materials) - Versión estable
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

// Common (necesaria para KTX)
    implementation("com.google.firebase:firebase-common")

// Autenticación de Firebase
    implementation("com.google.firebase:firebase-auth")

// Base de datos Firestore
    implementation("com.google.firebase:firebase-firestore")
    // --- ¡NUEVO! Dependencia de Firebase Storage (para archivos/fotos) ---
    implementation("com.google.firebase:firebase-storage")

// --- ¡NUEVO! Biblioteca Glide (para cargar imágenes desde una URL) ---
    implementation("com.github.bumptech.glide:glide:4.16.0")

// Servicio de Autenticación de Google (para Google Sign-In) - Versión estable
    implementation("com.google.android.gms:play-services-auth:21.2.0")

// Analytics (Opcional pero recomendado)
    implementation("com.google.firebase:firebase-analytics")
// Room (Base de Datos)
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

// ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

// DataStore (Configuraciones)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

}