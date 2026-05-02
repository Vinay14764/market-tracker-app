package com.example.markettracker.ui.screens.market

import com.example.markettracker.domain.model.Coin
import com.example.markettracker.domain.model.MarketFilter

/**
 * Represents the complete UI state of the Market screen at any given moment.
 *
 * This is the "State" part of MVI (Model-View-Intent).
 *
 * WHY a single state object instead of many separate StateFlows?
 *   - The UI always renders a consistent snapshot. You can't have [isLoading = true]
 *     and [coins = listOf(...)] at the same time because they'd conflict.
 *   - A single state makes it trivial to unit test: "given state X, does the UI look right?"
 *   - [StateFlow<MarketState>] in the ViewModel replaces the old mix of
 *     [mutableStateOf(false)] for isLoading and [StateFlow] for coins.
 *
 * Immutable by design: use [copy()] to create updated versions.
 *   Example: _state.update { it.copy(isLoading = false) }
 */
data class MarketState(

    /** The filtered+searched list of coins currently shown in the list. */
    val coins: List<Coin> = emptyList(),

    /** The current text in the search bar. Kept in state so the UI stays in sync. */
    val searchQuery: String = "",

    /** The currently active filter tab (ALL or FAVORITES). */
    val filter: MarketFilter = MarketFilter.ALL,

    /** True while coins are being fetched from the network for the first time. */
    val isLoading: Boolean = false,

    /**
     * An error message to show if the network request failed.
     * Null = no error. Non-null = show this message to the user.
     */
    val error: String? = null,
    val rateLimitCountdown: Int = 0
)