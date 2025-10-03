package com.oxidelabmobile.ui.accessibility

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.pow
import com.oxidelabmobile.ui.theme.Dimensions

/**
 * Accessibility utilities for Oxide Lab Mobile
 * Ensures WCAG 2.1 AA compliance and proper TalkBack support
 */

/**
 * Ensures minimum touch target size of 48dp for accessibility
 */
fun Modifier.minimumTouchTarget(): Modifier = this.size(Dimensions.TouchTargetMin)

/**
 * Adds semantic content description for TalkBack
 */
fun Modifier.accessibilityDescription(description: String): Modifier =
    this.semantics { contentDescription = description }

/**
 * Checks if color contrast meets WCAG AA standards
 */
@Composable
@Suppress("unused")
fun isAccessibleContrast(foreground: androidx.compose.ui.graphics.Color, background: androidx.compose.ui.graphics.Color): Boolean {
    // Simplified contrast check - in production, use proper contrast calculation
    val foregroundLuminance = calculateLuminance(foreground)
    val backgroundLuminance = calculateLuminance(background)

    val contrast = (maxOf(foregroundLuminance, backgroundLuminance) + 0.05) /
                  (minOf(foregroundLuminance, backgroundLuminance) + 0.05)

    return contrast >= 4.5 // WCAG AA standard for normal text
}

/**
 * Calculate relative luminance of a color
 */
private fun calculateLuminance(color: androidx.compose.ui.graphics.Color): Double {
    val r = if (color.red <= 0.03928) color.red / 12.92 else
            (color.red + 0.055).pow(2.4) / 1.055.pow(2.4)
    val g = if (color.green <= 0.03928) color.green / 12.92 else
            (color.green + 0.055).pow(2.4) / 1.055.pow(2.4)
    val b = if (color.blue <= 0.03928) color.blue / 12.92 else
            (color.blue + 0.055).pow(2.4) / 1.055.pow(2.4)

    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/**
 * Accessibility-focused color scheme that ensures proper contrast
 */
@Suppress("unused")
object AccessibleColors {
    val HighContrastPrimary = androidx.compose.ui.graphics.Color(0xFF1976D2)
    val HighContrastOnPrimary = androidx.compose.ui.graphics.Color.White
    val HighContrastSurface = androidx.compose.ui.graphics.Color(0xFFF5F5F5)
    val HighContrastOnSurface = androidx.compose.ui.graphics.Color(0xFF212121)
    val HighContrastError = androidx.compose.ui.graphics.Color(0xFFD32F2F)
    val HighContrastOnError = androidx.compose.ui.graphics.Color.White
}

/**
 * Common accessibility descriptions for UI elements
 */
@Suppress("unused")
object AccessibilityDescriptions {
    const val SEND_MESSAGE = "Send message"
    const val COPY_MESSAGE = "Copy message"
    const val CONTINUE_CONVERSATION = "Continue conversation"
    const val RATE_RESPONSE = "Rate AI response"
    const val TOGGLE_THINKING_MODE = "Toggle thinking mode"
    const val MODEL_SETTINGS = "Model settings"
    const val CLOSE_SETTINGS = "Close settings"
    const val EXPAND_THINKING_PROCESS = "Expand thinking process"
    const val COLLAPSE_THINKING_PROCESS = "Collapse thinking process"
    const val BACK_TO_CHAT = "Back to chat"
    const val SELECT_MODEL = "Select AI model"
    const val CANCEL_LOADING = "Cancel model loading"
    const val START_WORKING = "Start working with model"
    const val ONLINE_STATUS = "Online"
    const val OFFLINE_STATUS = "Offline"
    const val CODE_DETECTED = "Code detected - using monospace font"
    const val PROGRESS_INDICATOR = "Loading progress"
    const val THINKING_ANIMATION = "AI is thinking"
}

/**
 * Accessibility modifiers for common UI patterns
 */
@Suppress("unused")
object AccessibilityModifiers {
    fun Modifier.sendButton(): Modifier =
        this.minimumTouchTarget()
            .accessibilityDescription(AccessibilityDescriptions.SEND_MESSAGE)

    fun Modifier.copyButton(): Modifier =
        this.minimumTouchTarget()
            .accessibilityDescription(AccessibilityDescriptions.COPY_MESSAGE)

    fun Modifier.settingsButton(): Modifier =
        this.minimumTouchTarget()
            .accessibilityDescription(AccessibilityDescriptions.MODEL_SETTINGS)

    fun Modifier.closeButton(): Modifier =
        this.minimumTouchTarget()
            .accessibilityDescription(AccessibilityDescriptions.CLOSE_SETTINGS)

    fun Modifier.toggleButton(description: String): Modifier =
        this.minimumTouchTarget()
            .accessibilityDescription(description)

    fun Modifier.statusIndicator(isOnline: Boolean): Modifier =
        this.accessibilityDescription(
            if (isOnline) AccessibilityDescriptions.ONLINE_STATUS
            else AccessibilityDescriptions.OFFLINE_STATUS
        )
}
