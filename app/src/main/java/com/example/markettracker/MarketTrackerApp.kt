package com.example.markettracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Custom Application class required by Hilt.
 *
 * @HiltAndroidApp triggers Hilt's code generation and sets up the top-level
 * dependency injection component (SingletonComponent). This means all
 * @Singleton-scoped objects (Retrofit, Room, Repositories) are created once
 * and live as long as the application lives.
 *
 * Without this class registered in AndroidManifest.xml, Hilt won't work.
 */
@HiltAndroidApp
class MarketTrackerApp : Application()