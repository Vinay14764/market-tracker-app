package com.example.markettracker.domain.usecase

import com.example.markettracker.core.AppResult
import com.example.markettracker.domain.repository.CoinRepository
import javax.inject.Inject

/**
 * Use case: Fetch the latest coin prices from the CoinGecko API and
 * save them to the local Room database.
 *
 * This is the "sync" operation — it pulls fresh data from the network
 * and stores it locally. After this completes, [GetCoinsUseCase] will
 * automatically emit the updated list (because Room emits on every change).
 *
 * Returns [AppResult.Success] if the sync worked, [AppResult.Error] if it failed.
 * The ViewModel uses the result to show a loading spinner or an error message.
 */
class RefreshCoinsUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    /**
     * Fetches fresh data from the API and saves it to the local DB.
     * This is a suspend function — call it inside a coroutine (e.g. viewModelScope.launch).
     */
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshCoins()
}