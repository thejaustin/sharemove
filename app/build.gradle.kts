plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace   = "com.thejaustin.sharemove"
    compileSdk  = 35

    defaultConfig {
        applicationId   = "com.thejaustin.sharemove"
        minSdk          = 31
        targetSdk       = 35
        versionCode     = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName     = (findProperty("versionName") as String?) ?: "1.0.0-alpha.dev"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
        release {
            isMinifyEnabled     = true
            isShrinkResources   = true
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

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.prev)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.activity)
    implementation(libs.compose.animation)

    implementation(libs.androidx.core.ktx)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    implementation(libs.datastore)
    implementation(libs.coroutines)
    implementation(libs.dadb)

    debugImplementation(libs.compose.ui.tooling)
}
