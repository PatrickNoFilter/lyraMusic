/*
 * Lyra Music Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * TV home: welcome header, mood chips and browsable rails (quick picks,
 * new releases, charts), mirroring the Lyra phone app's look. Reuses the
 * existing Hilt view models and the Innertube backend.
 */

package com.shnwaz.lyramusic.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shnwaz.lyramusic.LocalPlayerConnection
import com.shnwaz.lyramusic.innertube.YouTube
import com.shnwaz.lyramusic.innertube.models.AlbumItem
import com.shnwaz.lyramusic.innertube.models.SongItem
import com.shnwaz.lyramusic.models.toMediaMetadata
import com.shnwaz.lyramusic.playback.queues.YouTubeAlbumRadio
import com.shnwaz.lyramusic.playback.queues.YouTubeQueue
import com.shnwaz.lyramusic.viewmodels.ChartsViewModel
import com.shnwaz.lyramusic.viewmodels.HomeViewModel
import com.shnwaz.lyramusic.viewmodels.NewReleaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val TvMoods = listOf(
    "Feel good", "Relax", "Energize", "Romantic",
    "Focus", "Party", "Sleep", "Workout",
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHomeScreen(onOpenPlayer: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current
    val homeViewModel: HomeViewModel = hiltViewModel()
    val newReleaseViewModel: NewReleaseViewModel = hiltViewModel()
    val chartsViewModel: ChartsViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()

    val quickPicks by homeViewModel.quickPicks.collectAsStateWithLifecycle()
    val newReleases by newReleaseViewModel.newReleaseAlbums.collectAsStateWithLifecycle()
    val chartsPage by chartsViewModel.chartsPage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { chartsViewModel.loadCharts() }

    fun playSongItem(item: SongItem) {
        playerConnection?.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
        onOpenPlayer()
    }

    /** Tapping a mood chip searches that mood and plays the top song. */
    fun playMood(mood: String) {
        scope.launch(Dispatchers.IO) {
            YouTube.search("$mood music", YouTube.SearchFilter.FILTER_SONG)
                .onSuccess { page ->
                    val first = page.items.filterIsInstance<SongItem>().firstOrNull()
                    if (first != null) {
                        withContext(Dispatchers.Main) { playSongItem(first) }
                    }
                }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Welcome header, like the phone app
        item(key = "welcome") {
            Text(
                text = "Welcome to Lyra Music",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.padding(start = 48.dp, top = 8.dp, bottom = 16.dp),
            )
        }

        // Mood chips
        item(key = "moods") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(TvMoods) { mood ->
                    TvMoodChip(label = mood, onClick = { playMood(mood) })
                }
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
                                    else -> Unit
                                }
                            },
                        )
                    }
                }
            }
        }

        if (picks.isNullOrEmpty() && newReleases.isEmpty() && chartsPage == null) {
            item(key = "loading") {
                TvEmptyState("Loading…")
            }
        }
    }
}
