/*
 * Lyra Music Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * TV search: query field (system IME / on-screen keyboard) + song results
 * from the Innertube backend. D-pad navigable.
 */

package com.shnwaz.lyramusic.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shnwaz.lyramusic.LocalPlayerConnection
import com.shnwaz.lyramusic.innertube.YouTube
import com.shnwaz.lyramusic.innertube.models.SongItem
import com.shnwaz.lyramusic.models.toMediaMetadata
import com.shnwaz.lyramusic.playback.queues.YouTubeQueue
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSearchScreen() {
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    fun doSearch() {
        val q = query.trim()
        if (q.isBlank() || searching) return
        searching = true
        searched = true
        scope.launch {
            results =
                YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
                    .getOrNull()
                    ?.items
                    ?.filterIsInstance<SongItem>()
                    .orEmpty()
            searching = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search songs") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { doSearch() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
        )

        when {
            searching -> TvEmptyState("Searching…")
            searched && results.isEmpty() -> TvEmptyState("No results for \"$query\"")
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(results, key = { it.id }) { item ->
                        TvMediaCard(
                            title = item.title,
                            subtitle = item.artists.joinToString { it.name },
                            imageUrl = item.thumbnail,
                            onClick = {
                                playerConnection?.playQueue(
                                    YouTubeQueue.radio(item.toMediaMetadata()),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
