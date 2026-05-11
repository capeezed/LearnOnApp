package com.learnon.app.instructor.data.remote

import android.content.Context
import com.learnon.app.BuildConfig
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class InstructorTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("learnon_instructor_prefs", Context.MODE_PRIVATE)

    fun save(token: String, refreshToken: String?, name: String?) {
        val editor = prefs.edit()
            .putString("token", token)
            .putString("refresh_token", refreshToken)
        if (!name.isNullOrBlank()) editor.putString("name", name)
        editor.apply()
    }

    fun token(): String? = prefs.getString("token", null)
    fun refreshToken(): String? = prefs.getString("refresh_token", null)
    fun hasToken(): Boolean = !token().isNullOrBlank()
    fun name(): String = prefs.getString("name", "Instrutor") ?: "Instrutor"
    fun clear() = prefs.edit().clear().apply()
}

private class InstructorTokenAuthenticator(
    private val tokenStore: InstructorTokenStore,
) : Authenticator {
    private val refreshClient = OkHttpClient()

    override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
        if (response.request.url.encodedPath.endsWith("/auth/refresh")) return null
        if (responseCount(response) > 1) return null

        val currentToken = tokenStore.token()
        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (!currentToken.isNullOrBlank() && currentToken != requestToken) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        }

        val refreshedToken = refreshAccessToken() ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $refreshedToken")
            .build()
    }

    @Synchronized
    private fun refreshAccessToken(): String? {
        val refreshToken = tokenStore.refreshToken().takeUnless { it.isNullOrBlank() } ?: return null
        val body = JSONObject()
            .put("refreshToken", refreshToken)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(BuildConfig.LEARNON_BASE_URL + "auth/refresh")
            .post(body)
            .header("Accept", "application/json")
            .header("ngrok-skip-browser-warning", "true")
            .build()

        refreshClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val json = JSONObject(response.body?.string().orEmpty())
            val accessToken = json.optString("accessToken", json.optString("token", ""))
            val newRefreshToken = json.optString("refreshToken", refreshToken)
            if (accessToken.isBlank()) return null
            tokenStore.save(accessToken, newRefreshToken, null)
            return accessToken
        }
    }

    private fun responseCount(response: okhttp3.Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

object InstructorNetwork {
    fun createApi(context: Context): InstructorApi {
        val tokenStore = InstructorTokenStore(context.applicationContext)
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .authenticator(InstructorTokenAuthenticator(tokenStore))
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("ngrok-skip-browser-warning", "true")
                    .header("Accept", "application/json")

                tokenStore.token()?.let { builder.header("Authorization", "Bearer $it") }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.LEARNON_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InstructorApi::class.java)
    }
}
