package com.example.data.remote.websearch

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object DuckDuckGoWebSearch {
    private const val TAG = "DuckDuckGoWebSearch"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Executes an instant API search on DuckDuckGo using relative URLEncoded queries.
     * Keeps execution strictly off the Main Thread.
     */
    suspend fun performSearch(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chromebook/100.0.0.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                if (body.startsWith("{")) {
                    val json = JSONObject(body)
                    
                    val abstractText = json.optString("AbstractText", "")
                    if (abstractText.isNotEmpty()) {
                        return@withContext abstractText
                    }

                    val definition = json.optString("Definition", "")
                    if (definition.isNotEmpty()) {
                        return@withContext definition
                    }

                    val relatedTopics = json.optJSONArray("RelatedTopics")
                    if (relatedTopics != null && relatedTopics.length() > 0) {
                        val firstTopic = relatedTopics.optJSONObject(0)
                        val text = firstTopic?.optString("Text", "")
                        if (!text.isNullOrEmpty()) {
                            return@withContext text
                        }
                    }
                }
            }
            "اطلاعات مشخصی یافت نشد. موضوع را می‌توانید در وب جستجو کنید."
        } catch (e: Exception) {
            Log.e(TAG, "Search network error", e)
            "خطا در شبکه. از صحت اتصال اینترنت خود اطمینان حاصل کنید."
        }
    }
}
