package io.stipop.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.stipop.data.MyStickerRepository
import io.stipop.models.StickerPackage
import io.stipop.models.response.StipopResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyStickerViewModel(private val repository: MyStickerRepository) : ViewModel() {

    private var isWantVisibleStickers: Boolean = true
    var packageVisibilityChanged: MutableLiveData<Triple<StipopResponse, Int, Int>> =
        MutableLiveData<Triple<StipopResponse, Int, Int>>()

    init {
        viewModelScope.launch {
            repository.packageVisibilityUpdateResult.collectLatest {
                packageVisibilityChanged.postValue(it)
            }
        }
    }

    fun loadsPackages(wantVisibleSticker: Boolean): Flow<PagingData<StickerPackage>> {
        isWantVisibleStickers = wantVisibleSticker
        return repository.getMyStickerStream(isWantVisibleStickers).cachedIn(viewModelScope)
    }

    fun changePackageOrder(fromStickerPackage: StickerPackage, toStickerPackage: StickerPackage) =
        viewModelScope.launch {
            repository.request(fromStickerPackage, toStickerPackage)
        }

    fun hideOrRecoverPackage(packageId: Int, position: Int) = viewModelScope.launch {
        repository.updatePackageVisibility(packageId, position)
    }
}