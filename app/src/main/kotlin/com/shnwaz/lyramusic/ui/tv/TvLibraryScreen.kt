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
import androidx.tv.material3.Button
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
        var songs by remember { mutableStateOf<List<PlaylistSong>>(emptyList()) }
        LaunchedEffect(current.playlist.id) {
            songs = database.playlistSongs(current.playlist.id).first()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 32.dp, top = 16.dp, end = 32.dp),
            ) {
                Button(onClick = { selectedPlaylist = null }) {
                    Text("Back")
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = current.playlist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "${songs.size} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE8A33D),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(songs, key = { it.map.songId }) { item ->
                    val song = item.song
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val idx = songs.indexOf(item).coerceAtLeast(0)
                                playerConnection?.playQueue(
                                    ListQueue(
                                        items = songs.map { it.song.toMediaItem() },
                                        startIndex = idx,
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                Text(
                                    text = song.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = song.artists.joinToString { it.name },
                                    style = MaterialTheme.typography.bodySmall,
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
}
