package io.stipop.refactor.data.services

import io.stipop.refactor.domain.entities.PackageListResponse
import io.stipop.refactor.domain.entities.PackageResponse
import io.stipop.refactor.domain.entities.VoidResponse
import io.stipop.refactor.domain.repositories.StickerStoreRepositoryProtocol
import retrofit2.http.*

interface StickerStoreService : StickerStoreRepositoryProtocol {

    @GET("package")
    override suspend fun trendingStickerPacks(
        @Header("apikey")
        apikey: String,

        @Query("q")
        q: String,

        @Query("userId")
        userId: String,

        @Query("lang")
        lang: String?,

        @Query("countryCode")
        countryCode: String?,

        @Query("premium")
        premium: String?,

        @Query("limit")
        limit: Int?,

        @Query("pageNumber")
        pageNumber: Int?,

        @Query("animated")
        animated: String?
    ): PackageListResponse

    @GET("package/{packageId}")
    override suspend fun stickerPackInfo(
        @Header("apikey")
        apikey: String,

        @Path("packageId")
        packId: String,

        @Query("userId")
        userId: String
    ): PackageResponse

    @POST("download/{packageId}")
    override suspend fun downloadPurchaseSticker(
        @Header("apikey")
        apikey: String,

        @Path("packageId")
        packId: String,

        @Query("userId")
        userId: String,

        @Query("isPurchase")
        isPurchase: String,

        @Query("lang")
        lang: String?,

        @Query("countryCode")
        countryCode: String?,

        @Query("price")
        price: String?
    ): VoidResponse
}
