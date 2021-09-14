package io.stipop.refactor.data.repositories

import android.util.Log
import io.stipop.refactor.data.datasources.SearchRestDatasource
import io.stipop.refactor.domain.entities.SPUser
import io.stipop.refactor.domain.repositories.SearchStickerRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

class SearchStickerDataRepository @Inject constructor(
    private val _remoteDatasource: SearchRestDatasource
) : SearchStickerRepository() {

    override fun onLoadList(user: SPUser, keyword: String, pageNumber: Int, limit: Int?) {
        Log.d(
            this::class.simpleName, "onLoadList : \n " +
                    "user -> $user \n" +
                    "keyword -> $keyword \n" +
                    "pageNumber -> $pageNumber \n" +
                    "limit -> $limit \n" +
                    ""
        )
        launch {
            hasLoading = coroutineContext.isActive
            _remoteDatasource.stickerSearch(
                user.apikey,
                keyword,
                user.userId,
                user.language,
                user.country,
                limit,
                pageNumber,
            )
                .run {
                    body.stickerList?.let {
                        if (it.isNotEmpty()) {
                            pageMap = body.pageMap
                            _listChanged.postValue(it)
                        }
                    }
                }
            cancel()
            hasLoading = coroutineContext.isActive
        }
    }

}
