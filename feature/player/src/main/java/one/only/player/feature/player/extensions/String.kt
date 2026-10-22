package one.only.player.feature.player.extensions

// Media3 合并媒体源时会加数字前缀，字幕 uri 自身的冒号必须保留
private val PERIOD_PREFIX = Regex("""^\d+:""")

// 去掉 Media3 的 period 前缀；不带前缀时原样返回
internal fun String.withoutTrackPeriodPrefix(): String = replaceFirst(PERIOD_PREFIX, "")

// 把轨道 id 还原成外挂字幕 id；不属于已知外挂字幕时返回 null
internal fun String?.toExternalSubtitleId(externalSubtitleIds: Collection<String>): String? {
    val rawId = this ?: return null
    if (rawId in externalSubtitleIds) return rawId

    val withoutPrefix = rawId.withoutTrackPeriodPrefix()
    return withoutPrefix.takeIf { it in externalSubtitleIds }
}
