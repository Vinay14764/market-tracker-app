package com.example.markettracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The main Room database for the app.
 *
 * [entities] lists every table in the database. Each @Entity class = one table.
 * [version] must be incremented whenever the schema changes (add/remove/rename columns).
 *   If you bump the version without providing a Migration, Room will crash —
 *   unless fallbackToDestructiveMigration() is used (which drops all data).
 *
 * This class is a singleton — Hilt creates exactly one instance via [DatabaseModule].
 * Do NOT instantiate this class directly anywhere in the app.
 */
@Database(
    entities = [CoinEntity::class],
    version = 3   // bumped from 2: added sparklineJson column to CoinEntity
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Returns the DAO for coin operations.
     * Room auto-generates the implementation of [CoinDao] at compile time.
     */
    abstract fun coinDto(): CoinDao
}