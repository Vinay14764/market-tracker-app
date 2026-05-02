package com.example.markettracker.domain.usecase

import com.example.markettracker.core.AppResult
import com.example.markettracker.domain.repository.CoinRepository
import javax.inject.Inject

/**
 * Use case: Toggle the favorite/starred status of a single coin.
 *
 * The user taps the heart icon on the Market screen → the ViewModel
 * calls this use case → it flips the [isFavorite] flag in Room DB →
 * Room emits the updated list → UI recomposes automatically.
 *
 * WHY pass [currentState] instead of the [Coin] object?
 *   - The use case doesn't need to know about the full [Coin] model.
 *   - We only need the ID to find the record and the current value to flip it.
 *   - This keeps the interface clean and avoids passing heavy objects.
 *
 * @param coinId       The unique ID of the coin to update (e.g. "bitcoin").
 * @param currentState The coin's current favorite status. This will be flipped.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    suspend operator fun invoke(coinId: String, currentState: Boolean): AppResult<Unit> =
        repository.toggleFavorite(coinId, currentState)
}