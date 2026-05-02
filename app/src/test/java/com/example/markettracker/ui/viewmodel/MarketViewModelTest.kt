package com.example.markettracker.ui.viewmodel

import app.cash.turbine.test
import com.example.markettracker.core.AppException
import com.example.markettracker.core.AppResult
import com.example.markettracker.domain.model.Coin
import com.example.markettracker.domain.model.MarketFilter
import com.example.markettracker.domain.usecase.GetCoinsUseCase
import com.example.markettracker.domain.usecase.RefreshCoinsUseCase
import com.example.markettracker.domain.usecase.ToggleFavoriteUseCase
import com.example.markettracker.ui.screens.market.MarketEffect
import com.example.markettracker.ui.screens.market.MarketIntent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarketViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getCoins: GetCoinsUseCase
    private lateinit var refreshCoins: RefreshCoinsUseCase
    private lateinit var toggleFavorite: ToggleFavoriteUseCase

    // MutableStateFlow lets us push new coin lists mid-test to simulate DB updates
    private val coinsFlow = MutableStateFlow<List<Coin>>(emptyList())

    private val bitcoin = Coin("bitcoin", "Bitcoin", "BTC", "", 50000.0, 2.5, isFavorite = false)
    private val ethereum = Coin("ethereum", "Ethereum", "ETH", "", 3000.0, -1.2, isFavorite = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getCoins = mockk()
        refreshCoins = mockk()
        toggleFavorite = mockk()

        every { getCoins() } returns coinsFlow
        coEvery { refreshCoins() } returns AppResult.Success(Unit)
        coEvery { toggleFavorite(any(), any()) } returns AppResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = MarketViewModel(getCoins, refreshCoins, toggleFavorite)

    // ─── Loading state ────────────────────────────────────────────────────────

    @Test
    fun `initial state shows loading then clears it after refresh`() = runTest {
        val vm = buildViewModel()

        vm.state.test {
            assertTrue(awaitItem().isLoading)

            advanceUntilIdle()

            assertFalse(awaitItem().isLoading)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error state is set and cleared on retry success`() = runTest {
        coEvery { refreshCoins() } returns AppResult.Error(
            AppException.NetworkException(RuntimeException("No internet"))
        )

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("No internet connection. Check your network.", vm.state.value.error)

        coEvery { refreshCoins() } returns AppResult.Success(Unit)
        vm.onIntent(MarketIntent.Refresh)
        advanceUntilIdle()

        assertNull(vm.state.value.error)
    }

    // ─── Coin list ────────────────────────────────────────────────────────────

    @Test
    fun `coins from repository appear in state`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        coinsFlow.value = listOf(bitcoin, ethereum)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.coins.size)
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    @Test
    fun `SearchChanged updates searchQuery in state immediately`() = runTest {
        val vm = buildViewModel()

        vm.onIntent(MarketIntent.SearchChanged("bit"))

        assertEquals("bit", vm.state.value.searchQuery)
    }

    @Test
    fun `search filters coins by name after debounce`() = runTest {
        coinsFlow.value = listOf(bitcoin, ethereum)
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(MarketIntent.SearchChanged("bitcoin"))
        advanceUntilIdle()

        assertEquals(1, vm.state.value.coins.size)
        assertEquals("bitcoin", vm.state.value.coins.first().id)
    }

    @Test
    fun `clearing search query shows all coins again`() = runTest {
        coinsFlow.value = listOf(bitcoin, ethereum)
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(MarketIntent.SearchChanged("bitcoin"))
        advanceUntilIdle()

        vm.onIntent(MarketIntent.SearchChanged(""))
        advanceUntilIdle()

        assertEquals(2, vm.state.value.coins.size)
    }

    // ─── Filter ───────────────────────────────────────────────────────────────

    @Test
    fun `FilterChanged to FAVORITES shows only favorite coins`() = runTest {
        coinsFlow.value = listOf(bitcoin, ethereum) // only ethereum is favorite
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(MarketIntent.FilterChanged(MarketFilter.FAVORITES))
        advanceUntilIdle()

        assertEquals(MarketFilter.FAVORITES, vm.state.value.filter)
        assertEquals(1, vm.state.value.coins.size)
        assertEquals("ethereum", vm.state.value.coins.first().id)
    }

    @Test
    fun `FilterChanged back to ALL shows all coins`() = runTest {
        coinsFlow.value = listOf(bitcoin, ethereum)
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(MarketIntent.FilterChanged(MarketFilter.FAVORITES))
        advanceUntilIdle()
        vm.onIntent(MarketIntent.FilterChanged(MarketFilter.ALL))
        advanceUntilIdle()

        assertEquals(2, vm.state.value.coins.size)
    }

    // ─── Favorite toggle ──────────────────────────────────────────────────────

    @Test
    fun `FavoriteClicked calls toggleFavorite with correct arguments`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(MarketIntent.FavoriteClicked("bitcoin", false))
        advanceUntilIdle()

        coVerify(exactly = 1) { toggleFavorite("bitcoin", false) }
    }

    // ─── Effects ──────────────────────────────────────────────────────────────

    @Test
    fun `CoinClicked emits NavigateToCoinDetail effect`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(MarketIntent.CoinClicked("bitcoin"))
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is MarketEffect.NavigateToCoinDetail)
            assertEquals("bitcoin", (effect as MarketEffect.NavigateToCoinDetail).coinId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `network error emits ShowError effect`() = runTest {
        coEvery { refreshCoins() } returns AppResult.Error(
            AppException.NetworkException(RuntimeException())
        )

        val vm = buildViewModel()

        vm.effect.test {
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is MarketEffect.ShowError)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rate limit error sets rateLimitCountdown`() = runTest {
        coEvery { refreshCoins() } returns AppResult.Error(AppException.RateLimitException())

        val vm = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.rateLimitCountdown > 0)
    }
}
