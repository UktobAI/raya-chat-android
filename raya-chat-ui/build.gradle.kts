plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ai.teammates.rayachat.ui"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    // Core module (brings in models + serialization transitively)
    api(project(":raya-chat-core"))

    // Serialization — needed for deserializing attachments JSON in MessageBubble
    implementation(libs.serialization.json)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)
    implementation(libs.compose.activity)

    // Image loading
    implementation(libs.coil.compose)

    // Markdown
    implementation(libs.markwon.core)

    // Lifecycle
    implementation(libs.lifecycle.runtime)

    // Material (for BottomSheetDialogFragment)
    implementation("com.google.android.material:material:1.12.0")

    // Fragment KTX (for bundleOf)
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
