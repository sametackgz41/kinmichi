package app.tsuzuku.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Replays the first-run tour and welcome screen from Settings, exactly as a new user sees them,
 * without touching data or the onboarding flags.
 */
object IntroReplay {
    enum class Stage { Off, Tour, Welcome }

    private val mutableStage = MutableStateFlow(Stage.Off)
    val stage: StateFlow<Stage> = mutableStage.asStateFlow()

    fun start() { mutableStage.value = Stage.Tour }

    fun next() {
        mutableStage.value = when (mutableStage.value) {
            Stage.Tour -> Stage.Welcome
            else -> Stage.Off
        }
    }
}
