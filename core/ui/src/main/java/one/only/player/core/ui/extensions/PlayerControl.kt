package one.only.player.core.ui.extensions

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import one.only.player.core.model.PlayerControl
import one.only.player.core.ui.R
import one.only.player.core.ui.designsystem.AppIcons

// 控件的唯一小写标识，用于拼 testTag 和调试指令参数
val PlayerControl.id: String
    get() = name.lowercase()

// 图标与文案在此集中维护：播放器和设置页共用同一份，新增控件只改这里
@StringRes
fun PlayerControl.labelRes(): Int = when (this) {
    PlayerControl.BACK -> R.string.navigate_up
    PlayerControl.PLAYLIST -> R.string.now_playing
    PlayerControl.PLAYBACK_SPEED -> R.string.select_playback_speed
    PlayerControl.AUDIO -> R.string.select_audio_track
    PlayerControl.AUDIO_EQUALIZER -> R.string.audio_equalizer
    PlayerControl.SUBTITLE -> R.string.select_subtitle_track
    PlayerControl.PREVIOUS -> R.string.player_controls_previous
    PlayerControl.PLAY_PAUSE -> R.string.player_controls_play_pause
    PlayerControl.NEXT -> R.string.player_controls_next
    PlayerControl.LOCK -> R.string.controls_lock_switch
    PlayerControl.MUTE -> R.string.mute_switch
    PlayerControl.MARK -> R.string.playback_marks
    PlayerControl.CHAPTERS -> R.string.chapters
    PlayerControl.SCALE -> R.string.video_zoom
    PlayerControl.DECODER -> R.string.decoder_priority
    PlayerControl.AMBIENCE_MODE -> R.string.ambience_mode
    PlayerControl.VIDEO_FILTERS -> R.string.video_filters
    PlayerControl.PIP -> R.string.pip_settings
    PlayerControl.SCREENSHOT -> R.string.take_screenshot
    PlayerControl.BACKGROUND_PLAY -> R.string.background_play
    PlayerControl.LOOP -> R.string.loop_mode
    PlayerControl.SHUFFLE -> R.string.shuffle
    PlayerControl.SLEEP_TIMER -> R.string.sleep_timer
    PlayerControl.ROTATE -> R.string.screen_rotation
    PlayerControl.MIRROR_VIDEO -> R.string.mirror_video
    PlayerControl.VIDEO_INFO -> R.string.video_info
}

@Composable
fun PlayerControl.label(): String = stringResource(labelRes())

fun PlayerControl.icon(): ImageVector = when (this) {
    PlayerControl.BACK -> AppIcons.ArrowBack
    PlayerControl.PLAYLIST -> AppIcons.PlaylistPlay
    PlayerControl.PLAYBACK_SPEED -> AppIcons.Speed
    PlayerControl.AUDIO -> AppIcons.Audio
    PlayerControl.AUDIO_EQUALIZER -> AppIcons.Equalizer
    PlayerControl.SUBTITLE -> AppIcons.Subtitle
    PlayerControl.PREVIOUS -> AppIcons.SkipPrevious
    PlayerControl.PLAY_PAUSE -> AppIcons.Play
    PlayerControl.NEXT -> AppIcons.SkipNext
    PlayerControl.LOCK -> AppIcons.Lock
    PlayerControl.MUTE -> AppIcons.VolumeUp
    PlayerControl.MARK -> AppIcons.History
    PlayerControl.CHAPTERS -> AppIcons.PlaylistPlay
    PlayerControl.SCALE -> AppIcons.Frame
    PlayerControl.DECODER -> AppIcons.Decoder
    PlayerControl.AMBIENCE_MODE -> AppIcons.Ambience
    PlayerControl.VIDEO_FILTERS -> AppIcons.Sensitivity
    PlayerControl.PIP -> AppIcons.Pip
    PlayerControl.SCREENSHOT -> AppIcons.Screenshot
    PlayerControl.BACKGROUND_PLAY -> AppIcons.Headset
    PlayerControl.LOOP -> AppIcons.Loop
    PlayerControl.SHUFFLE -> AppIcons.Shuffle
    PlayerControl.SLEEP_TIMER -> AppIcons.Timer
    PlayerControl.ROTATE -> AppIcons.Rotation
    PlayerControl.MIRROR_VIDEO -> AppIcons.Size
    PlayerControl.VIDEO_INFO -> AppIcons.Info
}

data class PlayerCornerControlsCapacity(
    val topRight: Int,
    val bottomRight: Int,
)

// 角落控件可无限编排，容量只决定各方向一次最多显示多少个。
fun playerCornerControlsCapacity(isPortrait: Boolean): PlayerCornerControlsCapacity = if (isPortrait) {
    PlayerCornerControlsCapacity(
        topRight = 5,
        bottomRight = 3,
    )
} else {
    PlayerCornerControlsCapacity(
        topRight = 9,
        bottomRight = 6,
    )
}
