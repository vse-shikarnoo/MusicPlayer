package kv.compose.musicplayer.data.model

data class Track(
    val id: Long,
    val title: String,
    val artist: Artist,
    val album: Album,
    val preview: String,
    val duration: Int
)

data class Artist(
    val id: Long,
    val name: String
)

data class Album(
    val id: Long,
    val title: String,
    val cover: String,
    val coverSmall: String,
    val coverMedium: String,
    val coverBig: String
)

data class ChartResponse(
    val tracks: TrackList
)

data class SearchResponse(
    val data: List<Track>
)

data class TrackList(
    val data: List<Track>
)