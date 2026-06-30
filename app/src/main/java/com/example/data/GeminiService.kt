package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// --- Retrofit API Interface ---
interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Moshi/Retrofit Request/Response Models ---
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<Tool>? = null,
    val systemInstruction: Content? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String
)

data class Tool(
    val googleSearch: Map<String, Any>? = null,
    val googleMaps: Map<String, Any>? = null
)

data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null,
    val thinkingConfig: ThinkingConfig? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null,
    val speechConfig: SpeechConfig? = null
)

data class ResponseFormat(
    val responseMimeType: String,
    val responseSchema: Map<String, Any>? = null
)

data class ThinkingConfig(
    val thinkingLevel: String // "OFF", "LOW", "HIGH"
)

data class ImageConfig(
    val aspectRatio: String, // "1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "21:9"
    val imageSize: String = "1K"
)

data class SpeechConfig(
    val voiceConfig: VoiceConfig
)

data class VoiceConfig(
    val prebuiltVoiceConfig: PrebuiltVoiceConfig
)

data class PrebuiltVoiceConfig(
    val voiceName: String // e.g. "Kore", "Puck", "Fenrir"
)

data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)

// --- Retrofit Client ---
object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// --- Helper Extensions ---
fun Bitmap.toBase64(): String {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

// --- High-Level Gemini Caller ---
object GeminiCaller {
    private const val MODEL_LITE = "gemini-3.1-flash-lite-preview"
    private const val MODEL_FLASH = "gemini-3.5-flash"
    private const val MODEL_PRO = "gemini-3.1-pro-preview"
    private const val MODEL_IMAGE = "gemini-3.1-flash-image-preview"
    private const val MODEL_TTS = "gemini-3.1-flash-tts-preview"

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            // Return an empty or fallback string so it doesn't crash but allows debugging or catches errors gracefully
            ""
        } else {
            key
        }
    }

    // Friendly Professor Explanation with TTS
    suspend fun getProfessorExplanation(prompt: String): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return "Please set your GEMINI_API_KEY in the AI Studio Secrets panel."
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are a friendly, helpful, highly empathetic college professor. You explain topics in a warm, friendly, conversational, interactive style using clear analogies, avoiding excessive jargon or AI tropes/emojis. Speak directly to the student.")))
        )
        return try {
            val response = RetrofitClient.service.generateContent(MODEL_LITE, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No explanation could be generated."
        } catch (e: Exception) {
            "Professor is offline right now. Error: ${e.message}"
        }
    }

    // Text to Speech Generation using gemini-3.1-flash-tts-preview
    suspend fun generateTTS(text: String): String? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return null
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Speak this clearly: $text")))),
            generationConfig = GenerationConfig(
                responseModalities = listOf("AUDIO"),
                speechConfig = SpeechConfig(
                    voiceConfig = VoiceConfig(
                        prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = "Kore")
                    )
                )
            )
        )
        return try {
            val response = RetrofitClient.service.generateContent(MODEL_TTS, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData?.data
        } catch (e: Exception) {
            null
        }
    }

    // High Thinking Quiz/Test Generator
    suspend fun generateHighThinkingQuiz(
        chapterName: String,
        fromPage: Int,
        toPage: Int,
        examType: String,
        level: String
    ): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return ""
        val prompt = """
            Generate a study quiz based on:
            Chapter: $chapterName (Pages $fromPage to $toPage)
            Exam Context: $examType (e.g. NEET, MDCAT, Board, CSS)
            Difficulty: $level
            
            Return exactly a JSON array containing exactly 5 multiple choice questions. 
            Do not include markdown blocks or formatting around the JSON, just the JSON array.
            Format:
            [
              {
                "question": "Question text here",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "answerIndex": 0,
                "explanation": "Detailed professional explanation of why this answer is correct."
              }
            ]
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                temperature = 0.4f,
                thinkingConfig = ThinkingConfig(thinkingLevel = "HIGH")
            ),
            systemInstruction = Content(parts = listOf(Part(text = "You are a master academic examiner. Your questions are extremely accurate, challenging, context-specific, and tailored precisely to the exam standards requested.")))
        )
        return try {
            val response = RetrofitClient.service.generateContent(MODEL_PRO, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // Image OCR text extraction and Flashcard generator using gemini-3.1-pro-preview
    suspend fun generateFlashcardsFromImage(bitmap: Bitmap): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return ""
        val prompt = """
            Analyze this uploaded image containing study material. Extract the core scientific/academic terms, equations, or statements, and convert them into a structured JSON list of flashcards.
            Return exactly a JSON array, with no other text or wrapper.
            Format:
            [
              {
                "question": "Clear study question or term definition prompt",
                "answer": "Detailed, highly accurate answer or explanation"
              }
            ]
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
                    )
                )
            )
        )
        return try {
            val response = RetrofitClient.service.generateContent(MODEL_PRO, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // Video Analyzer using gemini-3.1-pro-preview
    suspend fun analyzeVideo(videoTitle: String, isBrief: Boolean, customQuery: String?): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return "Please set your GEMINI_API_KEY."
        val prompt = """
            The student uploaded/provided a study video about: '$videoTitle'.
            The requested explanation length is: ${if (isBrief) "Brief Summary" else "Long Detailed Lecture Notes"}.
            User query: '${customQuery ?: "Explain the main topics and provide key exam-ready practice questions."}'
            
            Provide a beautifully structured lecture note response with sections:
            1. Core Concepts
            2. Detailed Walkthrough / Analogy
            3. Common Exam Pitfalls
            4. 3 High-Yield Practice Questions from the video topics
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are an expert academic tutor summarizing educational video lectures. You extract high-yield points and explain them brilliantly.")))
        )
        return try {
            val response = RetrofitClient.service.generateContent(MODEL_PRO, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Failed to generate video lecture notes."
        } catch (e: Exception) {
            "Error analyzing video: ${e.message}"
        }
    }

    // Maps Grounding search for study spaces using gemini-3.5-flash with maps tool
    suspend fun findStudySpaces(locationQuery: String): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return "Please set your GEMINI_API_KEY."
        val prompt = "Find 4 best public libraries, quiet study cafes, or exam coaching spaces in/near '$locationQuery' for students. List their actual physical names, accurate coordinates, addresses, and details."
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            tools = listOf(Tool(googleMaps = emptyMap(), googleSearch = emptyMap()))
        )
        return try {
            val response = RetrofitClient.service.generateContent(MODEL_FLASH, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not find any locations."
        } catch (e: Exception) {
            "Error looking up study spots: ${e.message}"
        }
    }

    // Image Generator using gemini-3.1-flash-image-preview
    suspend fun generateConceptImage(prompt: String, aspectRatio: String): String? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return null
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "A high-quality educational diagram, vector-style study illustration, or conceptual artwork of: $prompt")))),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = "1K"),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )
        return try {
            val response = RetrofitClient.service.generateContent(MODEL_IMAGE, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData?.data
        } catch (e: Exception) {
            null
        }
    }
}
