package com.example.markettracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the "coins" table in Room.
 *
 * A DAO is an interface that defines all SQL operations on a table.
 * Room generates the actual implementation at compile time using KSP.
 *
 * All methods that return [Flow] are REACTIVE — Room will automatically
 * re-emit the latest data whenever the underlying table changes. This is
 * what makes the UI update in real time without polling.
 */
@Dao
interface CoinDao {

    /**
     * Insert (or replace) a list of coins in the database.
     * [OnConflictStrategy.REPLACE] means: if a coin with the same [CoinEntity.id]
     * already exists, overwrite it with the new data (price, change, etc.).
     *
     * Called by [CoinRepositoryImpl.refreshCoins] after a successful API response.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoins(coins: List<CoinEntity>)

    /**
     * Get all coins from the database as a live [Flow].
     * Emits the full list immediately on collection, then re-emits whenever
     * any coin is inserted, updated, or deleted.
     */
    @Query("SELECT * FROM coins")
    fun getCoins(): Flow<List<CoinEntity>>

    /**
     * Get a single coin by its unique [coinId] as a live [Flow].
     * Returns null if no coin with that id exists yet in the database.
     * Used by [CoinDetailViewModel] to power the detail screen with real data.
     *
     * @param coinId The CoinGecko id, e.g. "bitcoin", "ethereum".
     */
    @Query("SELECT * FROM coins WHERE id = :coinId LIMIT 1")
    fun getCoinById(coinId: String): Flow<CoinEntity?>

    /**
     * Update the favorite status of a single coin identified by [coinId].
     * Called when the user taps the heart icon on the Market screen.
     *
     * @param coinId The coin to update.
     * @param isFav  The NEW favorite status to set (true = favorited, false = not).
     */
    @Query("UPDATE coins SET isFavorite = :isFav WHERE id = :coinId")
    suspend fun updateFavorite(coinId: String, isFav: Boolean)

    @Query("SELECT id FROM coins WHERE isFavorite = 1")
    suspend fun getFavoriteIds(): List<String>
}