package io.stipop.view.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.stipop.Config
import io.stipop.Stipop
import io.stipop.ViewPickerViewType
import io.stipop.api.StipopApi
import io.stipop.data.StickerDetailRepository
import io.stipop.event.PackageDownloadEvent
import io.stipop.models.StickerPackage
import io.stipop.models.body.UserIdBody
import io.stipop.models.enums.StipopApiEnum
import io.stipop.s_auth.GetStickerPackageEnum
import io.stipop.s_auth.PostDownloadStickersEnum
import io.stipop.s_auth.SAuthManager
import kotlinx.coroutines.launch
import retrofit2.HttpException

internal class PackDetailViewModel(private val repository: StickerDetailRepository) :
    ViewModel() {

    var stickerPackage: MutableLiveData<StickerPackage?> = MutableLiveData()

    fun trackViewPackage(packageId: Int, entrancePoint: String?) {
        viewModelScope.launch {
            try {
                StipopApi.create().trackViewPackage(
                    userIdBody = UserIdBody(userId = Stipop.userId),
                    entrancePoint = entrancePoint,
                    packageId = packageId
                )
            } catch (exception: HttpException) {
                when (exception.code()) {
                    401 -> {
                        SAuthManager.setTrackViewPackageData(packageId, entrancePoint)
                        Stipop.sAuthDelegate?.httpException(StipopApiEnum.TRACK_VIEW_PACKAGE, exception)
                    }
                }
            } catch (exception: Exception) {
                Stipop.trackError(exception)
            }
        }
    }

    fun loadsPackages(packageId: Int) {
        viewModelScope.launch {
            try {
                repository.getStickerPackage(packageId, onSuccess = {
                    if (it.header.isSuccess()) {
                        stickerPackage.postValue(it.body?.stickerPackage)
                    }
                })
            } catch (exception: HttpException) {
                when (exception.code()) {
                    401 -> {
                        SAuthManager.setGetStickerPackageData(GetStickerPackageEnum.PACK_DETAIL_VIEW_MODEL, StickerPackage(packageId))
                        Stipop.sAuthDelegate?.httpException(StipopApiEnum.GET_STICKER_PACKAGE, exception)
                    }
                }
            } catch (exception: Exception) {
                Stipop.trackError(exception)
            }
        }
    }

    fun requestDownloadPackage() {
        viewModelScope.launch {
            stickerPackage.value?.let { stickerPackage ->
                if (!stickerPackage.isDownloaded()) {
                    try {
                        repository.postDownloadStickers(stickerPackage) {
                            it?.let { response ->
                                if (response.header.isSuccess()) {
                                    PackageDownloadEvent.publishEvent(stickerPackage.packageId)
                                    if (Config.getViewPickerViewType() == ViewPickerViewType.FRAGMENT) {
                                        Stipop.stickerPickerViewClass?.packAdapter?.refresh()
                                    }
                                }
                            }
                        }
                    } catch (exception: HttpException) {
                        when (exception.code()) {
                            401 -> {
                                SAuthManager.setPostDownloadStickersData(PostDownloadStickersEnum.PACK_DETAIL_VIEW_MODEL, stickerPackage)
                                Stipop.sAuthDelegate?.httpException(StipopApiEnum.POST_DOWNLOAD_STICKERS, exception)
                            }
                        }
                    } catch (exception: Exception) {
                        Stipop.trackError(exception)
                    }
                }
            }
        }
    }
}