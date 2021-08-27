package io.stipop.refactor.domain.entities

import com.google.gson.annotations.SerializedName

data class KeywordItem(@SerializedName("keyword")
                           val keyword: String = "")
