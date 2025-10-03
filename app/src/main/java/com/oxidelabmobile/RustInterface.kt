package com.oxidelabmobile

import android.util.Log

class RustInterface private constructor() {
    companion object {
        private const val TAG = "RustInterface"

        private val INSTANCE = RustInterface()
        val instance: RustInterface
            get() = INSTANCE

        init {
            try {
                System.loadLibrary("oxide_lab_mobile")
                Log.d(TAG, "Rust library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load Rust library", e)
            }
        }
    }

    // Native method declarations
    external fun processMessage(message: String): String

    fun processMessageWithRust(message: String): String {
        return try {
            processMessage(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message with Rust", e)
            "Error: ${e.message}"
        }
    }
}
