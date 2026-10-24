/*
 * Lyra Music Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * TV library: local playlists from the Room database. Selecting a playlist
 * shows its songs (like the phone app); selecting a song plays it through
 * the existing MusicService queue.
 */

package com.shnwaz.lyramusic.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.shnwaz.lyramusic.LocalDatabase
import com.shnwaz.lyramusic.LocalPlayerConnection
import com.shnwaz.lyramusic.db.entities.Playlist
import com.shnwaz.lyramusic.db.entities.PlaylistSong
import com.shnwaz.lyramusic.extensions.toMediaItem
import com.shnwaz.lyramusic.playback.queues.ListQueue
import com.shnwaz.lyramusic.viewmodels.LibraryPlaylistsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvLibraryScreen() {
    val playerConnection = LocalPlayerConnection.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    val viewModel: LibraryPlaylistsViewModel = hiltViewModel()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    // null = playlist list; non-null = showing songs of that playlist
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    if (playlists.isEmpty()) {
        TvEmptyState("No playlists yet — like songs or create playlists in the phone app.")
        return
    }

    val current = selectedPlaylist
    if (current == null) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(playlists, key = { it.playlist.id }) { playlist ->
                TvMediaCard(
                    title = playlist.playlist.name,
                    subtitle = "${playlist.songCount} songs",
                    imageUrl = playlist.playlist.thumbnailUrl,
                    onClick = { selectedPlaylist = playlist },
                )
            }
        }
    } else {
        // Playlist detail: show songs. Back returns to playlist list.
        BackHandler { selectedPlaylist = null }
        var songs by remember(current.playlist.id) { mutableStateOf(emptyList<PlaylistSong>()) }
        LaunchedEffect(current.playlist.id) {
            songs = database.playlistSongs(current.playlist.id).first()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header with playlist name and back hint
            Text(
                text = current.playlist.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp),
            )
            Text(
                text = "${songs.size} songs — press Back for playlists",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE8A33D),
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(songs, key = { it.song.id }) { item ->
                    val song = item.song
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val idx = songs.indexOf(item)
                                val queueSongs = songs.map { it.song.toMediaItem() }
                                playerConnection?.playQueue(
                                    ListQueue(
                                        items = queueSongs,
                                        startIndex = idx.coerceAtLeast(0),
                                    ),
                                )
                            }
                            .padding(8.dp),
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artists.joinToString { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE8A33D),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
