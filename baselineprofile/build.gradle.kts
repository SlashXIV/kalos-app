// Génération du baseline profile et mesure du démarrage de :app.
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.kalos.app.baselineprofile"
    compileSdk = 35
    targetProjectPath = ":app"

    defaultConfig {
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Mesure sur émulateur acceptée : seul l'écart sans / avec profil y compte (la vraie se fait sur téléphone).
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

baselineProfile {
    // Sur l'émulateur branché (ANDROID_SERIAL=emulator-5554), pas sur un appareil géré par Gradle.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.junit.ext)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
