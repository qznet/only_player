package one.only.player.feature.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import one.only.player.core.model.OnlineSubtitleLanguageFilter
import one.only.player.core.model.OnlineSubtitleProvider
import one.only.player.core.model.OnlineSubtitleProviderStatus
import one.only.player.core.model.OnlineSubtitleResult
import one.only.player.core.model.OnlineSubtitleSearchPreferences
import one.only.player.core.ui.R
import one.only.player.core.ui.base.DataState
import one.only.player.core.ui.components.AppDialog
import one.only.player.core.ui.components.ListSectionTitle
import one.only.player.core.ui.designsystem.AppIcons
import one.only.player.core.ui.extensions.label
import one.only.player.feature.player.state.OnlineSubtitleResultStatus
import one.only.player.feature.player.state.OnlineSubtitleSearchUiState
import one.only.player.feature.player.ui.panel.PanelActionButton
import one.only.player.feature.player.ui.panel.PanelOptionList
import one.only.player.feature.player.ui.panel.PanelOptionRow
import one.only.player.feature.player.ui.panel.rememberPlayerPanelTokens
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun OnlineSubtitleSearchContent(
    state: OnlineSubtitleSearchUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelectResult: (OnlineSubtitleResult) -> Unit,
    onCancelDownload: () -> Unit,
    onShowSubtitleTracks: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var detailsResult by remember { mutableStateOf<OnlineSubtitleResult?>(null) }
    PanelOptionList(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
            .padding(horizontal = 16.dp),
    ) {
        TextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_online_subtitle_query"),
            label = stringResource(R.string.online_subtitle_search_hint),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        )
        PanelActionButton(
            modifier = Modifier.testTag("btn_online_subtitle_search"),
            text = stringResource(R.string.online_subtitle_search_action),
            isProminent = true,
            isEnabled = state.query.isNotBlank() && !state.isSearching,
            onClick = onSearch,
        )
        OnlineSubtitleSearchStatus(state = state, onRetry = onSearch)
        if (state.downloadingKey != null) {
            PanelActionButton(
                modifier = Modifier.testTag("btn_online_subtitle_cancel_download"),
                text = stringResource(R.string.online_subtitle_cancel_download),
                onClick = onCancelDownload,
            )
        }
        if (state.hasAddedResults) {
            PanelActionButton(
                modifier = Modifier.testTag("btn_online_subtitle_back_to_tracks"),
                text = stringResource(R.string.online_subtitle_back_to_tracks),
                onClick = onShowSubtitleTracks,
            )
        }
        state.results.forEach { result ->
            val status = state.statusOf(result)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PanelOptionRow(
                    modifier = Modifier.weight(1f),
                    isSelected = status == OnlineSubtitleResultStatus.IN_USE,
                    text = result.title,
                    description = result.describe(status),
                    testTag = "item_online_subtitle_${result.key}",
                    // 已添加的条目不再接受点击，避免重复下载同一份字幕
                    isEnabled = state.downloadingKey == null && status == OnlineSubtitleResultStatus.AVAILABLE,
                    onClick = { onSelectResult(result) },
                )
                MiuixIconButton(
                    modifier = Modifier.testTag("btn_online_subtitle_details_${result.key}"),
                    onClick = { detailsResult = result },
                ) {
                    MiuixIcon(
                        imageVector = AppIcons.Info,
                        contentDescription = stringResource(R.string.online_subtitle_details),
                    )
                }
            }
        }
    }

    detailsResult?.let { result ->
        OnlineSubtitleDetailsDialog(
            result = result,
            onDismiss = { detailsResult = null },
        )
    }
}

// 结果行会截断长版本名，详情里给出完整名称和全部匹配依据
@Composable
private fun OnlineSubtitleDetailsDialog(
    result: OnlineSubtitleResult,
    onDismiss: () -> Unit,
) {
    AppDialog(
        modifier = Modifier.testTag("dialog_online_subtitle_details"),
        onDismissRequest = onDismiss,
        title = stringResource(R.string.online_subtitle_details),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MiuixText(
                    modifier = Modifier.testTag("text_online_subtitle_details_title"),
                    text = result.title,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                )
                MiuixText(
                    text = result.describe(OnlineSubtitleResultStatus.AVAILABLE),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
        },
        confirmButton = {
            MiuixTextButton(
                modifier = Modifier.testTag("btn_online_subtitle_details_close"),
                text = stringResource(R.string.done),
                colors = MiuixButtonDefaults.textButtonColorsPrimary(),
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun OnlineSubtitleSearchStatus(
    state: OnlineSubtitleSearchUiState,
    onRetry: () -> Unit,
) {
    val tokens = rememberPlayerPanelTokens()
    val result = state.outcome?.result
    if (state.isSearching) {
        Row(
            modifier = Modifier.testTag("status_online_subtitle_searching"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            MiuixText(
                text = stringResource(R.string.online_subtitle_searching),
                color = tokens.contentColor,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
    result?.providerStates?.forEach { (provider, status) ->
        val statusText = stringResource(
            when (status) {
                OnlineSubtitleProviderStatus.SEARCHING -> R.string.online_subtitle_searching
                OnlineSubtitleProviderStatus.SUCCEEDED -> R.string.online_subtitle_source_finished
                OnlineSubtitleProviderStatus.FAILED -> R.string.online_subtitle_source_failed
            },
        )
        MiuixText(
            modifier = Modifier.testTag("status_online_subtitle_source_${provider.name.lowercase()}"),
            text = stringResource(R.string.online_subtitle_source_status, provider.label(), statusText),
            color = tokens.secondaryContentColor,
            style = MiuixTheme.textStyles.body2,
        )
    }
    if (state.outcome is DataState.Error || result?.hasFailures == true) {
        val hasSuccessfulSource = result?.providerStates?.containsValue(OnlineSubtitleProviderStatus.SUCCEEDED) == true
        MiuixText(
            modifier = Modifier.testTag("status_online_subtitle_search_error"),
            text = stringResource(
                if (hasSuccessfulSource) R.string.online_subtitle_search_partial else R.string.online_subtitle_search_failed,
            ),
            color = tokens.contentColor,
            style = MiuixTheme.textStyles.body2,
        )
        PanelActionButton(
            modifier = Modifier.testTag("btn_online_subtitle_retry"),
            text = stringResource(R.string.retry),
            isEnabled = !state.isSearching,
            onClick = onRetry,
        )
    } else if (result != null && !result.isSearching && result.results.isEmpty()) {
        MiuixText(
            modifier = Modifier.testTag("status_online_subtitle_search_empty"),
            text = stringResource(R.string.online_subtitle_search_empty),
            color = tokens.secondaryContentColor,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

@Composable
internal fun OnlineSubtitleSearchSettingsContent(
    preferences: OnlineSubtitleSearchPreferences,
    onShowLanguageFilter: () -> Unit,
    onProviderToggle: (OnlineSubtitleProvider) -> Unit,
) {
    PanelOptionList(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ListSectionTitle(text = stringResource(R.string.online_subtitle_search_language))
        PanelOptionRow(
            text = preferences.languageFilter.label(),
            isSelected = false,
            testTag = "btn_online_subtitle_language",
            onClick = onShowLanguageFilter,
        )
        ListSectionTitle(text = stringResource(R.string.online_subtitle_search_source))
        OnlineSubtitleProvider.entries.forEach { provider ->
            PanelOptionRow(
                text = provider.label(),
                isSelected = provider in preferences.providers,
                testTag = "option_online_subtitle_source_${provider.name.lowercase()}",
                onClick = { onProviderToggle(provider) },
            )
        }
    }
}

@Composable
internal fun OnlineSubtitleLanguageContent(
    selected: OnlineSubtitleLanguageFilter,
    onSelect: (OnlineSubtitleLanguageFilter) -> Unit,
) {
    PanelOptionList(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .selectableGroup(),
    ) {
        OnlineSubtitleLanguageFilter.entries.forEach { filter ->
            PanelOptionRow(
                text = filter.label(),
                isSelected = filter == selected,
                testTag = "item_online_subtitle_language_${filter.name.lowercase()}",
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun OnlineSubtitleResult.describe(status: OnlineSubtitleResultStatus): String {
    if (status == OnlineSubtitleResultStatus.DOWNLOADING) return stringResource(R.string.online_subtitle_downloading)
    val parts = buildList {
        when (status) {
            OnlineSubtitleResultStatus.ADDED -> add(stringResource(R.string.online_subtitle_result_added))
            OnlineSubtitleResultStatus.IN_USE -> add(stringResource(R.string.online_subtitle_result_in_use))
            OnlineSubtitleResultStatus.AVAILABLE,
            OnlineSubtitleResultStatus.DOWNLOADING,
            -> Unit
        }
        val language = OnlineSubtitleLanguageFilter.entries.firstOrNull { it.languageCode == languageCode }
        add(language?.label() ?: languageName.ifEmpty { languageCode.ifEmpty { stringResource(R.string.unknown) } })
        if (format.isNotEmpty()) add(format.uppercase())
        add(provider.label())
        downloadCount?.let { count -> add(stringResource(R.string.online_subtitle_downloads, count)) }
    }
    return parts.joinToString(separator = " · ")
}
