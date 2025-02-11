package kv.compose.musicplayer.domain.repository

import kotlinx.coroutines.flow.Flow
import kv.compose.musicplayer.data.model.Track
import kv.compose.musicplayer.domain.util.Result

interface MusicRepository {
    suspend fun getChartTracks(): Result<List<Track>>
    suspend fun searchTracks(query: String): Result<List<Track>>
    suspend fun getTrack(id: Long): Result<Track>
    fun getLocalTracks(): Flow<Result<List<Track>>>
    suspend fun searchLocalTracks(query: String): Result<List<Track>>
}