package one.only.player.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import one.only.player.core.common.Logger
import one.only.player.core.data.remote.subtitle.OpenSubtitlesRestClient
import one.only.player.core.data.remote.subtitle.OpenSubtitlesXmlRpcClient
import one.only.player.core.data.remote.subtitle.SubtitleCatClient
import one.only.player.core.data.remote.subtitle.SubtitleSearchFailedException
import one.only.player.core.model.OnlineSubtitleMatchHint
import one.only.player.core.model.OnlineSubtitlePayload
import one.only.player.core.model.OnlineSubtitleProvider
import one.only.player.core.model.OnlineSubtitleProviderStatus
import one.only.player.core.model.OnlineSubtitleResult
import one.only.player.core.model.OnlineSubtitleSearchResult

@Singleton
class RemoteSubtitleSearchRepository @Inject constructor(
    private val openSubtitlesRestClient: OpenSubtitlesRestClient,
    private val openSubtitlesXmlRpcClient: OpenSubtitlesXmlRpcClient,
    private val subtitleCatClient: SubtitleCatClient,
) : SubtitleSearchRepository {

    // 每个来源独立更新，失败状态与已返回的字幕一起保留。
    override fun search(
        query: String,
        languageCode: String?,
        providers: Set<OnlineSubtitleProvider>,
        matchHint: OnlineSubtitleMatchHint,
    ): Flow<OnlineSubtitleSearchResult> = combine(
        providers.map { provider ->
            flow {
                emit(ProviderOutcome(provider, OnlineSubtitleProviderStatus.SEARCHING))
                emit(searchProvider(provider, query, languageCode))
            }
        },
    ) { outcomes ->
        OnlineSubtitleSearchResult(
            results = outcomes.flatMap { it.results }
                .distinctBy { result -> result.key }
                .sortedWith(matchHint.resultComparator())
                .take(MAX_RESULTS),
            providerStates = outcomes.associate { it.provider to it.status },
        )
    }

    override suspend fun fetchSubtitle(result: OnlineSubtitleResult): OnlineSubtitlePayload = when (result.provider) {
        OnlineSubtitleProvider.OPEN_SUBTITLES -> openSubtitlesRestClient.fetchSubtitle(result)
        OnlineSubtitleProvider.OPEN_SUBTITLES_XML_RPC -> openSubtitlesXmlRpcClient.fetchSubtitle(result)
        OnlineSubtitleProvider.SUBTITLE_CAT -> subtitleCatClient.fetchSubtitle(result)
    }

    private suspend fun searchProvider(
        provider: OnlineSubtitleProvider,
        query: String,
        languageCode: String?,
    ): ProviderOutcome = try {
        // 单个来源卡住不能拖死整次搜索，超时按失败处理
        val results = withTimeoutOrNull(PROVIDER_TIMEOUT_MILLIS) {
            when (provider) {
                OnlineSubtitleProvider.OPEN_SUBTITLES -> openSubtitlesRestClient.searchByQuery(
                    query = query,
                    languageCode = languageCode,
                )

                OnlineSubtitleProvider.OPEN_SUBTITLES_XML_RPC -> openSubtitlesXmlRpcClient.searchByQuery(
                    query = query,
                    languageCode = languageCode,
                )

                OnlineSubtitleProvider.SUBTITLE_CAT -> subtitleCatClient.searchByQuery(
                    query = query,
                    languageCode = languageCode,
                )
            }
        } ?: throw SubtitleSearchFailedException("Subtitle provider timed out: $provider")

        Logger.debug(TAG, "Subtitle search ok: provider=$provider, results=${results.size}")
        ProviderOutcome(provider, OnlineSubtitleProviderStatus.SUCCEEDED, results)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Logger.error(TAG, "Subtitle search failed: provider=$provider", exception)
        ProviderOutcome(provider, OnlineSubtitleProviderStatus.FAILED)
    }

    private data class ProviderOutcome(
        val provider: OnlineSubtitleProvider,
        val status: OnlineSubtitleProviderStatus,
        val results: List<OnlineSubtitleResult> = emptyList(),
    )

    private companion object {
        const val TAG = "RemoteSubtitleSearchRepository"
        const val MAX_RESULTS = 60
        const val PROVIDER_TIMEOUT_MILLIS = 30_000L
    }
}
