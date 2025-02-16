package kv.compose.musicplayer.data.repository

import kv.compose.musicplayer.data.model.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackListRepository @Inject constructor() {

    private var tracks: List<Track> = emptyList()
    private var currentTrackId = -1L
    private var currentTrackIndex = -1

    fun setCurrentTrackId(newId: Long) {
        currentTrackId = newId
        currentTrackIndex = tracks.indexOfFirst {
            it.id == currentTrackId
        }
    }

    fun getCurrentTrack(): Track {
        return tracks[currentTrackIndex]
    }

    fun updateTracks(newTracks: List<Track>) {
        tracks = newTracks
    }

    fun nextTrack(): Track {
        if (currentTrackIndex != tracks.size - 1) {
            currentTrackIndex++

        } else {
            currentTrackIndex = 0
        }
        return tracks[currentTrackIndex]
    }

    fun prevTrack(): Track {
        if (currentTrackIndex != 0) {
            currentTrackIndex--
        } else {
            currentTrackIndex = tracks.size - 1
        }
        return tracks[currentTrackIndex]
    }


}