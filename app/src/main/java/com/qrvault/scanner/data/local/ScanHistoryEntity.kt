package com.qrvault.scanner.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_history",
    indices = [Index(value = ["scanned_at"])]
)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "raw_value")
    val rawValue: String,

    @ColumnInfo(name = "payload_type")
    val payloadType: String,

    @ColumnInfo(name = "display_title")
    val displayTitle: String,

    @ColumnInfo(name = "scanned_at")
    val scannedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "note")
    val note: String = ""
)
