package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Recommended models per guidelines
    const val MODEL_IMAGE = "gemini-3.1-flash-image-preview"
    const val MODEL_MUSIC_CLIP = "lyria-3-clip-preview"
    const val MODEL_MUSIC_PRO = "lyria-3-pro-preview"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun generateOrEditImage(
        prompt: String,
        baseImageBitmap: Bitmap? = null,
        aspectRatio: String = "1:1"
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add your key in the AI Studio Secrets panel.")
            )
        }

        try {
            val parts = mutableListOf<GeminiPart>()
            parts.add(GeminiPart(text = prompt))

            if (baseImageBitmap != null) {
                val stream = ByteArrayOutputStream()
                baseImageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64)))
            }

            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = parts)),
                generationConfig = GeminiGenerationConfig(
                    responseModalities = listOf("TEXT", "IMAGE"),
                    imageConfig = GeminiImageConfig(aspectRatio = aspectRatio, imageSize = "1K")
                )
            )

            val response = apiService.generateContent(
                model = MODEL_IMAGE,
                apiKey = apiKey,
                request = request
            )

            val candidate = response.candidates?.firstOrNull()
            val imagePart = candidate?.content?.parts?.firstOrNull { it.inlineData != null }

            if (imagePart?.inlineData != null) {
                val imageBytes = Base64.decode(imagePart.inlineData.data, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap != null) {
                    Result.success(bitmap)
                } else {
                    Result.failure(IllegalStateException("Failed to decode generated image bytes"))
                }
            } else {
                val textResponse = candidate?.content?.parts?.firstOrNull { it.text != null }?.text
                Result.failure(IllegalStateException(textResponse ?: "No image returned by model"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateMusic(
        prompt: String,
        isShortClip: Boolean = true,
        context: Context
    ): Result<File> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add your key in the AI Studio Secrets panel.")
            )
        }

        val model = if (isShortClip) MODEL_MUSIC_CLIP else MODEL_MUSIC_PRO

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseModalities = listOf("AUDIO")
                )
            )

            val response = apiService.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )

            val candidate = response.candidates?.firstOrNull()
            val audioPart = candidate?.content?.parts?.firstOrNull { it.inlineData != null }

            if (audioPart?.inlineData != null) {
                val audioBytes = Base64.decode(audioPart.inlineData.data, Base64.DEFAULT)
                val extension = when {
                    audioPart.inlineData.mimeType.contains("wav") -> "wav"
                    audioPart.inlineData.mimeType.contains("mp3") -> "mp3"
                    audioPart.inlineData.mimeType.contains("mpeg") -> "mp3"
                    else -> "mp3"
                }

                val musicFile = File(context.cacheDir, "lyria_${UUID.randomUUID()}.$extension")
                FileOutputStream(musicFile).use { it.write(audioBytes) }
                Result.success(musicFile)
            } else {
                val textResponse = candidate?.content?.parts?.firstOrNull { it.text != null }?.text
                Result.failure(IllegalStateException(textResponse ?: "No music audio returned by Lyria model"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
