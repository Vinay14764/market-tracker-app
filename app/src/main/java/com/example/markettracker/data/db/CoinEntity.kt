package com.example.markettracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coins")
data class CoinEntity(
    @PrimaryKey
    val id : String,

    val name : String,
    val symbol : String,
    val image : String,
    val price : Double,
    val change : Double,
    val isFavorite : Boolean = false,

    // Sparkline prices serialised as a JSON array (e.g. "[100.0,101.5,...]").
    // Stored as plain text so Room doesn't need a TypeConverter.
    // Populated from SparklineDto.price when the API response is saved.
    val sparklineJson: String = "[]"
)