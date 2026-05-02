package com.example.markettracker.di

import com.example.markettracker.data.api.Cryptoapi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module that provides all networking-related dependencies.
 *
 * @Module tells Hilt: "this object/class contains dependency providers."
 * @InstallIn(SingletonComponent::class) means: all @Provides functions here
 *   are scoped to the Application lifetime — created once, shared everywhere.
 *
 * WHY a module instead of RetrofitClient singleton object?
 *   - [CoinRepositoryImpl] can receive [Cryptoapi] via constructor injection.
 *   - Tests can provide a fake [Cryptoapi] without changing any production code.
 *   - Proper timeouts, logging, and future interceptors (auth, cache) are in one place.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides a configured [OkHttpClient] — the HTTP engine used by Retrofit.
     *
     * Configured with:
     * - 30 second timeouts (connection + read) to avoid hanging indefinitely
     * - HTTP logging interceptor that prints full request/response in DEBUG builds
     *   and is completely silent in RELEASE builds (no sensitive data in logs)
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // Logging interceptor — shows request URL, headers, and response body in Logcat
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // BODY logs everything including the full JSON response (great for debugging)
            // NONE logs nothing (important for privacy in release builds)
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)   // Time to establish a TCP connection
            .readTimeout(30, TimeUnit.SECONDS)      // Time to wait for server data
            .build()
    }

    /**
     * Provides a [Retrofit] instance configured to talk to the CoinGecko API.
     *
     * [GsonConverterFactory] automatically converts the JSON response body
     * into [CoinDto] Kotlin data classes using the @SerializedName annotations.
     *
     * @param okHttpClient Injected from [provideOkHttpClient] above. Hilt handles the wiring.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")   // CoinGecko free API base URL
            .client(okHttpClient)                            // Use our configured OkHttp client
            .addConverterFactory(GsonConverterFactory.create())  // JSON → Kotlin data class
            .build()
    }

    /**
     * Provides the [Cryptoapi] Retrofit service interface.
     *
     * Retrofit generates the implementation of [Cryptoapi] at runtime by reading
     * the @GET, @Query annotations and mapping them to HTTP requests.
     *
     * @param retrofit Injected from [provideRetrofit] above.
     */
    @Provides
    @Singleton
    fun provideCryptoApi(retrofit: Retrofit): Cryptoapi {
        return retrofit.create(Cryptoapi::class.java)
    }
}