package com.example.markettracker.di

import com.example.markettracker.data.repository.CoinRepositoryImpl
import com.example.markettracker.domain.repository.CoinRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository interfaces to their implementations.
 *
 * WHY @Binds instead of @Provides?
 *   @Binds is a compile-time hint to Hilt: "when someone asks for [ICoinRepository],
 *   give them [CoinRepositoryImpl]." It's more efficient than @Provides because
 *   Hilt doesn't need to generate a factory method body — it just creates an alias.
 *
 * WHY abstract class instead of object?
 *   @Binds must be in an abstract class (not an object) because it's an abstract function.
 *   Hilt generates the implementation at compile time.
 *
 * HOW IT WORKS:
 *   Whenever a class has @Inject constructor and needs [ICoinRepository],
 *   Hilt will automatically inject [CoinRepositoryImpl] (which is @Singleton).
 *
 *   Example in a ViewModel:
 *     class MarketViewModel @Inject constructor(
 *         private val repo: ICoinRepository  ← Hilt gives CoinRepositoryImpl here
 *     )
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Tells Hilt: ICoinRepository = CoinRepositoryImpl.
     * The @Singleton scope ensures only one instance is ever created.
     */
    @Binds
    @Singleton
    abstract fun bindCoinRepository(
        impl: CoinRepositoryImpl   // Hilt constructs this via @Inject constructor
    ): CoinRepository
}