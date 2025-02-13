package kv.compose.musicplayer.data.model

data class LocalTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String
)

fun LocalTrack.toTrack(): Track = Track(
    id = id,
    title = title,
    artist = Artist(0, artist),
    album = Album(
        id = 0,
        title = album,
        cover = "",
        coverSmall = "",
        coverMedium = "",
        coverBig = ""
    ),
    preview = path,
    duration = (duration / 1000).toInt()
)