package com.example.markettracker.ui.screens.dashboard

import com.example.markettracker.domain.model.Coin

/**
 * UI state for the Dashboard (Home) screen.
 *
 * Previously, TopGainersSection and TopLosersSection showed hardcoded data.
 * Now [DashboardViewModel] populates these lists using real Room data
 * (via [GetTopGainersUseCase] and [GetTopLosersUseCase]).
 *
 * The lists start empty and are populated automatically once the Market screen
 * (or the Dashboard itself) triggers a data refresh from the API.
 */
data class DashboardState(

    /**
     * Top N coins with the highest positive 24h price change.
     * Empty until data is loaded from Room. Shown in the "Top Gainers" section.
     */
    val topGainers: List<Coin> = emptyList(),

    /**
     * Top N coins with the biggest negative 24h price change.
     * Empty until data is loaded from Room. Shown in the "Top Losers" section.
     */
    val topLosers: List<Coin> = emptyList(),

    /** True while data is being loaded for the first time. */
    val isLoading: Boolean = false
)