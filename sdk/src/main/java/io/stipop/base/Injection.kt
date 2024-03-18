package io.stipop.base

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import io.stipop.data.MyStickerRepository
import io.stipop.data.PkgRepository
import io.stipop.data.SearchingRepository
import io.stipop.data.StickerDetailRepository
import io.stipop.view.viewmodel.*

internal object Injection {

    private fun provideMyStickerRepository(): MyStickerRepository {
        return MyStickerRepository()
    }

    private fun providePackageRepository(): PkgRepository {
        return PkgRepository()
    }

    private fun provideStickerDetailRepository(): StickerDetailRepository {
        return StickerDetailRepository()
    }

    private fun provideSearchingRepository(): SearchingRepository {
        return SearchingRepository()
    }

    fun provideViewModelFactory(owner: SavedStateRegistryOwner): ViewModelProvider.Factory {
        return ViewModelFactory(owner)
    }

    class ViewModelFactory(
        owner: SavedStateRegistryOwner
    ) : AbstractSavedStateViewModelFactory(owner, null) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            if (modelClass.isAssignableFrom(StoreMyStickerViewModel::class.java)) {
                return StoreMyStickerViewModel(provideMyStickerRepository()) as T
            } else if (modelClass.isAssignableFrom(StoreHomeViewModel::class.java)) {
                return StoreHomeViewModel(providePackageRepository()) as T
            } else if (modelClass.isAssignableFrom(StoreNewsViewModel::class.java)) {
                return StoreNewsViewModel(providePackageRepository()) as T
            } else if (modelClass.isAssignableFrom(PackDetailViewModel::class.java)) {
                return PackDetailViewModel(provideStickerDetailRepository()) as T
            } else if (modelClass.isAssignableFrom(SsvModel::class.java)) {
                return SsvModel(provideSearchingRepository()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}


