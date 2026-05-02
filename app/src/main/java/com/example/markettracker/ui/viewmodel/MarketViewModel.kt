package com.example.markettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.markettracker.core.AppException
import com.example.markettracker.core.AppResult
import com.example.markettracker.domain.model.MarketFilter
import com.example.markettracker.domain.usecase.GetCoinsUseCase
import com.example.markettracker.domain.usecase.RefreshCoinsUseCase
import com.example.markettracker.domain.usecase.ToggleFavoriteUseCase
import com.example.markettracker.ui.screens.market.MarketEffect
import com.example.markettracker.ui.screens.market.MarketIntent
import com.example.markettracker.ui.screens.market.MarketState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Market screen. Follows the MVI (Model-View-Intent) pattern.
 *
 * HOW IT WORKS:
 *   1. The UI sends user actions as [MarketIntent] via [onIntent].
 *   2. This ViewModel processes the intent and updates [state] (a [StateFlow]).
 *   3. The Composable collects [state] and redraws itself.
 *   4. One-shot events (navigation) are sent via [effect] (a [Channel]).
 *
 * WHY @HiltViewModel?
 *   Hilt creates and injects the ViewModel's dependencies automatically.
 *   No more [companion object { fun factory(context) { ... } }] boilerplate.
 *   The UI just calls hiltViewModel() and gets a fully wired ViewModel.
 *
 * @param getCoins      Provides the live coin list from Room (auto-updates on DB change).
 * @param refreshCoins  Fetches fresh data from the CoinGecko API.
 * @param toggleFavorite Flips a coin's isFavorite status in Room.
 */
@HiltViewModel
class MarketViewModel @Inject constructor(
    private val getCoins: GetCoinsUseCase,
    private val refreshCoins: RefreshCoinsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    // ─────────────────────────────────────────────────
    // State — the single source of truth for the UI
    // ─────────────────────────────────────────────────

    /** Internal mutable state. Only this ViewModel can write to it. */
    private val _state = MutableStateFlow(MarketState())

    /**
     * Public read-only state exposed to the UI.
     * Collect this in the Composable: val state by viewModel.state.collectAsState()
     */
    val state: StateFlow<MarketState> = _state.asStateFlow()

    // ─────────────────────────────────────────────────
    // Effect — one-shot events (navigation, snackbars)
    // ─────────────────────────────────────────────────

    /** Tracks the active search debounce job so it can be cancelled on each new keystroke. */
    private var searchDebounceJob: Job? = null

    /**
     * Internal Channel for one-shot effects.
     * [Channel.BUFFERED] means effects sent before the UI starts collecting won't be lost.
     */
    private val _effect = Channel<MarketEffect>(Channel.BUFFERED)

    /**
     * Public effects stream. Collect this in the Composable inside a LaunchedEffect:
     *   LaunchedEffect(Unit) { viewModel.effect.collect { handle(it) } }
     *
     * Each effect is delivered EXACTLY ONCE, even on screen rotation.
     * This prevents the double-navigation bug that existed in the old code.
     */
    val effect = _effect.receiveAsFlow()

    // ─────────────────────────────────────────────────
    // Internal state for search + filter (kept separate for debouncing)
    // ─────────────────────────────────────────────────

    /**
     * The query value actually used for filtering in [combine].
     * Only updated after the 300ms debounce fires — NOT on every keystroke.
     * Starts as "" so the first combine emission fires immediately with no delay.
     */
    private val _searchQuery = MutableStateFlow("")

    /** Currently active filter (ALL or FAVORITES). */
    private val _filter = MutableStateFlow(MarketFilter.ALL)

    // ─────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────

    init {
        observeFilteredCoins()
        refresh()
    }

    /**
     * Combines the live coin list from Room with the current search query and filter.
     *
     * No debounce here — [_searchQuery] is already debounced in [onIntent] before it
     * is updated. This means coins show instantly on screen open without any 300ms delay.
     */
    private fun observeFilteredCoins() {
        viewModelScope.launch {
            combine(
                getCoins(),      // Live coin list from Room — emits immediately
                _searchQuery,    // Pre-debounced query — no artificial delay on first load
                _filter          // Currently selected filter tab
            ) { coins, query, filter ->

                // Apply the filter (ALL or FAVORITES)
                val afterFilter = when (filter) {
                    MarketFilter.ALL       -> coins
                    MarketFilter.FAVORITES -> coins.filter { it.isFavorite }
                }

                // Apply the text search (skip if query is empty)
                if (query.isBlank()) {
                    afterFilter
                } else {
                    val trimmed = query.trim()
                    afterFilter.filter { coin ->
                        // Match by full name ("Bitcoin") or symbol ("BTC"), case-insensitive
                        coin.name.contains(trimmed, ignoreCase = true) ||
                        coin.symbol.contains(trimmed, ignoreCase = true)
                    }
                }
            }.collect { filteredCoins ->
                // Push the filtered list into the state so the UI redraws
                _state.update { it.copy(coins = filteredCoins) }
            }
        }
    }

    /**
     * Fetches the latest coin prices from the CoinGecko API and saves them to Room.
     * After this completes, [observeFilteredCoins] will automatically emit the new data.
     */
    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = refreshCoins()) {
                is AppResult.Success -> {
                    // Data saved to Room — [observeFilteredCoins] handles the UI update
                    _state.update { it.copy(isLoading = false) }
                }
                is AppResult.Error -> {
                    // Show the error in state AND send a one-shot effect (e.g. for Snackbar)
                    val errorMsg = result.exception.message ?: "An error occurred"
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                    _effect.send(MarketEffect.ShowError(errorMsg))
                    if (result.exception is AppException.RateLimitException) {
                        startRateLimitCountdown(60)
                    }
                }
                AppResult.Loading -> Unit  // Already set isLoading = true above
            }
        }
    }
    private var countdownJob: Job? = null

    private fun startRateLimitCountdown(seconds: Int = 60) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _state.update { it.copy(rateLimitCountdown = remaining) }
                kotlinx.coroutines.delay(1_000)
            }
            _state.update { it.copy(rateLimitCountdown = 0) }
        }
    }

    // ─────────────────────────────────────────────────
    // Intent Handler — the single entry point for all UI events
    // ─────────────────────────────────────────────────

    /**
     * The UI calls this for every user action. The ViewModel decides what to do.
     *
     * This is the "I" in MVI — the UI sends Intents, never calls ViewModel methods directly.
     * Adding a new user action? Add it to [MarketIntent] and handle it here.
     *
     * @param intent The user action that occurred.
     */
    fun onIntent(intent: MarketIntent) {
        when (intent) {
            is MarketIntent.SearchChanged -> {
                // Update state immediately so the TextField shows the typed text without lag
                _state.update { it.copy(searchQuery = intent.query) }
                // Debounce before filtering — waits 300ms of silence after last keystroke
                // Cancels the previous debounce job if the user types again within 300ms
                searchDebounceJob?.cancel()
                searchDebounceJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(300)
                    _searchQuery.value = intent.query
                }
            }

            is MarketIntent.FilterChanged -> {
                // Update both the filter flow and the state
                _filter.value = intent.filter
                _state.update { it.copy(filter = intent.filter) }
            }

            is MarketIntent.FavoriteClicked -> {
                // Toggle favorite in Room. Room will emit an updated list automatically.
                viewModelScope.launch {
                    toggleFavorite(intent.coinId, intent.isFavorite)
                }
            }

            is MarketIntent.Refresh -> {
                // Pull-to-refresh or retry button tapped
                refresh()
            }

            is MarketIntent.CoinClicked -> {
                // Send a one-shot navigation effect to the UI
                viewModelScope.launch {
                    _effect.send(MarketEffect.NavigateToCoinDetail(intent.coinId))
                }
            }
        }
    }

}