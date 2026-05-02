package com.example.markettracker.ui.screens.portfolio

data class PortfolioHolding(
  val id : String,
    val name : String,
    val symbol : String,
    val imageUri : String,
    val investmentAmount : Double,
    val currentValue : Double,
    val priceChangePercentage : Double
)

data class PortfolioState(
    val holding: List<PortfolioHolding> = emptyList(),
    val totalInvested : Double = 0.0,
    val totalCurrentValue : Double = 0.0,
    val overallPnl : Double = 0.0,
    val isLoading : Boolean = true,
    val isEmpty : Boolean = true

)