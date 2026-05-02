package com.example.markettracker.core

/**
 * Typed error hierarchy for the MarketTracker app.
 *
 * WHY: Catching raw [Exception] gives you no information about WHAT went wrong.
 * With this hierarchy, a 429 rate-limit error from CoinGecko is a different type
 * than a JSON parse failure or a timeout — so the UI can show the right message.
 *
 * Usage in Repository:
 *   try { ... }
 *   catch (e: IOException)       { AppException.NetworkException(e) }
 *   catch (e: HttpException)     { if (e.code() == 429) AppException.RateLimitException() ... }
 *   catch (e: JsonSyntaxException) { AppException.ParseException(e) }
 *   catch (e: Exception)         { AppException.UnknownException(e) }
 */
sealed class AppException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * The device has no internet connection, or the request timed out.
     * Show: "No internet connection. Check your network."
     */
    class NetworkException(cause: Throwable) :
        AppException("No internet connection. Check your network.", cause)

    /**
     * The CoinGecko server responded with a non-2xx HTTP status code.
     * [code] holds the HTTP status (e.g. 500), [body] holds the response text if any.
     * Show: "Something went wrong on the server (HTTP [code])."
     */
    class ServerException(val code: Int, val body: String?) :
        AppException("Server error: HTTP $code")

    /**
     * CoinGecko's free tier rate limit was exceeded (HTTP 429).
     * The free tier allows ~10-30 calls/minute. Wait before retrying.
     * Show: "Too many requests. Please wait a moment."
     */
    class RateLimitException :
        AppException("Too many requests. Please wait a moment.")

    /**
     * The server returned data that couldn't be parsed as expected JSON.
     * Usually happens when the API changes its response format.
     * Show: "Failed to read server data. Please try again."
     */
    class ParseException(cause: Throwable) :
        AppException("Failed to read server data. Please try again.", cause)

    /**
     * Any error that doesn't fit the above categories.
     * Wrap the original [cause] so it can be logged/inspected.
     * Show: "An unknown error occurred."
     */
    class UnknownException(cause: Throwable) :
        AppException("An unknown error occurred.", cause)
}