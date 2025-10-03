package com.oxidelabmobile

import android.util.Log

@Suppress("unused")
class RustInterface private constructor() {
    companion object {
        private const val TAG = "RustInterface"

        private val INSTANCE = RustInterface()
        @Suppress("unused")
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
    external fun downloadQwenModel(): String
    external fun downloadQwenModelTo(cacheDir: String): String
    external fun downloadModelWithHttp(modelUrl: String, cacheDir: String): String
    external fun initializeModel(modelPath: String): String
    external fun runCandleExampleNative(): String

    @Suppress("unused")
    fun processMessageWithRust(message: String): String {
        return try {
            processMessage(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message with Rust", e)
            "Error: ${e.message}"
        }
    }

    @Suppress("unused")
    fun downloadQwenModelPath(): String {
        return try {
            downloadQwenModel()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model with Rust", e)
            "Error: ${e.message}"
        }
    }

    @Suppress("unused")
    fun downloadQwenModelToPath(cacheDir: String): String {
        Log.d(TAG, "Starting model download to cache directory: $cacheDir")

        // Validate cache directory parameter
        if (cacheDir.isBlank()) {
            Log.e(TAG, "Cache directory is empty or blank")
            return "Error: Cache directory cannot be empty"
        }

        return try {
            val result = downloadQwenModelTo(cacheDir)
            Log.d(TAG, "Model download completed with result: $result")
            result
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native library not found or not loaded", e)
            "Error: Native library not available"
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during model download", e)
            "Error: ${e.message ?: "Unknown error occurred"}"
        }
    }

    // Новый метод для загрузки модели через Kotlin HTTP клиент
    @Suppress("unused")
    fun downloadModelWithKotlinHttp(modelUrl: String, cacheDir: String): String {
        Log.d(TAG, "Starting Kotlin HTTP model download from: $modelUrl to: $cacheDir")

        return try {
            val result = downloadModelWithHttp(modelUrl, cacheDir)
            Log.d(TAG, "Kotlin HTTP model download completed with result: $result")
            result
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native library not found or not loaded", e)
            "Error: Native library not available"
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Kotlin HTTP model download", e)
            "Error: ${e.message ?: "Unknown error occurred"}"
        }
    }

    // Новый метод для инициализации модели из локального файла
    @Suppress("unused")
    fun initializeModelFromFile(modelPath: String): String {
        Log.d(TAG, "Initializing model from file: $modelPath")

        return try {
            val result = initializeModel(modelPath)
            Log.d(TAG, "Model initialization completed with result: $result")
            result
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native library not found or not loaded", e)
            "Error: Native library not available"
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during model initialization", e)
            "Error: ${e.message ?: "Unknown error occurred"}"
        }
    }

    // Метод для тестирования Candle
    @Suppress("unused")
    fun runCandleExample(): String {
        Log.d(TAG, "Running Candle example")

        return try {
            val result = runCandleExampleNative()
            Log.d(TAG, "Candle example completed with result: $result")
            result
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native library not found or not loaded", e)
            "Error: Native library not available"
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Candle example", e)
            "Error: ${e.message ?: "Unknown error occurred"}"
        }
    }
}
