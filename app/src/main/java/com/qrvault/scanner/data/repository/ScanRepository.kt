package com.qrvault.scanner.data.repository

import com.qrvault.scanner.data.local.ScanHistoryDao
import com.qrvault.scanner.data.local.ScanHistoryEntity
import com.qrvault.scanner.scanner.QrPayloadClassifier
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class ScanRepository(private val dao: ScanHistoryDao) {

    fun observeHistory(
        query: String = "",
        typeFilter: String? = null,
        onlyFavorites: Boolean = false
    ): Flow<List<ScanHistoryEntity>> {
        return if (query.isBlank() && typeFilter == null && !onlyFavorites) {
            dao.observeAll()
        } else {
            dao.searchScans(query.trim(), typeFilter, onlyFavorites)
        }
    }

    suspend fun saveDecodedQr(rawValue: String): ScanHistoryEntity {
        val parsed = QrPayloadClassifier.classify(rawValue)
        val entity = ScanHistoryEntity(
            rawValue = rawValue,
            payloadType = parsed.type,
            displayTitle = parsed.title
        )
        val insertedId = dao.insert(entity)
        Timber.i("RoomScanDao INSERT id=%d type=%s payload=%s", insertedId, parsed.type, rawValue)
        return entity.copy(id = insertedId)
    }

    suspend fun toggleFavorite(entity: ScanHistoryEntity) {
        val next = !entity.isFavorite
        dao.updateFavorite(entity.id, next)
        Timber.d("RoomScanDao UPDATE id=%d is_favorite=%s", entity.id, next)
    }

    suspend fun updateNote(id: Long, note: String) {
        dao.updateNote(id, note.trim())
        Timber.d("RoomScanDao UPDATE id=%d note_length=%d", id, note.trim().length)
    }

    suspend fun deleteScan(entity: ScanHistoryEntity) {
        dao.delete(entity)
        Timber.w("RoomScanDao DELETE id=%d type=%s", entity.id, entity.payloadType)
    }

    suspend fun clearAll() {
        dao.clearAll()
        Timber.w("RoomScanDao TRUNCATE scan_history")
    }
}
