/*
 * TV search: query field (system IME / on-screen keyboard) + song, playlist,
 * album and artist results from the Innertube backend, each as a full-width
 * horizontal row. D-pad navigable.
 */

package com.shnwaz.lyramusic.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.shnwaz.lyramusic.LocalPlayerConnection
import com.shnwaz.lyramusic.innertube.YouTube
import com.shnwaz.lyramusic.innertube.models.AlbumItem
import com.shnwaz.lyramusic.innertube.models.ArtistItem
import com.shnwaz.lyramusic.innertube.models.PlaylistItem
import com.shnwaz.lyramusic.innertube.models.SongItem
import com.shnwaz.lyramusic.models.toMediaMetadata
import com.shnwaz.lyramusic.playback.queues.YouTubeAlbumRadio
import com.shnwaz.lyramusic.playback.queues.YouTubeQueue
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSearchScreen(onOpenPlayer: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var albums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var artists by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    fun doSearch() {
        val q = query.trim()
        if (q.isBlank() || searching) return
        searching = true
        searched = true
        scope.launch {
            val songsDeferred = async {
                YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
                    .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
            }
            val communityDeferred = async {
                YouTube.search(q, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
            }
            val featuredDeferred = async {
                YouTube.search(q, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
            }
            val albumsDeferred = async {
                YouTube.search(q, YouTube.SearchFilter.FILTER_ALBUM)
                    .getOrNull()?.items?.filterIsInstance<AlbumItem>().orEmpty()
            }
            val artistsDeferred = async {
                YouTube.search(q, YouTube.SearchFilter.FILTER_ARTIST)
                    .getOrNull()?.items?.filterIsInstance<ArtistItem>().orEmpty()
            }
            songs = songsDeferred.await()
            playlists = (featuredDeferred.await() + communityDeferred.await())
                .distinctBy { it.id }
            albums = albumsDeferred.await()
            artists = artistsDeferred.await()
            searching = false
        }
    }

    fun playSong(item: SongItem) {
        playerConnection?.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
        onOpenPlayer()
    }

    fun playPlaylist(item: PlaylistItem) {
        item.playEndpoint?.let { endpoint ->
            playerConnection?.playQueue(YouTubeQueue(endpoint))
            onOpenPlayer()
        }
    }

    fun playAlbum(item: AlbumItem) {
        playerConnection?.playQueue(YouTubeAlbumRadio(item.playlistId))
        onOpenPlayer()
    }

    fun playArtist(item: ArtistItem) {
        val endpoint = item.radioEndpoint ?: item.playEndpoint
        endpoint?.let {
            playerConnection?.playQueue(YouTubeQueue(it))
            onOpenPlayer()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search songs, playlists, albums, artists") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { doSearch() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
        )

        when {
            searching -> TvEmptyState("Searching…")
            searched && songs.isEmpty() && playlists.isEmpty() && albums.isEmpty() && artists.isEmpty() ->
                TvEmptyState("No results for \"$query\"")
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (songs.isNotEmpty()) {
                        item(key = "search_songs") {
                            TvRail(
                                title = "Songs",
                                items = songs,
                                key = { it.id },
                            ) { item ->
                                TvMediaCard(
                                    title = item.title,
                                    subtitle = item.artists.joinToString { it.name },
                                    imageUrl = item.thumbnail,
                                    onClick = { playSong(item) },
                                )
                            }
                        }
                    }
                    if (playlists.isNotEmpty()) {
                        item(key = "search_playlists") {
                            TvRail(
                                title = "Playlists",
                                items = playlists,
                                key = { it.id },
                            ) { item ->
                                TvMediaCard(
                                    title = item.title,
                                    subtitle = item.author?.name ?: item.songCountText,
                                    imageUrl = item.thumbnail,
                                    onClick = { playPlaylist(item) },
                                )
                            }
                        }
                    }
                    if (albums.isNotEmpty()) {
                        item(key = "search_albums") {
                            TvRail(
                                title = "Albums",
                                items = albums,
                                key = { it.id },
                            ) { item ->
                                TvMediaCard(
                                    title = item.title,
                                    subtitle = item.artists?.joinToString { it.name },
                                    imageUrl = item.thumbnail,
                                    onClick = { playAlbum(item) },
                                )
                            }
                        }
                    }
                    if (artists.isNotEmpty()) {
                        item(key = "search_artists") {
                            TvRail(
                                title = "Artists",
                                items = artists,
                                key = { it.id },
                            ) { item ->
                                TvMediaCard(
                                    title = item.title,
                                    subtitle = null,
                                    imageUrl = item.thumbnail,
                                    onClick = { playArtist(item) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
