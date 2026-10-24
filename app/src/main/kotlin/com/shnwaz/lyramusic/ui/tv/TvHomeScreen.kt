/*
 * Lyra Music Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * TV home: browsable rails (quick picks, new releases, charts) reusing the
 * existing Hilt view models and the Innertube backend.
 */

package com.shnwaz.lyramusic.ui.tv

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shnwaz.lyramusic.LocalPlayerConnection
import com.shnwaz.lyramusic.innertube.models.AlbumItem
import com.shnwaz.lyramusic.innertube.models.PlaylistItem
import com.shnwaz.lyramusic.innertube.models.SongItem
import com.shnwaz.lyramusic.models.toMediaMetadata
import com.shnwaz.lyramusic.playback.queues.YouTubeAlbumRadio
import com.shnwaz.lyramusic.playback.queues.YouTubeQueue
import com.shnwaz.lyramusic.viewmodels.ChartsViewModel
import com.shnwaz.lyramusic.viewmodels.HomeViewModel
import com.shnwaz.lyramusic.viewmodels.NewReleaseViewModel

@Composable
fun TvHomeScreen(onOpenPlayer: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current
    val homeViewModel: HomeViewModel = hiltViewModel()
    val newReleaseViewModel: NewReleaseViewModel = hiltViewModel()
    val chartsViewModel: ChartsViewModel = hiltViewModel()

    val quickPicks by homeViewModel.quickPicks.collectAsStateWithLifecycle()
    val newReleases by newReleaseViewModel.newReleaseAlbums.collectAsStateWithLifecycle()
    val chartsPage by chartsViewModel.chartsPage.collectAsStateWithLifecycle()
    val homePage by homeViewModel.homePage.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) { chartsViewModel.loadCharts() }

    fun playSongItem(item: SongItem) {
        playerConnection?.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
        onOpenPlayer()
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Refresh row: reloads home feed + charts; status shows whether
        // each feed loaded, so a missing row can be diagnosed.
        item(key = "refresh_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
            ) {
                Button(
                    onClick = {
                        homeViewModel.refresh()
                        chartsViewModel.loadCharts()
                    },
                ) {
                    Text("Refresh home")
                }
                Text(
                    text = "  home: " + if (homePage == null) "loading…" else "${homePage?.sections?.size ?: 0} sections" +
                        "  •  charts: " + if (chartsPage == null) "loading…" else "${chartsPage?.sections?.size ?: 0} sections",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                )
            }
        }

        val picks = quickPicks
        if (!picks.isNullOrEmpty()) {
            item(key = "quick_picks") {
                TvRail(
                    title = "Quick picks",
                    items = picks,
                    key = { it.id },
                ) { song ->
                    TvMediaCard(
                        title = song.title,
                        subtitle = song.artists.joinToString { it.name },
                        imageUrl = song.thumbnailUrl,
                        onClick = {
                            playerConnection?.playQueue(
                                YouTubeQueue.radio(song.toMediaMetadata()),
                            )
                            onOpenPlayer()
                        },
                    )
                }
            }
        }

        if (newReleases.isNotEmpty()) {
            item(key = "new_releases") {
                TvRail(
                    title = "New releases",
                    items = newReleases,
                    key = { it.id },
                ) { album: AlbumItem ->
                    TvMediaCard(
                        title = album.title,
                        subtitle = album.artists?.joinToString { it.name },
                        imageUrl = album.thumbnail,
                        onClick = {
                            playerConnection?.playQueue(YouTubeAlbumRadio(album.playlistId))
                            onOpenPlayer()
                        },
                    )
                }
            }
        }

        chartsPage?.sections?.forEach { section ->
            if (section.items.isNotEmpty()) {
                item(key = "charts_${section.title}") {
                    TvRail(
                        title = section.title,
                        items = section.items,
                        key = { it.id },
                    ) { item ->
                        TvMediaCard(
                            title = item.title,
                            subtitle = (item as? SongItem)?.artists?.joinToString { it.name },
                            imageUrl = item.thumbnail,
                            onClick = {
                                when (item) {
                                    is SongItem -> playSongItem(item)
                                    is AlbumItem -> {
                                        playerConnection?.playQueue(
                                            YouTubeAlbumRadio(item.playlistId),
                                        )
                                        onOpenPlayer()
                                    }
                                    is PlaylistItem -> {
                                        item.playEndpoint?.let { endpoint ->
                                            playerConnection?.playQueue(
                                                YouTubeQueue(endpoint),
                                            )
                                            onOpenPlayer()
                                        }
                                    }
                                    else -> Unit
                                }
                            },
                        )
                    }
                }
            }
        }

        homePage?.sections?.forEach { section ->
            if (section.items.isNotEmpty()) {
                item(key = "home_${section.title}") {
                    TvRail(
                        title = section.title,
                        items = section.items,
                        key = { it.id },
                    ) { item ->
                        TvMediaCard(
                            title = item.title,
                            subtitle = (item as? SongItem)?.artists?.joinToString { it.name },
                            imageUrl = item.thumbnail,
                            onClick = {
                                when (item) {
                                    is SongItem -> playSongItem(item)
                                    is AlbumItem -> {
                                        playerConnection?.playQueue(
                                            YouTubeAlbumRadio(item.playlistId),
                                        )
                                        onOpenPlayer()
                                    }
                                    is PlaylistItem -> {
                                        item.playEndpoint?.let { endpoint ->
                                            playerConnection?.playQueue(
                                                YouTubeQueue(endpoint),
                                            )
                                            onOpenPlayer()
                                        }
                                    }
                                    else -> Unit
                                }
                            },
                        )
                    }
                }
            }
        }

        if (picks.isNullOrEmpty() && newReleases.isEmpty() && chartsPage == null && homePage == null) {
            item(key = "loading") {
                TvEmptyState("Loading…")
            }
        }
    }
}
