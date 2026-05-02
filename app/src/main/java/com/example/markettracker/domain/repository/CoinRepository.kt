package com.example.markettracker.domain.repository

import com.example.markettracker.core.AppResult
import com.example.markettracker.domain.model.Coin
import kotlinx.coroutines.flow.Flow

interface CoinRepository {

    fun getCoins() : Flow<List<Coin>>
    suspend fun refreshCoins() : AppResult<Unit>
    fun getCoinById(id:String): Flow<Coin?>
    suspend fun getCoinDescription(id : String) : String
    suspend fun toggleFavorite(coinId : String, currentState: Boolean): AppResult<Unit>
}