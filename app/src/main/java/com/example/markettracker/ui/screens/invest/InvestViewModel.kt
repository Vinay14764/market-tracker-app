package com.example.markettracker.ui.screens.invest

import androidx.compose.runtime.MutableState
import androidx.compose.ui.util.fastCbrt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.markettracker.domain.usecase.GetCoinsUseCase
import com.example.markettracker.domain.usecase.GetTopGainersUseCase
import com.example.markettracker.domain.usecase.GetTopLosersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvestViewModel @Inject constructor(
    private val getCoins: GetCoinsUseCase,
    private val getTopGainers: GetTopGainersUseCase
) : ViewModel() {


    private val _state = MutableStateFlow(InvestState())
    val state: StateFlow<InvestState> = _state.asStateFlow()

    init {
        observeTrending()
        observeMostTraded()
    }



    private fun observeTrending() {
        viewModelScope.launch {
            getTopGainers(limit = 10).collect { coins ->
                _state.update {
                    it.copy(
                        trendingCoins = coins,
                        isLoading = false
                    )
                }
            }
        }
    }
    private fun observeMostTraded() {
        viewModelScope.launch {
            getTopGainers(10).collect { coins ->
                _state.update {
                    it.copy(
                        mostTraded = coins.take(10) ,
                        isLoading = false
                    )
                }
            }
        }
    }
}