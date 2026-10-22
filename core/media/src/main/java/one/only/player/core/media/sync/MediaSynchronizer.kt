package one.only.player.core.media.sync

import one.only.player.core.model.StoragePath

interface MediaSynchronizer {
    suspend fun refresh(path: String? = null): Boolean

    suspend fun refreshMovedPaths(paths: List<StoragePath>)

    suspend fun removeDeleted(uris: List<String>)
    suspend fun registerManualVideoPath(path: String)
    fun startSync()
    fun stopSync()
}
