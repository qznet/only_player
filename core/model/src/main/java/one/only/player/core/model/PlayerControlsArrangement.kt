package one.only.player.core.model

import kotlinx.serialization.Serializable

// 控件的四个去处，一个控件同时只属于其中一个
@Serializable
enum class PlayerControlSlot {
    TOP_RIGHT,
    BOTTOM_RIGHT,
    MENU,
    HIDDEN,
}

private val DefaultBottomRightControls = listOf(
    PlayerControl.ROTATE,
    PlayerControl.PLAYLIST,
    PlayerControl.PLAYBACK_SPEED,
)

private val DefaultMenuControls = listOf(
    PlayerControl.SUBTITLE,
    PlayerControl.AUDIO,
    PlayerControl.AUDIO_EQUALIZER,
    PlayerControl.CHAPTERS,
    PlayerControl.SCALE,
    PlayerControl.DECODER,
    PlayerControl.VIDEO_INFO,
    PlayerControl.VIDEO_FILTERS,
    PlayerControl.SLEEP_TIMER,
    PlayerControl.MARK,
    PlayerControl.LOCK,
    PlayerControl.MUTE,
    PlayerControl.AMBIENCE_MODE,
    PlayerControl.MIRROR_VIDEO,
    PlayerControl.PIP,
    PlayerControl.SCREENSHOT,
    PlayerControl.BACKGROUND_PLAY,
    PlayerControl.LOOP,
    PlayerControl.SHUFFLE,
)

// 可编排控件池；返回、上一项、播放、下一项和菜单键位置固定，不参与编排
val ArrangeablePlayerControls: List<PlayerControl> = DefaultBottomRightControls + DefaultMenuControls

// 列表顺序即渲染顺序，不在任何列表里的控件按池顺序补到菜单末尾
@Serializable
data class PlayerControlsArrangement(
    val topRight: List<PlayerControl> = emptyList(),
    val bottomRight: List<PlayerControl> = DefaultBottomRightControls,
    val menu: List<PlayerControl> = DefaultMenuControls,
    val hidden: Set<PlayerControl> = emptySet(),
)

// 未编排过的控件补到菜单，保证以后新增的控件不会凭空消失
private fun PlayerControlsArrangement.unarrangedControls(): List<PlayerControl> {
    val arrangedControls = topRight.toSet() + bottomRight + menu + hidden
    return ArrangeablePlayerControls.filterNot { it in arrangedControls }
}

fun PlayerControlsArrangement.controlsIn(slot: PlayerControlSlot): List<PlayerControl> = when (slot) {
    PlayerControlSlot.TOP_RIGHT -> topRight
    PlayerControlSlot.BOTTOM_RIGHT -> bottomRight
    PlayerControlSlot.MENU -> menu + unarrangedControls()
    PlayerControlSlot.HIDDEN -> ArrangeablePlayerControls.filter { it in hidden }
}

fun PlayerControlsArrangement.slotOf(control: PlayerControl): PlayerControlSlot = when {
    control in hidden -> PlayerControlSlot.HIDDEN
    control in topRight -> PlayerControlSlot.TOP_RIGHT
    control in bottomRight -> PlayerControlSlot.BOTTOM_RIGHT
    else -> PlayerControlSlot.MENU
}

fun PlayerControlsArrangement.withControlMoved(
    control: PlayerControl,
    slot: PlayerControlSlot,
): PlayerControlsArrangement {
    if (control !in ArrangeablePlayerControls) return this
    if (slotOf(control) == slot) return this

    val detached = copy(
        topRight = topRight - control,
        bottomRight = bottomRight - control,
        menu = menu - control,
        hidden = hidden - control,
    )
    return when (slot) {
        PlayerControlSlot.TOP_RIGHT -> detached.copy(topRight = detached.topRight + control)
        PlayerControlSlot.BOTTOM_RIGHT -> detached.copy(bottomRight = detached.bottomRight + control)
        PlayerControlSlot.MENU -> detached.copy(menu = detached.menu + control)
        PlayerControlSlot.HIDDEN -> detached.copy(hidden = detached.hidden + control)
    }
}

// offset 为 -1 上移、1 下移；未显示区不排序
fun PlayerControlsArrangement.withControlShifted(
    control: PlayerControl,
    offset: Int,
): PlayerControlsArrangement {
    val slot = slotOf(control)
    if (slot == PlayerControlSlot.HIDDEN) return this

    val orderedControls = controlsIn(slot)
    val fromIndex = orderedControls.indexOf(control)
    if (fromIndex < 0) return this
    val toIndex = fromIndex + offset
    if (toIndex !in orderedControls.indices) return this

    val reorderedControls = orderedControls.toMutableList()
    reorderedControls.removeAt(fromIndex)
    reorderedControls.add(toIndex, control)
    return when (slot) {
        PlayerControlSlot.TOP_RIGHT -> copy(topRight = reorderedControls)
        PlayerControlSlot.BOTTOM_RIGHT -> copy(bottomRight = reorderedControls)
        PlayerControlSlot.MENU -> copy(menu = reorderedControls)
        PlayerControlSlot.HIDDEN -> this
    }
}
