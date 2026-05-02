package com.example.markettracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CoinDto(
    val id : String,
    val name : String,
    val symbol : String,
    val image : String,
    @SerializedName("current_price")
    val currentPrice : Double,
    @SerializedName("price_change_percentage_24h")
    val priceChangePercentage24h : Double,
    @SerializedName("sparkline_in_7d")
    val sparklineIn7d: SparklineDto
)