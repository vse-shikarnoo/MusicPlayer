package kv.compose.musicplayer.data.repository

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kv.compose.musicplayer.data.model.LocalTrack
import kv.compose.musicplayer.data.model.Track
import kv.compose.musicplayer.data.model.toTrack
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

    override suspend fun searchTracks(query: String): Result<List<Track>> =
        withContext(Dispatchers.IO) {
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

    override fun getLocalTracks(): Flow<Result<List<Track>>> = flow {
        emit(Result.Loading)
        try {
            val tracks = queryLocalTracks()
            emit(Result.Success(tracks.map { it.toTrack() }))
        } catch (e: Exception) {
            emit(Result.Error("Error loading local tracks: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun searchLocalTracks(query: String): Result<List<Track>> =
        withContext(Dispatchers.IO) {
            try {
                val tracks = queryLocalTracks()
                    .filter { track ->
                        track.title.contains(query, ignoreCase = true) ||
                                track.artist.contains(query, ignoreCase = true) ||
                                track.album.contains(query, ignoreCase = true)
                    }
                    .map { it.toTrack() }
                Result.Success(tracks)
            } catch (e: Exception) {
                Result.Error("Error searching local tracks: ${e.message}")
            }
        }

    private fun queryLocalTracks(): List<LocalTrack> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        return context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            buildList {
                while (cursor.moveToNext()) {
                    add(
                        LocalTrack(
                            id = cursor.getLong(idColumn),
                            title = cursor.getString(titleColumn),
                            artist = cursor.getString(artistColumn),
                            album = cursor.getString(albumColumn),
                            duration = cursor.getLong(durationColumn),
                            path = cursor.getString(pathColumn)
                        )
                    )
                }
            }
        } ?: emptyList()
    }
}
