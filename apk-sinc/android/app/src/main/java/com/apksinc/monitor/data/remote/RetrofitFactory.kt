package com.apksinc.monitor.data.remote

import com.apksinc.monitor.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitFactory {

    fun create(baseUrl: String = BuildConfig.API_BASE_URL): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        // O backend exige o header X-API-Key quando API_KEY esta definida no
        // .env dele. A chave vem de local.properties (fora do Git) via
        // BuildConfig; se estiver vazia, nada e enviado - util para rodar
        // contra um backend local sem autenticacao.
        val apiKeyInterceptor = Interceptor { chain ->
            val request = if (BuildConfig.API_KEY.isNotEmpty()) {
                chain.request().newBuilder()
                    .addHeader("X-API-Key", BuildConfig.API_KEY)
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
