package one.only.player.feature.player.state

import android.net.Uri
import androidx.compose.runtime.Stable
import one.only.player.core.model.OnlineSubtitleResult
import one.only.player.core.model.OnlineSubtitleSearchPreferences
import one.only.player.core.model.OnlineSubtitleSearchResult
import one.only.player.core.ui.base.DataState

@Stable
data class OnlineSubtitleSearchUiState(
    val query: String = "",
    val preferences: OnlineSubtitleSearchPreferences = OnlineSubtitleSearchPreferences(),
    val outcome: DataState<OnlineSubtitleSearchResult>? = null,
    val downloadingKey: String? = null,
    // 已挂载到当前视频的搜索结果；取自字幕轨道元数据，跳出重进也能还原
    val addedKeys: Set<String> = emptySet(),
    val selectedKey: String? = null,
) {
    val results: List<OnlineSubtitleResult>
        get() = outcome?.result?.results.orEmpty()

    val isSearching: Boolean
        get() = outcome == DataState.Loading || outcome?.result?.isSearching == true

    val hasAddedResults: Boolean
        get() = addedKeys.isNotEmpty()

    fun statusOf(result: OnlineSubtitleResult): OnlineSubtitleResultStatus = when {
        downloadingKey == result.key -> OnlineSubtitleResultStatus.DOWNLOADING
        selectedKey == result.key -> OnlineSubtitleResultStatus.IN_USE
        result.key in addedKeys -> OnlineSubtitleResultStatus.ADDED
        else -> OnlineSubtitleResultStatus.AVAILABLE
    }
}

// 搜索结果行的当前状态，决定文案、选中标记和是否允许再次点击
enum class OnlineSubtitleResultStatus {
    AVAILABLE,
    DOWNLOADING,
    ADDED,
    IN_USE,
}

sealed interface OnlineSubtitleEvent {
    val mediaId: String

    data class Saved(val uri: Uri, override val mediaId: String) : OnlineSubtitleEvent
    data class Failed(val cause: Throwable, override val mediaId: String) : OnlineSubtitleEvent
}
