package com.example.markettracker.data.api

import com.example.markettracker.data.remote.dto.CoinDto
import com.example.markettracker.data.remote.dto.CoinInfoDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface Cryptoapi {

    @GET("coins/markets")
    suspend fun getCoins(
        @Query("vs_currency")
        currency: String = "inr",

        @Query("sparkline")
        sparkline: Boolean = true
    ): List<CoinDto>

    @GET("coins/{id}")
    suspend fun getCoinInfo(
        @Path("id") id: String,
        @Query("localization") localization: Boolean = false,
        @Query("tickers") tickers: Boolean = false,
        @Query("market_data") marketData: Boolean = false,
        @Query("community_data") communityData: Boolean = false,
        @Query("developer_data") developerData: Boolean = false
    ): CoinInfoDto

}