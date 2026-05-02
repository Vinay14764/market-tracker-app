package com.example.markettracker.domain.usecase

import com.example.markettracker.domain.model.Coin
import com.example.markettracker.domain.model.MarketFilter
import com.example.markettracker.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case: Filter and search the coin list by query text and market filter.
 *
 * This contains the search/filter BUSINESS LOGIC that used to live inside [MarketViewModel].
 * By extracting it here, it becomes independently testable — you can write unit tests
 * for search behavior without needing a ViewModel, Android context, or a running database.
 *
 * How it works:
 *   1. Gets all coins from [ICoinRepository.coins] (Room DB — always fresh)
 *   2. Applies the [MarketFilter] (ALL or FAVORITES)
 *   3. Applies the text [query] to filter by name or symbol (case-insensitive)
 *   4. Returns the filtered list as a reactive [Flow]
 *
 * @param query  The search text typed by the user. Blank = show all.
 * @param filter [MarketFilter.ALL] or [MarketFilter.FAVORITES].
 */
class SearchCoinsUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    /**
     * @param query  Text to search by coin name or symbol.
     * @param filter Whether to show all coins or only favorited ones.
     * @return A [Flow] that emits the filtered list whenever the DB changes.
     */
    operator fun invoke(query: String, filter: MarketFilter): Flow<List<Coin>> =
        repository.getCoins().map { coins ->

            // Step 1: Apply the favorites filter
            val afterFilterApplied = when (filter) {
                MarketFilter.ALL       -> coins
                MarketFilter.FAVORITES -> coins.filter { it.isFavorite }
            }

            // Step 2: Apply the text search (skip if query is blank)
            if (query.isBlank()) {
                afterFilterApplied
            } else {
                val trimmedQuery = query.trim()
                afterFilterApplied.filter { coin ->
                    // Match against both full name ("Bitcoin") and symbol ("BTC")
                    coin.name.contains(trimmedQuery, ignoreCase = true) ||
                    coin.symbol.contains(trimmedQuery, ignoreCase = true)
                }
            }
        }
}