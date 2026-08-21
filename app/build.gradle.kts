import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    kotlin("kapt")
}

val secrets = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) secretsFile.inputStream().use { load(it) }
}
fun secret(name: String): String = secrets.getProperty(name, "")

android {
    namespace = "com.ecomap.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ecomap.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPBOX_ACCESS_TOKEN"] = secret("MAPBOX_ACCESS_TOKEN")
        buildConfigField("String", "MAPBOX_TOKEN", "\"${secret("MAPBOX_ACCESS_TOKEN")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${secret("GEMINI_API_KEY")}\"")
        buildConfigField("boolean", "USE_MONGO", secret("USE_MONGO").ifBlank { "false" })
        buildConfigField("String", "MONGO_API_KEY", "\"${secret("MONGO_API_KEY")}\"")
        buildConfigField("String", "MONGO_URL", "\"${secret("MONGO_URL")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.playServicesLocationLib)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    // Mapbox requiere un MAPBOX_DOWNLOADS_TOKEN real para resolver su repositorio privado.
    if (secret("MAPBOX_DOWNLOADS_TOKEN").isNotBlank() && !secret("MAPBOX_DOWNLOADS_TOKEN").startsWith("REEMPLAZAR")) {
        implementation(libs.mapbox.maps)
        implementation(libs.mapbox.navigation)
    }
    implementation(libs.tflite)
    implementation(libs.tflite.support)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.generativeai)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.coil.compose)
    implementation("com.airbnb.android:lottie-compose:6.6.2")
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
}

kapt { correctErrorTypes = true }
