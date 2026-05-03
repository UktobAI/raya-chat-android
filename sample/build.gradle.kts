import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val sampleToken: String = run {
    val props = Properties()
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
    props.getProperty("rayaChatSampleToken")
        ?: System.getenv("RAYA_CHAT_SAMPLE_TOKEN")
        ?: ""
}

android {
    namespace = "ai.teammates.rayachat.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "ai.teammates.rayachat.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "SAMPLE_TOKEN", "\"$sampleToken\"")
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
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":raya-chat-ui"))
    // Core module directly — for headless demo (Mode 4)
    implementation(project(":raya-chat-core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.activity)
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)

    // Coil for image loading (headless demo)
    implementation(libs.coil.compose)

    // Serialization (headless demo — parsing attachments JSON)
    implementation(libs.serialization.json)

    // AppCompat for Fragment demo (Mode 2)
    implementation("androidx.appcompat:appcompat:1.7.0")
    // Material for AppCompat theme
    implementation("com.google.android.material:material:1.12.0")
}
