package com.oxidelabmobile

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import com.oxidelabmobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Интерфейс для отслеживания прогресса загрузки
 */
interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}

/**
 * ResponseBody с отслеживанием прогресса
 */
class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val progressListener: ProgressListener
) : ResponseBody() {

    private val bufferedSource: BufferedSource by lazy {
        source(responseBody.source()).buffer()
    }

    override fun contentType(): MediaType? = responseBody.contentType()

    override fun contentLength(): Long = responseBody.contentLength()

    override fun source(): BufferedSource = bufferedSource

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1L)
                return bytesRead
            }
        }
    }
}

/**
 * Класс для управления кэшем загруженных моделей
 */
class ModelCache(private val context: Context) {
    
    private val TAG = "ModelCache"
    private val PREFS_NAME = "ModelCachePrefs"
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Получает путь к папке models
     */
    private fun getModelsDirectory(): File {
        val rootDirectory = Environment.getExternalStorageDirectory()
        return File(rootDirectory, "models")
    }
    
    /**
     * Проверяет, загружена ли модель
     * @param modelId Уникальный идентификатор модели
     * @param fileName Имя файла модели
     * @return true если модель загружена и файл существует
     */
    fun isModelDownloaded(modelId: String, fileName: String): Boolean {
        val isMarkedAsDownloaded = sharedPreferences.getBoolean(modelId, false)
        val fileExists = isModelFileExists(fileName)
        
        Log.d(TAG, "Model $modelId: marked=$isMarkedAsDownloaded, fileExists=$fileExists")
        
        // Если файл не существует, но помечен как загруженный - очищаем кэш
        if (isMarkedAsDownloaded && !fileExists) {
            Log.w(TAG, "Model $modelId marked as downloaded but file doesn't exist, clearing cache")
            setModelDownloaded(modelId, false)
            return false
        }
        
        return isMarkedAsDownloaded && fileExists
    }
    
    /**
     * Проверяет существование файла модели
     * @param fileName Имя файла модели
     * @return true если файл существует
     */
    fun isModelFileExists(fileName: String): Boolean {
        val modelsDir = getModelsDirectory()
        val file = File(modelsDir, fileName)
        val exists = file.exists() && file.length() > 1024 * 1024 // больше 1MB
        Log.d(TAG, "File $fileName exists: $exists (size: ${file.length()} bytes)")
        return exists
    }
    
    /**
     * Помечает модель как загруженную
     * @param modelId Уникальный идентификатор модели
     * @param isDownloaded true если загружена, false если удалена
     */
    fun setModelDownloaded(modelId: String, isDownloaded: Boolean) {
        sharedPreferences.edit().putBoolean(modelId, isDownloaded).apply()
        Log.d(TAG, "Model $modelId marked as downloaded: $isDownloaded")
    }
    
    /**
     * Удаляет файл модели и очищает кэш
     * @param modelId Уникальный идентификатор модели
     * @param fileName Имя файла модели
     * @return true если файл был удален
     */
    fun deleteModel(modelId: String, fileName: String): Boolean {
        val modelsDir = getModelsDirectory()
        val file = File(modelsDir, fileName)
        
        return try {
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    setModelDownloaded(modelId, false)
                    Log.d(TAG, "Model $modelId file $fileName deleted successfully")
                } else {
                    Log.e(TAG, "Failed to delete model file: $fileName")
                }
                deleted
            } else {
                Log.w(TAG, "Model file $fileName doesn't exist")
                setModelDownloaded(modelId, false)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting model file: $fileName", e)
            false
        }
    }
    
    /**
     * Получает информацию о размере файла модели
     * @param fileName Имя файла модели
     * @return Размер файла в байтах или -1 если файл не существует
     */
    fun getModelFileSize(fileName: String): Long {
        val modelsDir = getModelsDirectory()
        val file = File(modelsDir, fileName)
        return if (file.exists()) file.length() else -1
    }
    
    /**
     * Сканирует папку models и синхронизирует кэш с реальными файлами
     */
    fun syncCacheWithFiles() {
        Log.d(TAG, "Syncing cache with files in models directory")
        
        val modelsDir = getModelsDirectory()
        if (!modelsDir.exists()) {
            Log.d(TAG, "Models directory doesn't exist, clearing all cache")
            sharedPreferences.edit().clear().apply()
            return
        }
        
        val files = modelsDir.listFiles()
        if (files == null) {
            Log.w(TAG, "Cannot list files in models directory")
            return
        }
        
        // Получаем все ключи из SharedPreferences
        val allKeys = sharedPreferences.all.keys
        
        // Проверяем каждый файл в кэше
        for (key in allKeys) {
            val isMarkedAsDownloaded = sharedPreferences.getBoolean(key, false)
            if (isMarkedAsDownloaded) {
                // Извлекаем имя файла из ключа (формат: "modelName/fileName")
                val fileName = key.substringAfterLast("/")
                val fileExists = files.any { it.name == fileName }
                
                Log.d(TAG, "Checking model $key -> fileName: $fileName, exists: $fileExists")
                
                if (!fileExists) {
                    Log.d(TAG, "File for model $key not found, clearing cache")
                    setModelDownloaded(key, false)
                }
            }
        }
        
        // Проверяем файлы, которые не помечены в кэше (обратная синхронизация)
        for (file in files) {
            val fileName = file.name
            val modelKey = "unsloth/Qwen3-0.6B-GGUF/$fileName" // Пока только для одной модели
            
            if (!allKeys.contains(modelKey)) {
                Log.d(TAG, "Found unmarked file: $fileName, adding to cache")
                setModelDownloaded(modelKey, true)
            }
        }
        
        Log.d(TAG, "Cache sync completed. Found ${files.size} files in models directory")
    }
}

/**
 * Kotlin HTTP клиент для загрузки моделей с Hugging Face
 * Стабилен на Android и не вызывает SIGABRT крашей
 */
class ModelDownloader(private val context: Context) {
    
    private val TAG = "ModelDownloader"
    private val modelCache = ModelCache(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Загружает модель с Hugging Face в папку models в корне устройства
     * @param modelName Имя модели (например, "unsloth/Qwen3-0.6B-GGUF")
     * @param fileName Имя файла модели (например, "Qwen3-0.6B-Q5_K_M.gguf")
     * @param progressListener Слушатель прогресса загрузки
     * @return Путь к загруженному файлу или null в случае ошибки
     */
    suspend fun downloadModel(
        modelName: String,
        fileName: String,
        progressListener: ProgressListener? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting download of model: $modelName/$fileName")
            
            // Создаем папку models в корне устройства
            val rootDirectory = Environment.getExternalStorageDirectory()
            val modelsDirectory = File(rootDirectory, "models")
            if (!modelsDirectory.exists()) {
                val created = modelsDirectory.mkdirs()
                if (created) {
                    Log.d(TAG, "Created models directory: ${modelsDirectory.absolutePath}")
                } else {
                    Log.e(TAG, "Failed to create models directory: ${modelsDirectory.absolutePath}")
                    return@withContext null
                }
            }
            
            // Формируем URL для загрузки модели через resolve (прямая ссылка на файл)
            val modelUrl = "https://huggingface.co/$modelName/resolve/main/$fileName"
            Log.d(TAG, "Downloading from URL: $modelUrl")
            
            // Создаем запрос
            val request = Request.Builder()
                .url(modelUrl)
                .addHeader("User-Agent", "OxideLabMobile/1.0")
                .build()
            
            // Выполняем запрос
            val response: Response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "HTTP request failed: ${response.code} ${response.message}")
                return@withContext null
            }
            
            // Получаем размер файла
            val contentLength = response.body?.contentLength() ?: -1
            Log.d(TAG, "Content length: $contentLength bytes")
            
            // Проверяем Content-Type для убеждения что это не HTML
            val contentType = response.header("Content-Type") ?: ""
            Log.d(TAG, "Content-Type: $contentType")
            
            if (contentType.contains("text/html")) {
                Log.e(TAG, "Received HTML instead of model file - URL may be incorrect")
                return@withContext null
            }
            
            // Создаем файл для сохранения в папке models
            val outputFile = File(modelsDirectory, fileName)
            Log.d(TAG, "Saving to file: ${outputFile.absolutePath}")
            
            // Создаем ProgressResponseBody если нужен прогресс
            val responseBody = if (progressListener != null) {
                ProgressResponseBody(response.body!!, progressListener)
            } else {
                response.body!!
            }
            
            // Загружаем и сохраняем файл
            responseBody.use { body ->
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(outputFile)
                
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    // Логируем прогресс каждые 10%
                    if (contentLength > 0 && totalBytesRead % (contentLength / 10) == 0L) {
                        val progress = (totalBytesRead * 100) / contentLength
                        Log.d(TAG, "Download progress: $progress%")
                    }
                }
                
                outputStream.close()
                inputStream.close()
            }
            
            Log.d(TAG, "Model downloaded successfully: ${outputFile.absolutePath}")
            Log.d(TAG, "File size: ${outputFile.length()} bytes")
            
            // Проверяем что файл не слишком маленький (модели обычно больше 1MB)
            val fileSize = outputFile.length()
            if (fileSize < 1024 * 1024) { // меньше 1MB
                Log.e(TAG, "Downloaded file is too small ($fileSize bytes) - may be corrupted or incomplete")
                outputFile.delete() // удаляем некорректный файл
                return@withContext null
            }
            
            // Помечаем модель как загруженную в кэше
            val modelId = "$modelName/$fileName"
            modelCache.setModelDownloaded(modelId, true)
            
            return@withContext outputFile.absolutePath
            
        } catch (e: IOException) {
            Log.e(TAG, "IO error during model download", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during model download", e)
            return@withContext null
        }
    }
    
    /**
     * Получает информацию о модели с Hugging Face
     * @param modelName Имя модели
     * @return Информация о модели или null в случае ошибки
     */
    suspend fun getModelInfo(modelName: String): ModelInfo? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting model info for: $modelName")
            
            val url = "https://huggingface.co/api/models/$modelName"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "OxideLabMobile/1.0")
                .build()
            
            val response: Response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to get model info: ${response.code}")
                return@withContext null
            }
            
            val json = response.body?.string()
            if (json != null) {
                Log.d(TAG, "Model info retrieved successfully")
                // Здесь можно парсить JSON для получения информации о модели
                return@withContext ModelInfo(modelName, json)
            }
            
            return@withContext null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting model info", e)
            return@withContext null
        }
    }
    
    /**
     * Проверяет доступность модели
     * @param modelName Имя модели
     * @param fileName Имя файла модели
     * @return true если модель доступна, false иначе
     */
    suspend fun isModelAvailable(modelName: String, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://huggingface.co/$modelName/blob/main/$fileName"
            val request = Request.Builder()
                .url(url)
                .head() // Используем HEAD запрос для проверки доступности
                .addHeader("User-Agent", "OxideLabMobile/1.0")
                .build()
            
            val response: Response = client.newCall(request).execute()
            val isAvailable = response.isSuccessful
            
            Log.d(TAG, "Model availability check: $modelName/$fileName = $isAvailable")
            return@withContext isAvailable
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking model availability", e)
            return@withContext false
        }
    }
    
    /**
     * Проверяет, загружена ли модель
     * @param modelName Имя модели (например, "unsloth/Qwen3-0.6B-GGUF")
     * @param fileName Имя файла модели (например, "Qwen3-0.6B-Q5_K_M.gguf")
     * @return true если модель загружена и файл существует
     */
    fun isModelDownloaded(modelName: String, fileName: String): Boolean {
        val modelId = "$modelName/$fileName"
        return modelCache.isModelDownloaded(modelId, fileName)
    }
    
    /**
     * Удаляет модель
     * @param modelName Имя модели (например, "unsloth/Qwen3-0.6B-GGUF")
     * @param fileName Имя файла модели (например, "Qwen3-0.6B-Q5_K_M.gguf")
     * @return true если модель была удалена
     */
    fun deleteModel(modelName: String, fileName: String): Boolean {
        val modelId = "$modelName/$fileName"
        return modelCache.deleteModel(modelId, fileName)
    }
    
    /**
     * Получает размер файла модели
     * @param fileName Имя файла модели
     * @return Размер файла в байтах или -1 если файл не существует
     */
    fun getModelFileSize(fileName: String): Long {
        return modelCache.getModelFileSize(fileName)
    }
    
    /**
     * Синхронизирует кэш с файлами в папке models
     */
    fun syncCacheWithFiles() {
        modelCache.syncCacheWithFiles()
    }
    
    /**
     * Получает список всех скачанных моделей
     * @return Список скачанных моделей
     */
    fun getDownloadedModels(): List<DownloadedModel> {
        val modelsDir = File(Environment.getExternalStorageDirectory(), "models")
        if (!modelsDir.exists()) {
            return emptyList()
        }
        
        val downloadedModels = mutableListOf<DownloadedModel>()
        
        // Получаем все файлы из папки models
        val files = modelsDir.listFiles { file ->
            file.isFile && file.extension.lowercase() in listOf("gguf", "bin", "safetensors")
        }
        
        files?.forEach { file ->
            val fileName = file.name
            val fileSize = file.length()
            
            // Определяем имя модели на основе имени файла
            val modelName = when {
                fileName.contains("qwen", ignoreCase = true) -> "Qwen3 0.6B"
                fileName.contains("gemma", ignoreCase = true) -> "Gemma 3 270M IT"
                else -> fileName.substringBeforeLast(".")
            }
            
            val modelId = "unsloth/$modelName"
            
            downloadedModels.add(
                DownloadedModel(
                    id = modelId,
                    name = modelName,
                    fileName = fileName,
                    size = fileSize,
                    iconResId = if (fileName.contains("qwen", ignoreCase = true)) R.drawable.qwen else R.drawable.gemma
                )
            )
        }
        
        return downloadedModels
    }
}

/**
 * Информация о модели
 */
data class ModelInfo(
    val name: String,
    val jsonData: String
)

/**
 * Информация о скачанной модели
 */
data class DownloadedModel(
    val id: String,
    val name: String,
    val fileName: String,
    val size: Long,
    val iconResId: Int = R.drawable.qwen
)
