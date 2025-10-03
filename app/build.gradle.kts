@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.oxidelabmobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oxidelabmobile"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add support for custom build types
        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Используем современный DSL для задания jvmTarget
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        // Enable native build
        prefab = true
    }

    // Add external native build configuration
    // externalNativeBuild {
    //     cmake {
    //         path = file("src/main/rust/CMakeLists.txt")
    //         version = "3.22.1"
    //     }
    // }
}

// Task to build Rust code
tasks.register<Exec>("buildRust") {
    workingDir = File(projectDir, "src/main/rust")
    commandLine(
        "cargo",
        "ndk",
        "--target",
        "aarch64-linux-android",
        "--",
        "build",
    )
}

// Task to copy the built Rust library to jniLibs
tasks.register<Copy>("copyRustLib") {
    dependsOn("buildRust")
    from("src/main/rust/target/aarch64-linux-android/debug/liboxide_lab_mobile.so")
    into("src/main/jniLibs/arm64-v8a")
}

// Configure detekt
detekt {
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    // Set the correct JVM target for detekt
    // Allow build to pass even with issues
    ignoreFailures = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "11"
    // Allow build to pass even with issues
    ignoreFailures = true
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "11"
}

// Make sure the Rust library is built before compiling the Android app
tasks.whenTaskAdded {
    if (name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders") {
        dependsOn("copyRustLib")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Additional Compose dependencies that might be missing
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-util")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.9.5")

    // SplashScreen API для правильного отображения splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // OkHttp для стабильной HTTP загрузки моделей на Android
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Detekt dependencies
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
}
