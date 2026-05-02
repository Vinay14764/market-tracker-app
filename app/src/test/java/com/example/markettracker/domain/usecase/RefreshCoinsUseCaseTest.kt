package com.example.markettracker.domain.usecase

import com.example.markettracker.core.AppException
import com.example.markettracker.core.AppResult
import com.example.markettracker.domain.repository.CoinRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RefreshCoinsUseCaseTest {

    private lateinit var repository: CoinRepository
    private lateinit var useCase: RefreshCoinsUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = RefreshCoinsUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository refreshCoins`() = runTest {
        coEvery { repository.refreshCoins() } returns AppResult.Success(Unit)

        useCase()

        coVerify(exactly = 1) { repository.refreshCoins() }
    }

    @Test
    fun `returns Success when sync succeeds`() = runTest {
        coEvery { repository.refreshCoins() } returns AppResult.Success(Unit)

        val result = useCase()

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `returns RateLimitException when API returns 429`() = runTest {
        coEvery { repository.refreshCoins() } returns AppResult.Error(AppException.RateLimitException())

        val result = useCase()

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).exception is AppException.RateLimitException)
    }

    @Test
    fun `returns NetworkException on no internet`() = runTest {
        val cause = RuntimeException("timeout")
        coEvery { repository.refreshCoins() } returns AppResult.Error(AppException.NetworkException(cause))

        val result = useCase()

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).exception is AppException.NetworkException)
    }
}
