package com.example.tinyprice_android_supabase.data.remote

import com.example.tinyprice_android_supabase.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object TinyPriceApiFactory {
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder().apply {
                if (BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) {
                    header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                }
            }.build()
            chain.proceed(request)
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun create(): TinyPriceApi = Retrofit.Builder()
        .baseUrl(BuildConfig.EDGE_RUNTIME_BASE_URL.ensureTrailingSlash())
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TinyPriceApi::class.java)
}

private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"
