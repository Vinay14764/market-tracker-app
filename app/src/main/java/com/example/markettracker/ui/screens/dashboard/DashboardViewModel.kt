package com.example.markettracker.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.markettracker.domain.usecase.GetTopGainersUseCase
import com.example.markettracker.domain.usecase.GetTopLosersUseCase
import com.example.markettracker.domain.usecase.RefreshCoinsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Dashboard (Home) screen.
 *
 * WHAT'S NEW: The Dashboard previously showed hardcoded coin data.
 * Now it shows real top gainers and losers from the local Room database.
 *
 * HOW DATA FLOWS WITHOUT A NETWORK CALL:
 *   1. [MarketScreen] triggers [MarketViewModel] on launch, which calls [RefreshCoinsUseCase].
 *   2. RefreshCoinsUseCase fetches data from CoinGecko and saves it to Room.
 *   3. Room emits the updated data to all active collectors, including this ViewModel.
 *   4. [GetTopGainersUseCase] and [GetTopLosersUseCase] filter and sort that data.
 *   5. DashboardViewModel updates its state → Dashboard redraws with real coins.
 *
 * Room is the single source of truth — there's no duplication of network calls.
 *
 * @param getTopGainers  Use case that provides coins with the highest 24h positive change.
 * @param getTopLosers   Use case that provides coins with the biggest 24h negative change.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTopGainers: GetTopGainersUseCase,
    private val getTopLosers: GetTopLosersUseCase,
    private val refreshCoins: RefreshCoinsUseCase  // Needed so Dashboard loads data on its own
) : ViewModel() {

    /** Internal mutable state — only this ViewModel can write to it. */
    private val _state = MutableStateFlow(DashboardState())

    /**
     * Public read-only state for the UI.
     * Collect in the Composable: val state by viewModel.state.collectAsState()
     */
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        observeTopGainers()
        observeTopLosers()
        // Fetch fresh data from API so Dashboard doesn't depend on Market screen being visited first
        viewModelScope.launch { refreshCoins() }
    }

    /**
     * Collects the top 5 gainers from Room and pushes them to [state].
     * This coroutine stays alive as long as the ViewModel is alive (viewModelScope).
     * Room automatically re-emits when the coin data changes.
     */
    private fun observeTopGainers() {
        viewModelScope.launch {
            getTopGainers(limit = 5).collect { gainers ->
                _state.update { it.copy(topGainers = gainers, isLoading = false) }
            }
        }
    }

    /**
     * Collects the top 5 losers from Room and pushes them to [state].
     * Runs in parallel with [observeTopGainers] — both coroutines are independent.
     */
    private fun observeTopLosers() {
        viewModelScope.launch {
            getTopLosers(limit = 5).collect { losers ->
                _state.update { it.copy(topLosers = losers) }
            }
        }
    }
}