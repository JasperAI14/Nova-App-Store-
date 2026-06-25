package com.example.data.api

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class PaystackCard(
    val number: String,
    val cvv: String,
    val expiry_month: String,
    val expiry_year: String
)

@JsonClass(generateAdapter = true)
data class PaystackChargeRequest(
    val email: String,
    val amount: String, // in kobo
    val card: PaystackCard
)

@JsonClass(generateAdapter = true)
data class PaystackPinRequest(
    val pin: String,
    val reference: String
)

@JsonClass(generateAdapter = true)
data class PaystackOtpRequest(
    val otp: String,
    val reference: String
)

@JsonClass(generateAdapter = true)
data class PaystackChargeResponse(
    val status: Boolean,
    val message: String?,
    val data: PaystackChargeData?
)

@JsonClass(generateAdapter = true)
data class PaystackChargeData(
    val status: String?, // "success", "send_pin", "send_otp", "failed"
    val reference: String?,
    val amount: Int?,
    val message: String?,
    val displayText: String?
)

interface PaystackApiService {
    @POST("charge")
    suspend fun chargeCard(
        @Header("Authorization") authorization: String,
        @Body request: PaystackChargeRequest
    ): PaystackChargeResponse

    @POST("charge/submit_pin")
    suspend fun submitPin(
        @Header("Authorization") authorization: String,
        @Body request: PaystackPinRequest
    ): PaystackChargeResponse

    @POST("charge/submit_otp")
    suspend fun submitOtp(
        @Header("Authorization") authorization: String,
        @Body request: PaystackOtpRequest
    ): PaystackChargeResponse
}

object PaystackRetrofitClient {
    private const val BASE_URL = "https://api.paystack.co/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: PaystackApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PaystackApiService::class.java)
    }
}
