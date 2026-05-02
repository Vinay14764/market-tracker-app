package com.example.markettracker.ui.screens.market

import com.example.markettracker.domain.model.Coin

/**
 * UI state for the Coin Detail screen.
 *
 * Replaces the old hardcoded data in [CoinDetailScreen].
 * Previously the screen always showed "Bitcoin" with a hardcoded price.
 * Now it shows the actual coin the user tapped on, loaded from Room.
 *
 * [coin] is null while data is loading. The UI should show a loading
 * indicator when [isLoading] is true and [coin] is null.
 */
data class CoinDetailState(

    /**
     * The coin data to display. Null until [CoinDetailViewModel] loads it from Room.
     * The screen should show a skeleton/loading state when this is null.
     */
    val coin: Coin? = null,

    /** True while the coin data is being fetched. */
    val isLoading: Boolean = true,

    /**
     * Error message if the coin couldn't be loaded (e.g. unknown coinId).
     * Null = no error.
     */
    val error: String? = null,

    /**
     * The currently selected chart timeframe.
     * Used by the timeframe buttons (15m, 30m, 1H, 4H, 1D).
     * Only the selected button is highlighted in the UI.
     */
    val selectedTimeframe: String = "1H",
    // Add these two ↓
    val description: String? = null,
    val isDescriptionLoading: Boolean = false

)