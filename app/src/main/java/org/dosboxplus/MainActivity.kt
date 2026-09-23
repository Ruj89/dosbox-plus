package org.dosboxplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import org.dosboxplus.data.GameRepository
import org.dosboxplus.domain.GameEntry
import org.dosboxplus.domain.DefaultProfiles
import java.util.UUID
import org.dosboxplus.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val repo = GameRepository(this)
        setContent { DosboxTheme { DosboxPlusApp(repo) } }
    }
}

@Composable private fun DosboxPlusApp(repo: GameRepository) {
    var gameToRun by remember { mutableStateOf<GameEntry?>(null) }
    var editor by remember { mutableStateOf<EditorRequest?>(null) }
    var homeSection by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = gameToRun != null || editor != null) { gameToRun = null; editor = null }
    when {
        gameToRun != null -> EmulatorScreen(gameToRun!!, repo)
        editor != null -> {
            val request = editor!!
            val initial = remember(request) {
                if (request.game == null) repo.globalProfile() ?: DefaultProfiles.create()
                else request.game.profileId?.let(repo::profile)
                    ?: repo.globalProfile()?.copy(id = UUID.randomUUID().toString(), name = request.game.title)
                    ?: DefaultProfiles.create(request.game.title)
            }
             ProfileEditorScreen(initial, request.game?.title ?: tr("Per tutti i giochi"), onSave = { profile ->
                if (request.game == null) repo.saveGlobalProfile(profile) else repo.assignProfile(request.game, profile)
                editor = null
            }, onClose = { editor = null })
        }
        else -> LibraryScreen(repo, homeSection, { homeSection = it }, launch = { gameToRun = it }, editGlobal = { editor = EditorRequest(null) }, editGame = { editor = EditorRequest(it) })
    }
}

private data class EditorRequest(val game: GameEntry?)
