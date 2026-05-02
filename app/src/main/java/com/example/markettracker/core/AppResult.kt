package com.example.markettracker.core

/**
 * A generic wrapper for the result of any operation (network call, DB query, etc.).
 *
 * WHY: Instead of throwing raw exceptions or returning null, every data operation
 * returns an [AppResult]. The UI then handles each case explicitly:
 *
 *   when (result) {
 *       is AppResult.Success -> showData(result.data)
 *       is AppResult.Error   -> showError(result.exception.message)
 *       is AppResult.Loading -> showSpinner()
 *   }
 *
 * This removes silent failures (no more e.printStackTrace()) and makes every
 * possible outcome visible to the caller at compile time.
 *
 * @param T The type of the successful result value.
 */
sealed class AppResult<out T> {

    /**
     * The operation completed successfully.
     * @param data The returned value (e.g. list of coins, Unit for write operations).
     */
    data class Success<T>(val data: T) : AppResult<T>()

    /**
     * The operation failed.
     * @param exception A typed [AppException] describing what went wrong.
     *   Use its subtypes to show specific error messages in the UI.
     */
    data class Error(val exception: AppException) : AppResult<Nothing>()

    /**
     * The operation is still in progress.
     * Emit this from a Flow while waiting for the result.
     */
    data object Loading : AppResult<Nothing>()
}