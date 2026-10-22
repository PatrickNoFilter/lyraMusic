/*
 * Lyra Music Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * TV library: local playlists from the Room database. Selecting a playlist
 * plays its songs through the existing MusicService queue.
 */

package com.shnwaz.lyramusic.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import com.shnwaz.lyramusic.LocalDatabase
import com.shnwaz.lyramusic.LocalPlayerConnection
import com.shnwaz.lyramusic.extensions.toMediaItem
import com.shnwaz.lyramusic.playback.queues.ListQueue
import com.shnwaz.lyramusic.viewmodels.LibraryPlaylistsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun TvLibraryScreen() {
    val playerConnection = LocalPlayerConnection.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    val viewModel: LibraryPlaylistsViewModel = hiltViewModel()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    if (playlists.isEmpty()) {
        TvEmptyState("No playlists yet — like songs or create playlists in the phone app.")
        return
    }

    TvLazyColumn(
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(playlists, key = { it.playlist.id }) { playlist ->
            TvMediaCard(
                title = playlist.playlist.name,
                subtitle = "${playlist.songCount} songs",
                imageUrl = playlist.playlist.thumbnailUrl,
                onClick = {
                    scope.launch {
                        val songs =
                            database.playlistSongs(playlist.playlist.id).first()
                        if (songs.isNotEmpty()) {
                            playerConnection?.playQueue(
                                ListQueue(items = songs.map { it.song.toMediaItem() }),
                            )
                        }
                    }
                },
            )
        }
    }
}
