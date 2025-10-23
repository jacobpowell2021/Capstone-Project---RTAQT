package com.example.airqualitytracker

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Http {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val ok = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://predictiveanalysisalgorithmfunction-d2h7awe2bpfad9ce.centralus-01.azurewebsites.net/") // no /api here
        .addConverterFactory(GsonConverterFactory.create())
        .client(ok)
        .build()

    val api: PredictiveApi = retrofit.create(PredictiveApi::class.java)

}
