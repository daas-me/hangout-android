package com.hangout.app.data

import com.google.gson.annotations.SerializedName

data class FavoriteStatusResponse(
    @SerializedName("isFavorite") val isFavorite: Boolean,
    val favoriteCount: Int?
)