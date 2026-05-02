package com.example.markettracker.ui.screens.invest

import com.example.markettracker.domain.model.Coin

data class InvestState (
    val trendingCoins : List<Coin> = emptyList(),
    val mostTraded : List<Coin> = emptyList(),
    val isLoading : Boolean = true
)