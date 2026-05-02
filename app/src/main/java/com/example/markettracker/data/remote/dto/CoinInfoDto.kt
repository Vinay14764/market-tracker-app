package com.example.markettracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CoinInfoDto(
    @SerializedName("id") val id:String,
    @SerializedName("description") val description: DescriptionDto?
)
data class DescriptionDto(
    @SerializedName("en") val en: String?
)