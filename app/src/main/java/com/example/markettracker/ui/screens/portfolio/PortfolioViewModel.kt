package com.example.markettracker.ui.screens.portfolio

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.markettracker.domain.usecase.GetCoinsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.String

private const val INVESTED_PER_COIN = 10_000.0

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val getCoins: GetCoinsUseCase
) : ViewModel() {


    private val _state = MutableStateFlow(PortfolioState())
    val state: StateFlow<PortfolioState> = _state.asStateFlow()

    init {
        observePortfolio()
    }

    private fun observePortfolio() {
        viewModelScope.launch {
            getCoins().collect { coins ->
                val favorites = coins.filter { it.isFavorite }
                if (favorites.isEmpty()) {
                    _state.update {
                        it.copy(isLoading = false, isEmpty = true, holding = emptyList())
                    }
                    return@collect
                }
                val holding = favorites.map { coin ->
                    val currentValue = INVESTED_PER_COIN * (1 + coin.priceChangePercent24h / 100)
                    PortfolioHolding(
                        id = coin.id,
                        name = coin.name,
                        symbol = coin.symbol,
                        imageUri = coin.imageUrl,
                        investmentAmount = INVESTED_PER_COIN,
                        currentValue = currentValue,
                        priceChangePercentage = coin.priceChangePercent24h
                    )
                }
                val totalInvested = favorites.size * INVESTED_PER_COIN
                val totalCurrentValue = holding.sumOf { it.currentValue }
                val overallPnl = ((totalCurrentValue - totalInvested)/totalInvested) * 100

                _state.update {
                    it.copy(
                        holding = holding,
                        totalInvested = totalInvested,
                        totalCurrentValue = totalCurrentValue,
                        overallPnl = overallPnl,
                        isLoading = false,
                        isEmpty = false,
                    )
                }
            }
        }
    }
}