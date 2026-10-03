plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.s15.codectest"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.s15.codectest"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/s15codectest.jks")
            storePassword = "s15codectest"
            keyAlias = "s15codectest"
            keyPassword = "s15codectest"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // MediaPlayer can only play audio files from assets if they are stored uncompressed.
    androidResources {
        noCompress += listOf("flac")
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.core)
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)

    testImplementation(libs.junit)
}
