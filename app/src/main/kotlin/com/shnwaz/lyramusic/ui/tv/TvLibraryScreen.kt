/*
 * Lyra Music Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * TV library: local playlists from the Room database. Tapping a playlist
 * expands it to show its songs; tapping a song plays from that position.
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shnwaz.lyramusic.LocalDatabase
import com.shnwaz.lyramusic.LocalPlayerConnection
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

    var expandedId by remember { mutableStateOf<String?>(null) }

    if (playlists.isEmpty()) {
        TvEmptyState("No playlists yet — like songs or create playlists in the phone app.")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(playlists, key = { it.playlist.id }) { playlist ->
            val isExpanded = expandedId == playlist.playlist.id
            Column {
                TvMediaCard(
                    title = playlist.playlist.name,
                    subtitle = "${playlist.songCount} songs",
                    imageUrl = playlist.playlist.thumbnailUrl,
                    onClick = {
                        expandedId = if (isExpanded) null else playlist.playlist.id
                    },
                )
                if (isExpanded) {
                    var songs by remember { mutableStateOf<List<PlaylistSong>>(emptyList()) }
                    LaunchedEffect(playlist.playlist.id) {
                        songs = database.playlistSongs(playlist.playlist.id).first()
                    }
                    Spacer(Modifier.height(8.dp))
                    songs.forEachIndexed { idx, item ->
                        val song = item.song
                        Button(
                            onClick = {
                                playerConnection?.playQueue(
                                    ListQueue(
                                        items = songs.map { it.song.toMediaItem() },
                                        startIndex = idx,
                                    ),
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE8A33D),
                                    modifier = Modifier.width(32.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
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
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
