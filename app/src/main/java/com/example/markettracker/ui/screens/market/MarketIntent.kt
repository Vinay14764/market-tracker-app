package com.example.markettracker.ui.screens.market

import com.example.markettracker.domain.model.MarketFilter

/**
 * All possible user actions (intents) on the Market screen.
 *
 * This is the "Intent" part of MVI (Model-View-Intent).
 *
 * WHY a sealed class?
 *   Every user interaction on the Market screen is one of these — nothing else.
 *   The ViewModel receives intents via [MarketViewModel.onIntent] and decides
 *   what to do with each one. The UI never calls multiple ViewModel methods directly;
 *   it always sends an intent.
 *
 * HOW TO ADD A NEW ACTION:
 *   1. Add a new subclass here (e.g. `data class SortChanged(val order: SortOrder)`)
 *   2. Handle it in [MarketViewModel.onIntent]
 *   3. Update [MarketState] if needed
 *   → The UI composable doesn't change if it already sends the intent correctly.
 */
sealed class MarketIntent {

    /** User typed text in the search bar. [query] is the full current text. */
    data class SearchChanged(val query: String) : MarketIntent()

    /** User tapped a filter chip (ALL or FAVORITES). */
    data class FilterChanged(val filter: MarketFilter) : MarketIntent()

    /**
     * User tapped the heart/favorite icon on a coin row.
     * @param coinId       The coin whose status should be toggled.
     * @param isFavorite   The coin's CURRENT status (will be flipped by the ViewModel).
     */
    data class FavoriteClicked(val coinId: String, val isFavorite: Boolean) : MarketIntent()

    /** User pulled to refresh or tapped a retry button. Re-fetch from API. */
    data object Refresh : MarketIntent()

    /**
     * User tapped on a coin row to view its detail.
     * Triggers a [MarketEffect.NavigateToCoinDetail] one-shot event.
     */
    data class CoinClicked(val coinId: String) : MarketIntent()
}