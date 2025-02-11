package kv.compose.musicplayer.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kv.compose.musicplayer.data.model.Track
import kv.compose.musicplayer.data.remote.DeezerApi
import kv.compose.musicplayer.domain.repository.MusicRepository
import kv.compose.musicplayer.domain.util.Result
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class MusicRepositoryImpl @Inject constructor(
    private val api: DeezerApi,
    private val context: Context,
) : MusicRepository {

    override suspend fun getChartTracks(): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val tracks = api.getChart().tracks.data
            Result.Success(tracks)
        } catch (e: HttpException) {
            Result.Error("Network error: ${e.message}")
        } catch (e: IOException) {
            Result.Error("IO error: ${e.message}")
        } catch (e: Exception) {
            Result.Error("Unknown error: ${e.message}")
        }
    }

    override suspend fun searchTracks(query: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val tracks = api.searchTracks(query).data
            Result.Success(tracks)
        } catch (e: HttpException) {
            Result.Error("Network error: ${e.message}")
        } catch (e: IOException) {
            Result.Error("IO error: ${e.message}")
        } catch (e: Exception) {
            Result.Error("Unknown error: ${e.message}")
        }
    }

    override suspend fun getTrack(id: Long): Result<Track> = withContext(Dispatchers.IO)
    {
        try {
            Result.Success(api.getTrack(id))
        } catch (e: HttpException) {
            Result.Error("Network error: ${e.message}")
        } catch (e: IOException) {
            Result.Error("IO error: ${e.message}")
        } catch (e: Exception) {
            Result.Error("Unknown error: ${e.message}")
        }
    }

    override fun getLocalTracks(): Flow<Result<List<Track>>> {
        TODO("Not yet implemented")
    }

    override suspend fun searchLocalTracks(query: String): Result<List<Track>> {
        TODO("Not yet implemented")
    }
}