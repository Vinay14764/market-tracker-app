package com.example.markettracker.ui.screens.market


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.markettracker.domain.repository.CoinRepository
import com.example.markettracker.domain.usecase.GetCoinDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * ViewModel for the Coin Detail screen.
 *
 * WHAT'S NEW: Replaces the old [CoinDetailScreen] that showed hardcoded Bitcoin data.
 * Now the screen loads REAL data for whichever coin the user tapped.
 *
 * HOW THE COIN ID IS PASSED:
 *   The navigation route is "coin_detail/{coinId}" (defined in NavGraph).
 *   When the user taps a coin, NavController navigates to "coin_detail/bitcoin" for example.
 *   Jetpack Navigation Compose automatically puts navigation arguments into [SavedStateHandle].
 *   So [savedStateHandle["coinId"]] gives us "bitcoin" — no need to pass it manually.
 *
 * HOW IT LOADS DATA:
 *   [GetCoinDetailUseCase] returns a Flow<Coin?> that reads from Room.
 *   If the coin exists in Room (because MarketScreen already fetched it), it shows immediately.
 *   If Room is empty (fresh install, no market refresh yet), [coin] will be null → show loading.
 *
 * @param savedStateHandle Injected by Hilt — contains the navigation arguments.
 * @param getCoinDetail    Use case that provides a live stream of a single coin from Room.
 */
@HiltViewModel
class CoinDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCoinDetail: GetCoinDetailUseCase,
    private val respository: CoinRepository
) : ViewModel() {

    /**
     * The coin ID read from the navigation argument.
     * [checkNotNull] crashes with a clear message if the ID is missing from nav args,
     * which would be a programming error (the route must always include {coinId}).
     */
    private val coinId: String = checkNotNull(savedStateHandle["coinId"]) {
        "CoinDetailViewModel requires a 'coinId' navigation argument. " +
        "Make sure the NavGraph route is 'coin_detail/{coinId}'."
    }

    /** Internal mutable state — only this ViewModel can write to it. */
    private val _state = MutableStateFlow(CoinDetailState())

    /**
     * Public read-only state for the UI.
     * Collect in the Composable: val state by viewModel.state.collectAsState()
     */
    val state: StateFlow<CoinDetailState> = _state.asStateFlow()

    init {
        // Start observing the coin from Room as soon as the ViewModel is created
        loadCoin()
        fetchDescription()
    }

    /**
     * Collects the live coin stream from [GetCoinDetailUseCase].
     *
     * The Flow emits a new value whenever:
     *   - The coin is first loaded from Room
     *   - The coin's price is updated (after a market refresh)
     *   - The coin's favorite status changes
     *
     * Null emission means the coin isn't in Room yet → keep showing loading state.
     */
    private fun loadCoin() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            getCoinDetail(coinId).collect { coin ->
                if (coin != null) {
                    // Coin found in Room — update state and stop loading
                    _state.update { it.copy(coin = coin, isLoading = false, error = null) }
                } else {
                    // Coin not in Room yet (DB empty or wrong ID)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Coin not found. Please go back and refresh the market."
                        )
                    }
                }
            }
        }
    }

    private fun fetchDescription() {
        viewModelScope.launch {
            _state.update { it.copy(isDescriptionLoading = true) }
            try{
            val description = respository.getCoinDescription(coinId)
            _state.update { it.copy(description = description.ifBlank { null }, isDescriptionLoading = false) }
        } catch (e: Exception) {
            _state.update { it.copy(isDescriptionLoading = false) }
        }
    }
}
/**
     * Called when the user taps a timeframe button (15m, 30m, 1H, 4H, 1D).
     * Updates the selected timeframe in state so the active button is highlighted.
     *
     * @param timeframe One of: "15m", "30m", "1H", "4H", "1D"
     */
    fun onTimeSelected(time: String) {
        _state.update {
            it.copy(selectedTimeframe = time)
        }
    }

    // 🔥 CHANGE 3: Chart data depends on STATE (not separate variable)
    fun getChartData(): List<Float> {

        val coin = _state.value.coin ?: return emptyList()
        val data = smoothData(coin.sparkline)

        return when (_state.value.selectedTimeframe) {
            "15m" -> data.takeLast(20)
            "30m" -> data.takeLast(40)
            "1H" -> data.takeLast(60)
            "4H" -> data.takeLast(120)
            "1D" -> data
            else -> data
        }
    }
    fun smoothData(data: List<Float>): List<Float> {
        return data.windowed(3, 1, partialWindows = true) {
            it.average().toFloat()
        }
    }
}