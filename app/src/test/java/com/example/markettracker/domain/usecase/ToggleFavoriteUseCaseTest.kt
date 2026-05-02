package com.example.markettracker.domain.usecase

import com.example.markettracker.core.AppException
import com.example.markettracker.core.AppResult
import com.example.markettracker.domain.repository.CoinRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleFavoriteUseCaseTest {

    private lateinit var repository: CoinRepository
    private lateinit var useCase: ToggleFavoriteUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = ToggleFavoriteUseCase(repository)
    }

    @Test
    fun `invoke calls repository with correct coinId and currentState`() = runTest {
        coEvery { repository.toggleFavorite("bitcoin", false) } returns AppResult.Success(Unit)

        useCase("bitcoin", false)

        coVerify(exactly = 1) { repository.toggleFavorite("bitcoin", false) }
    }

    @Test
    fun `returns Success when repository succeeds`() = runTest {
        coEvery { repository.toggleFavorite(any(), any()) } returns AppResult.Success(Unit)

        val result = useCase("bitcoin", true)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `returns Error when repository fails`() = runTest {
        val exception = AppException.NetworkException(RuntimeException("No internet"))
        coEvery { repository.toggleFavorite(any(), any()) } returns AppResult.Error(exception)

        val result = useCase("bitcoin", false)

        assertTrue(result is AppResult.Error)
        assertEquals(
            "No internet connection. Check your network.",
            (result as AppResult.Error).exception.message
        )
    }
}
