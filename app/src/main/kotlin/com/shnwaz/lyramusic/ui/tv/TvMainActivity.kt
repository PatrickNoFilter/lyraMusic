/*
 * Lyra Music Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Android TV entry point (PatrickNoFilter/lyraMusic fork, branch "tv").
 * Reuses the app's Hilt graph, Innertube engine, MusicService playback,
 * Room database and lyrics stack; only the UI layer is TV-specific.
 */

package com.shnwaz.lyramusic.ui.tv

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.lifecycle.lifecycleScope
import com.shnwaz.lyramusic.LocalDatabase
import com.shnwaz.lyramusic.LocalPlayerAwareWindowInsets
import com.shnwaz.lyramusic.LocalPlayerConnection
import com.shnwaz.lyramusic.db.MusicDatabase
import com.shnwaz.lyramusic.playback.MusicService
import com.shnwaz.lyramusic.playback.MusicService.MusicBinder
import com.shnwaz.lyramusic.playback.PlayerConnection
import com.shnwaz.lyramusic.utils.reportException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TvMainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var isMusicServiceBound = false

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                isMusicServiceBound = true
                if (service is MusicBinder) {
                    playerConnection =
                        PlayerConnection(this@TvMainActivity, service, database, lifecycleScope)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isMusicServiceBound = false
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(
                LocalPlayerConnection provides playerConnection,
                LocalPlayerAwareWindowInsets provides WindowInsets.systemBars,
                LocalDatabase provides database,
            ) {
                TvTheme {
                    TvNav()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isMusicServiceBound =
            bindService(
                Intent(this, MusicService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE,
            )
    }

    override fun onStop() {
        if (isMusicServiceBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                reportException(e)
            } finally {
                isMusicServiceBound = false
            }
        }
        super.onStop()
    }
}
