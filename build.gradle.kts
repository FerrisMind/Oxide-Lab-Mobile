// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Add plugins for Kotlin linting
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
}

// Configure ktlint globally
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        outputToConsole.set(true)
        ignoreFailures.set(true) // Allow build to pass even with ktlint issues
        enableExperimentalRules.set(true)
        // Removed deprecated disabledRules property
        // ktlint will automatically use .editorconfig from project root
        
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        }
    }
    
    // Configure detekt globally
    apply(plugin = "io.gitlab.arturbosch.detekt")
    
    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        ignoreFailures = true // Allow build to pass even with detekt issues
    }
}