package com.learnon.app.instructor.data.remote

import android.content.Context
import com.learnon.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class InstructorTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("learnon_instructor_prefs", Context.MODE_PRIVATE)

    fun save(token: String, refreshToken: String?, name: String?) {
        prefs.edit()
            .putString("token", token)
            .putString("refresh_token", refreshToken)
            .putString("name", name)
            .apply()
    }

    fun token(): String? = prefs.getString("token", null)
    fun hasToken(): Boolean = !token().isNullOrBlank()
    fun name(): String = prefs.getString("name", "Instrutor") ?: "Instrutor"
    fun clear() = prefs.edit().clear().apply()
}

object InstructorNetwork {
    fun createApi(context: Context): InstructorApi {
        val tokenStore = InstructorTokenStore(context.applicationContext)
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
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
