package io.stipop.refactor.domain.entities


import com.google.gson.annotations.SerializedName

data class KeywordListResponse(
    @SerializedName("header")
    val header: Header,
    @SerializedName("body")
    val body: KeywordListBody
)





