package com.example.markettracker.domain.usecase

import app.cash.turbine.test
import com.example.markettracker.domain.model.Coin
import com.example.markettracker.domain.model.MarketFilter
import com.example.markettracker.domain.repository.CoinRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchCoinsUseCaseTest {

    private lateinit var repository: CoinRepository
    private lateinit var useCase: SearchCoinsUseCase

    private val bitcoin = Coin("bitcoin", "Bitcoin", "BTC", "", 50000.0, 2.5, isFavorite = true)
    private val ethereum = Coin("ethereum", "Ethereum", "ETH", "", 3000.0, -1.2, isFavorite = false)
    private val dogecoin = Coin("dogecoin", "Dogecoin", "DOGE", "", 0.15, 5.0, isFavorite = true)

    @Before
    fun setUp() {
        repository = mockk()
        useCase = SearchCoinsUseCase(repository)
        every { repository.getCoins() } returns flowOf(listOf(bitcoin, ethereum, dogecoin))
    }

    @Test
    fun `blank query with ALL filter returns all coins`() = runTest {
        useCase("", MarketFilter.ALL).test {
            val result = awaitItem()
            assertEquals(3, result.size)
            awaitComplete()
        }
    }

    @Test
    fun `blank query with FAVORITES filter returns only favorites`() = runTest {
        useCase("", MarketFilter.FAVORITES).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.isFavorite })
            awaitComplete()
        }
    }

    @Test
    fun `query matches by coin name case-insensitively`() = runTest {
        useCase("bit", MarketFilter.ALL).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("bitcoin", result.first().id)
            awaitComplete()
        }
    }

    @Test
    fun `query matches by symbol case-insensitively`() = runTest {
        useCase("eth", MarketFilter.ALL).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("ethereum", result.first().id)
            awaitComplete()
        }
    }

    @Test
    fun `query with no match returns empty list`() = runTest {
        useCase("solana", MarketFilter.ALL).test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `query with FAVORITES filter applies both filters`() = runTest {
        // "coin" matches Bitcoin + Dogecoin by name, both are favorites
        useCase("coin", MarketFilter.FAVORITES).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.isFavorite })
            awaitComplete()
        }
    }

    @Test
    fun `whitespace-only query returns all coins`() = runTest {
        useCase("   ", MarketFilter.ALL).test {
            val result = awaitItem()
            assertEquals(3, result.size)
            awaitComplete()
        }
    }
}
