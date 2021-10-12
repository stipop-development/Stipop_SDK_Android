package io.stipop.api

import android.os.Build
import android.util.Log
import com.google.gson.Gson
import io.stipop.*
import io.stipop.models.body.InitSdkBody
import io.stipop.models.body.OrderChangeBody
import io.stipop.models.body.StipopMetaHeader
import io.stipop.models.response.MyStickerOrderChangedResponse
import io.stipop.models.response.MyStickerResponse
import io.stipop.models.response.StickerPackageResponse
import io.stipop.models.response.StipopResponse
import io.stipop.BuildConfig
import io.stipop.models.body.UserIdBody
import okhttp3.*
import okhttp3.Headers
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface StipopApi {

    @POST("init")
    suspend fun initSdk(
        @Body initSdkBody: InitSdkBody
    ): Response<StipopResponse>

    @GET("mysticker/{userId}")
    suspend fun getMyStickers(
        @Path("userId") userId: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("limit") limit: Int
    ): MyStickerResponse

    @GET("mysticker/hide/{userId}")
    suspend fun getMyHiddenStickers(
        @Path("userId") userId: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("limit") limit: Int
    ): MyStickerResponse

    @PUT("mysticker/order/{userId}")
    suspend fun putMyStickerOrders(
        @Path("userId") userId: String,
        @Body orderChangeBody: OrderChangeBody
    ): MyStickerOrderChangedResponse

    @PUT("mysticker/hide/{userId}/{packageId}")
    suspend fun putMyStickerVisibility(
        @Path("userId") userId: String,
        @Path("packageId") packageId: Int
    ): StipopResponse

    @GET("package")
    suspend fun getTrendingStickerPackages(
        @Query("userId") userId: String,
        @Query("lang") lang: String,
        @Query("countryCode") countryCode: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("limit") limit: Int,
        @Query("q") query: String? = null
    ): Response<StickerPackageResponse>

    @POST("download/{packageId}")
    suspend fun postDownloadStickers(
        @Path("packageId") packageId: Int,
        @Query("userId") userId: String,
        @Query("isPurchase") isPurchase: String,
        @Query("countryCode") countryCode: String,
        @Query("lang") lang: String,
        @Query("price") price: Double? = null,
    ): Response<StipopResponse>

    @POST("sdk/track/config")
    suspend fun trackConfig(@Body userIdBody: UserIdBody): Response<StipopResponse>

    companion object {
        fun create(): StipopApi {
            val loggingInterceptor = HttpLoggingInterceptor().apply { level = Level.BASIC }
            val requestInterceptor = Interceptor { chain ->
                val original = chain.request()
                val modifiedUrl = chain.request().url.newBuilder().addQueryParameter(Constants.ApiParams.Platform, Constants.Value.PLATFORM).build()
                chain.proceed(original.newBuilder().url(modifiedUrl).build())
            }
            val headers = Headers.Builder()
                .add(Constants.ApiParams.ApiKey, if(BuildConfig.DEBUG) Constants.Value.SANDBOX_APIKEY else Config.apikey)
                .add(Constants.ApiParams.SMetadata, Gson().toJson(StipopMetaHeader(platform = Constants.Value.PLATFORM, sdk_version = BuildConfig.SDK_VERSION_NAME, os_version = Build.VERSION.SDK_INT.toString())))
                .build()
            val authenticator = Authenticator { _, response ->
                response.request
                    .newBuilder()
                    .headers(headers)
                    .build()
            }
            val client = OkHttpClient.Builder()
                .authenticator(authenticator)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(requestInterceptor)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .build()
            return Retrofit.Builder()
                .baseUrl(if(BuildConfig.DEBUG) Constants.Value.SANDBOX_URL else Constants.Value.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(StipopApi::class.java)
        }
    }
}
