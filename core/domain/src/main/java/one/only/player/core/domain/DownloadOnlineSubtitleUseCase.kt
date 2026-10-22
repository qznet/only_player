package one.only.player.core.domain

import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import one.only.player.core.common.Dispatcher
import one.only.player.core.common.DispatcherType
import one.only.player.core.data.repository.SubtitleSearchRepository
import one.only.player.core.model.OnlineSubtitlePayload
import one.only.player.core.model.OnlineSubtitleResult

class DownloadOnlineSubtitleUseCase @Inject constructor(
    private val subtitleSearchRepository: SubtitleSearchRepository,
    @Dispatcher(DispatcherType.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(result: OnlineSubtitleResult): OnlineSubtitlePayload = withContext(ioDispatcher) {
        subtitleSearchRepository.fetchSubtitle(result)
    }
}
