package com.example.markettracker.data.repository

import com.example.markettracker.core.AppException
import com.example.markettracker.core.AppResult
import com.example.markettracker.data.api.Cryptoapi
import com.example.markettracker.data.db.CoinDao
import com.example.markettracker.data.db.toDomain
import com.example.markettracker.data.db.toEntity
import com.example.markettracker.domain.model.Coin
import com.example.markettracker.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth for all coin data.
 *
 *
 * RESPONSIBILITIES:
 *   1. Fetch data from the CoinGecko API via [Cryptoapi] (Retrofit)
 *   2. Persist it to the local [CoinDao] (Room database)
 *   3. Expose a reactive [Flow] of domain [Coin] objects to the rest of the app
 *   4. Map typed exceptions into [AppException] subtypes
 *
 * WHY @Singleton?
 *   There should only be ONE repository instance. If two ViewModels both had their
 *   own CoinRepositoryImpl, they'd have separate in-memory state and could conflict.
 *   @Singleton + @Inject ensures Hilt creates exactly one instance and reuses it.
 *
 * @param api Retrofit service for CoinGecko API calls. Injected by Hilt via [NetworkModule].
 * @param dao Room DAO for local database operations. Injected by Hilt via [DatabaseModule].
 */
@Singleton
class CoinRepositoryImpl @Inject constructor(
    private val api: Cryptoapi,
    private val dao: CoinDao
) : CoinRepository {

    override fun getCoins(): Flow<List<Coin>> {
       return dao.getCoins()
           .map { list -> list.map { it.toDomain() } }
    }

    override
    suspend fun refreshCoins() : AppResult<Unit> {
        return try {
            val apiCoins = api.getCoins()
            val favorites = dao.getFavoriteIds().toSet()
            val entities = apiCoins.map { dto ->
                dto.toEntity().copy(isFavorite = dto.id in favorites)
            }
            dao.insertCoins(entities)
            AppResult.Success(Unit)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 429) AppResult.Error(AppException.RateLimitException())
            else AppResult.Error(AppException.NetworkException(e))
        } catch (e: IOException) {
            AppResult.Error(AppException.NetworkException(e))
        }
    }
    override fun getCoinById(id: String): Flow<Coin?> {
        return dao.getCoinById(id)
            .map { it?.toDomain() }
    }


    // 🔥 For detail screen (sparkline)
    override suspend fun getCoinDescription(id: String): String {
        val info = api.getCoinInfo(id)
        return  info.description?.en
            .orEmpty()
            .replace(Regex("<[^>]*>"), "")
            .trim()

    }
    override suspend fun toggleFavorite(coinId: String, currentState: Boolean): AppResult<Unit> {
        return try {
            dao.updateFavorite(coinId, !currentState)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppException.UnknownException(e))
        }
    }
}