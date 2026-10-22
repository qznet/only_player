package one.only.player.feature.player.service.effects

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import one.only.player.core.model.DecoderPriority

// 杜比视界保留原生输出链路，扩展渲染器不接入 Media3 效果管线。
internal fun shouldApplyVideoEffects(
    decoderPriority: DecoderPriority,
    format: Format? = null,
): Boolean = decoderPriority != DecoderPriority.PREFER_APP && format?.isDolbyVisionVideoFormat() != true

internal fun Format.isDolbyVisionVideoFormat(): Boolean = sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION ||
    MimeTypes.getVideoMediaMimeType(codecs) == MimeTypes.VIDEO_DOLBY_VISION
