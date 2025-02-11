package kv.compose.musicplayer.data.remote

import kv.compose.musicplayer.data.model.ChartResponse
import kv.compose.musicplayer.data.model.SearchResponse
import kv.compose.musicplayer.data.model.Track
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DeezerApi {
    @GET("chart")
    suspend fun getChart(): ChartResponse

    @GET("search")
    suspend fun searchTracks(@Query("q") query: String): SearchResponse

    @GET("track/{id}")
    suspend fun getTrack(@Path("id") id: Long): Track
}