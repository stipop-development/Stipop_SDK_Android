package io.stipop.data

import io.stipop.Stipop
import io.stipop.api.StipopApi
import io.stipop.models.body.InitSdkBody
import io.stipop.models.body.UserIdBody
import io.stipop.models.enums.StipopApiEnum
import io.stipop.models.response.StipopResponse
import retrofit2.HttpException

internal class ConfigRepository() : BaseRepository() {

    var isConfigured = false
    var isInitialized = false
    var currentUserId: String? = null

    suspend fun postInitSdk(initSdkBody: InitSdkBody, onSuccess: ((data: Any) -> Unit)? = null) {
        initSdkBody.userId?.let {
            if (currentUserId == it || isInitialized) {
//                onSuccess?.let { it(Unit) }
            } else {
                currentUserId = it
                isInitialized = true
            }
            safeCall(
                call = { StipopApi.create().initSdk(initSdkBody = initSdkBody) }, onCompletable = {
                    onSuccess?.let { it(Unit) }
                })
        } ?: kotlin.run {
            onSuccess?.let { it(Unit) }
        }
    }

    suspend fun postConfigSdk() =
        try {
            safeCall(
                call = { StipopApi.create().trackConfig(
                    userIdBody = UserIdBody(userId = Stipop.userId)) },
                onCompletable = {
                    //
                })
        } catch(exception: HttpException){
            when(exception.code()){
                401 -> Stipop.sAuthDelegate?.httpException(StipopApiEnum.TRACK_CONFIG, exception)
                else -> {}
            }
        } catch (exception: Exception){
            Stipop.trackError(exception)
        }

    suspend fun postTrackUsingSticker(
        stickerId: String,
        userId: String,
        query: String?,
        countryCode: String,
        lang: String,
        eventPoint: String?,
        onSuccess: (data: StipopResponse) -> Unit
    ) {
        safeCall(
            call = {
                StipopApi.create().trackUsingSticker(
                    stickerId = stickerId,
                    userId = userId,
                    query = query,
                    countryCode = countryCode,
                    lang = lang,
                    eventPoint = eventPoint
                )
            }, onCompletable = {
                it?.let(onSuccess)
            })
    }
}