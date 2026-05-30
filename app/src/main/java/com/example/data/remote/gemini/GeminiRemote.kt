package com.example.data.remote.gemini

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header
import java.util.concurrent.TimeUnit

// --- OpenAI / GapGPT API Data Classes ---

@JsonClass(generateAdapter = true)
data class OpenAiMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val type: String
)

@JsonClass(generateAdapter = true)
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    @com.squareup.moshi.Json(name = "response_format") val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiChoiceMessage(
    val content: String?
)

@JsonClass(generateAdapter = true)
data class OpenAiChoice(
    val message: OpenAiChoiceMessage?
)

@JsonClass(generateAdapter = true)
data class OpenAiChatResponse(
    val choices: List<OpenAiChoice>?
)

interface GeminiApiService {
    @POST("chat/completions")
    suspend fun generateContent(
        @Header("Authorization") authHeader: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://api.gapgpt.app/v1/"
    private const val CHAT_MODEL = "gpt-4o"
    // WARNING: Hardcoded API key is a security risk. Move this to the Secrets panel in AI Studio.
    private const val HARDCODED_API_KEY = "sk-kU0MCOfMnqOj3i23x2w1SX7IbQtvrZLTh9MuexEwETCGlnVl"

    private val apiKey: String
        get() = HARDCODED_API_KEY

    val isApiKeyAvailable: Boolean
        get() = HARDCODED_API_KEY.isNotEmpty()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Executes a chat/instruction query with GPT-4o custom OpenAI endpoint and returns the text response.
     */
    suspend fun queryGemini(prompt: String, systemInstruction: String? = null, returnJson: Boolean = false): String {
        Log.d(TAG, "queryGemini dynamic prompt: $prompt")
        
        val messages = mutableListOf<OpenAiMessage>()
        if (!systemInstruction.isNullOrEmpty()) {
            messages.add(OpenAiMessage(role = "system", content = systemInstruction))
        }
        messages.add(OpenAiMessage(role = "user", content = prompt))

        val responseFormat = if (returnJson) ResponseFormat(type = "json_object") else null

        val request = OpenAiChatRequest(
            model = CHAT_MODEL,
            messages = messages,
            responseFormat = responseFormat,
            temperature = if (returnJson) 0.1f else 0.7f
        )

        val authHeader = "Bearer $apiKey"

        return try {
            val response = apiService.generateContent(authHeader, request)
            response.choices?.firstOrNull()?.message?.content ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "GapGPT API call failed", e)
            "ERROR: ${e.localizedMessage}"
        }
    }
}
