package com.oxidelabmobile

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

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

    // Sealed class для определения типов моделей
    sealed class ModelType(val id: Int) {
        object Qwen3 : ModelType(0)
        object Gemma3 : ModelType(1)
    }

    // Data class для конфигурации генерации текста
    data class GenerationConfig(
        val maxTokens: Int = 512,
        val temperature: Float = 0.7f,
        val topP: Float = 0.9f,
        val repeatPenalty: Float = 1.1f
    )

    // Интерфейс для обратных вызовов при потоковой генерации
    interface StreamCallback {
        fun onToken(token: String)
        fun onComplete()
        fun onError(error: String)
    }

    // Native method declarations
    external fun loadModel(modelType: Int, variant: String)
    external fun loadModelFromPath(modelPath: String)
    external fun switchModel(modelType: Int, variant: String)
    external fun generateText(prompt: String, config: GenerationConfig, callback: StreamCallback?): String
    external fun unloadModel()
    external fun stopGeneration()

    // Suspend функция для асинхронной загрузки модели
    suspend fun loadModelSuspend(modelType: ModelType, variant: String) = withContext(Dispatchers.IO) {
        try {
            loadModel(modelType.id, variant)
            Log.d(TAG, "${modelType.toPrettyString()} model loaded successfully, variant: $variant")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading ${modelType.toPrettyString()} model, variant: $variant", e)
            throw e
        }
    }

    // Suspend функция для загрузки модели из файла
    suspend fun loadModelFromPathSuspend(modelPath: String) = withContext(Dispatchers.IO) {
        try {
            loadModelFromPath(modelPath)
            Log.d(TAG, "Model loaded successfully from path: $modelPath")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model from path: $modelPath", e)
            throw e
        }
    }

    // Suspend функция для асинхронного переключения модели
    suspend fun switchModelSuspend(modelType: ModelType, variant: String) = withContext(Dispatchers.IO) {
        try {
            switchModel(modelType.id, variant)
            Log.d(TAG, "${modelType.toPrettyString()} model switched successfully, variant: $variant")
        } catch (e: Exception) {
            Log.e(TAG, "Error switching to ${modelType.toPrettyString()} model, variant: $variant", e)
            throw e
        }
    }

    // Suspend функция для выгрузки модели из памяти
    suspend fun unloadModelSuspend() = withContext(Dispatchers.IO) {
        try {
            unloadModel()
            Log.d(TAG, "Model unloaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading model", e)
            throw e
        }
    }

    // Suspend функция для потоковой генерации текста
    suspend fun generateTextStreaming(
        prompt: String,
        config: GenerationConfig = GenerationConfig(),
        onToken: (String) -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val callback = object : StreamCallback {
            override fun onToken(token: String) {
                onToken(token)
            }
            override fun onComplete() {
                onComplete()
            }
            override fun onError(error: String) {
                onError(error)
            }
        }
        try {
            val result = generateText(prompt, config, callback)
            Log.d(TAG, "Text generation completed. Result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error during text generation", e)
            onError(e.message ?: "Unknown error")
            throw e
        }
    }

    // Suspend функция для синхронной генерации текста (без потока)
    suspend fun generateTextSync(
        prompt: String,
        config: GenerationConfig = GenerationConfig()
    ): String = withContext(Dispatchers.IO) {
        try {
            val result = generateText(prompt, config, null) // Передаем null для callback
            Log.d(TAG, "Synchronous text generation completed. Result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error during synchronous text generation", e)
            throw e
        }
    }

    // Полноценные функции для работы с моделями

    /**
     * Скачивает и загружает модель Qwen3 в указанную директорию.
     * Возвращает путь к загруженной модели или ошибку.
     */
    fun downloadQwenModelToPath(cacheDir: String): String {
        try {
            // В реальной реализации здесь должна быть логика скачивания модели
            // Пока возвращаем заглушку, но без deprecated предупреждения
            Log.i(TAG, "downloadQwenModelToPath called with cacheDir: $cacheDir")
            return "Error: Model downloading not implemented yet"
        } catch (e: Exception) {
            Log.e(TAG, "Error in downloadQwenModelToPath", e)
            return "Error: ${e.message}"
        }
    }

    /**
     * Инициализирует модель из файла по указанному пути.
     * Токенизатор извлекается из метаданных GGUF файла.
     * Возвращает результат инициализации.
     */
    suspend fun initializeModelFromFileSuspend(modelPath: String): String = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "initializeModelFromFile called with modelPath: $modelPath")

            // Загружаем модель из файла (токенизатор извлекается из GGUF метаданных)
            loadModelFromPathSuspend(modelPath)
            "Success: Model loaded from $modelPath"
        } catch (e: Exception) {
            Log.e(TAG, "Error in initializeModelFromFile", e)
            "Error: Failed to load model - ${e.message}"
        }
    }

    /**
     * Синхронная версия initializeModelFromFile для обратной совместимости.
     */
    fun initializeModelFromFile(modelPath: String): String {
        return runBlocking { initializeModelFromFileSuspend(modelPath) }
    }

    /**
     * Выполняет пример генерации текста с использованием загруженной модели.
     */
    suspend fun runCandleExampleSuspend(): String = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "runCandleExample called")

            // Выполняем генерацию текста
            generateTextSync(
                prompt = "Write a short poem about artificial intelligence:",
                config = GenerationConfig(
                    maxTokens = 100,
                    temperature = 0.8f,
                    topP = 0.9f,
                    repeatPenalty = 1.1f
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in runCandleExample", e)
            "Error: ${e.message}"
        }
    }

    /**
     * Синхронная версия runCandleExample для обратной совместимости.
     */
    fun runCandleExample(): String {
        return runBlocking { runCandleExampleSuspend() }
    }

    // Suspend функция для остановки генерации текста
    suspend fun stopGenerationSuspend() = withContext(Dispatchers.IO) {
        try {
            stopGeneration()
            Log.d(TAG, "Generation stop requested successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping generation", e)
            throw e
        }
    }

    // Синхронная версия stopGeneration для обратной совместимости
    fun stopGenerationSync() {
        return runBlocking { stopGenerationSuspend() }
    }

    private fun ModelType.toPrettyString(): String {
        return when (this) {
            ModelType.Qwen3 -> "Qwen3"
            ModelType.Gemma3 -> "Gemma3"
        }
    }
}







