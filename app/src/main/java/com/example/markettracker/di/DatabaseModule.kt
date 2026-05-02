package com.example.markettracker.di

import android.content.Context
import androidx.room.Room
import com.example.markettracker.data.db.AppDatabase
import com.example.markettracker.data.db.CoinDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides Room database and DAO dependencies.
 *
 * Replaces the old [DatabaseProvider] singleton object.
 * Now [AppDatabase] and [CoinDao] are injected via Hilt wherever needed,
 * making them easily replaceable with in-memory databases in tests.
 *
 * @ApplicationContext is a Hilt qualifier — it injects the application-level
 * Android [Context] so we don't accidentally leak an Activity context into
 * a singleton database instance.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the single [AppDatabase] instance for the entire app.
     *
     * [Room.databaseBuilder] creates a persistent SQLite database on disk.
     * [fallbackToDestructiveMigration] drops and recreates all tables if Room
     * detects a schema change without a matching Migration object.
     *
     * ⚠️ WARNING: [fallbackToDestructiveMigration] will WIPE user data (including favorites)
     * if you change [AppDatabase] version without adding an explicit Migration.
     * Before release: replace this with proper Migration objects in [AppDatabase].
     *
     * @param context Application context used to find the database file on disk.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "market_db"           // Database file name on disk
        )
        .fallbackToDestructiveMigration()  // See warning above
        .build()
    }

    /**
     * Provides the [CoinDao] from the database.
     *
     * Room generates the DAO implementation at compile time. We don't create it manually —
     * we just call [AppDatabase.coinDto] which returns the Room-generated instance.
     *
     * @param db Injected from [provideAppDatabase] above.
     */
    @Provides
    @Singleton
    fun provideCoinDao(db: AppDatabase): CoinDao {
        return db.coinDto()
    }
}