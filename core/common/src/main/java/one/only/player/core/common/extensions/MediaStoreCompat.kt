package one.only.player.core.common.extensions

import android.net.Uri
import android.os.Build
import android.provider.MediaStore

// Scoped storage 自 Android 10 (API 29 / Q) 起在对应设备上强制。
// Android 9 (API 28) 及以下不存在多卷 / 相对路径概念，必须使用传统单卷 URI，
// 否则引用 API 29+ 的常量或方法会在运行时抛 NoSuchFieldError / NoSuchMethodError。
val isScopedStorage: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

fun videoCollectionUri(): Uri = if (isScopedStorage) {
    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
} else {
    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
}

fun filesCollectionUri(): Uri = if (isScopedStorage) {
    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
} else {
    MediaStore.Files.getContentUri("external")
}

fun imagesCollectionUri(): Uri = if (isScopedStorage) {
    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
} else {
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
}

// 跨卷移动 / 回收时按卷名取集合 URI；低版本无多卷概念，统一落到 external 单卷
fun volumeCollectionUri(
    mimeType: String,
    volumeName: String?,
): Uri = if (isScopedStorage) {
    if (mimeType.startsWith("video/")) {
        MediaStore.Video.Media.getContentUri(volumeName)
    } else {
        MediaStore.Files.getContentUri(volumeName)
    }
} else {
    if (mimeType.startsWith("video/")) {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Files.getContentUri("external")
    }
}
