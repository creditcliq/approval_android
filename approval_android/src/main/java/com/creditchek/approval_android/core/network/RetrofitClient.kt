package com.creditchek.approval_android.core.network

import android.os.Environment
import com.creditchek.approval_android.core.session.ApprovalEnv
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RetrofitClient {

    private fun createOkHttpClient(isDebug: Boolean = true): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (isDebug) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(ApiConstants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConstants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    fun createRetrofit(environment: ApprovalEnv = ApprovalEnv.SANDBOX): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConstants.getBaseUrl(environment))
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    fun <T> createService(serviceClass: Class<T>, environment: ApprovalEnv = ApprovalEnv.SANDBOX): T {
        return createRetrofit(environment).create(serviceClass)
    }
}