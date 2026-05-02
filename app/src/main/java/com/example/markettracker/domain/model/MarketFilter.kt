package com.example.markettracker.domain.model

/**
 * Represents the active filter selection on the Market screen.
 *
 * Moved to the domain layer so it can be used by [SearchCoinsUseCase]
 * without creating a dependency on the UI package.
 *
 * [ALL]       - Display every coin fetched from the API.
 * [FAVORITES] - Display only coins the user has marked as favorite (isFavorite = true).
 */
enum class MarketFilter {
    ALL,
    FAVORITES
}