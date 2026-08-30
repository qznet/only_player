package one.only.player.core.media.extensions

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.MediaStore
import java.io.File
import one.only.player.core.common.extensions.canonicalPathOrSelf
import one.only.player.core.model.StoragePath

// 已挂载的外部存储卷。卷根路径在此解析一次并定型，之后一路按 StoragePath 比较
data class MediaStorageVolume(
    val label: String,
    val rootPath: StoragePath,
    val mediaStoreVolumeName: String?,
    val isPrimary: Boolean,
)

fun Context.mediaStorageVolumes(): List<MediaStorageVolume> = getSystemService(StorageManager::class.java)
    .storageVolumes
    .mapNotNull { volume ->
        // StorageVolume.getDirectory() 与 getMediaStoreVolumeName() 均仅 API 30+；
        // 低版本分别用已弃用但可用的 getPath()，以及传统单卷名 "external"
        val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            volume.directory
        } else {
            val path = runCatching {
                StorageVolume::class.java.getMethod("getPath").invoke(volume) as? String
            }.getOrNull()
            path?.let(::File)
        } ?: return@mapNotNull null

        val volumeName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            volume.mediaStoreVolumeName ?: "external_primary".takeIf { volume.isPrimary }
        } else {
            "external".takeIf { volume.isPrimary }
        }

        MediaStorageVolume(
            label = volume.getDescription(this),
            rootPath = StoragePath.of(directory.path.canonicalPathOrSelf()),
            // 个别 ROM 对主存储不返回卷名，而 MediaStore 写入必须有它
            mediaStoreVolumeName = volumeName,
            isPrimary = volume.isPrimary,
        )
    }

fun Context.storageRootLabels(): Map<StoragePath, String> = mediaStorageVolumes()
    .associate { volume -> volume.rootPath to volume.label }

fun Map<StoragePath, String>.storageRootLabelOf(path: String): String? = this[StoragePath.of(path)]

fun Map<StoragePath, String>.isStorageRoot(path: String): Boolean = storageRootLabelOf(path) != null
