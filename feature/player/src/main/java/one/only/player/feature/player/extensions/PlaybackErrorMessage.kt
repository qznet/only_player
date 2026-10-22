package one.only.player.feature.player.extensions

import androidx.annotation.StringRes
import androidx.media3.common.PlaybackException
import one.only.player.core.ui.R as coreUiR

// 把 Media3 错误码映射成可本地化文案，未覆盖的码归入通用播放失败提示
@StringRes
fun PlaybackException.toMessageResId(): Int = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> coreUiR.string.playback_error_network_connection
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> coreUiR.string.playback_error_network_timeout
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> coreUiR.string.playback_error_bad_http_status
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> coreUiR.string.playback_error_file_not_found
    PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> coreUiR.string.playback_error_no_permission
    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> coreUiR.string.playback_error_cleartext_not_permitted
    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
    PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    -> coreUiR.string.playback_error_io

    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
    -> coreUiR.string.playback_error_parsing

    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
    -> coreUiR.string.playback_error_unsupported_format

    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
    -> coreUiR.string.playback_error_decoder

    PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
    PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
    -> coreUiR.string.playback_error_audio

    PlaybackException.ERROR_CODE_DRM_UNSPECIFIED,
    PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED,
    PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED,
    PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR,
    PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
    PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION,
    PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR,
    PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED,
    PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED,
    -> coreUiR.string.playback_error_drm

    PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> coreUiR.string.playback_error_live_window
    PlaybackException.ERROR_CODE_TIMEOUT -> coreUiR.string.playback_error_timeout
    else -> coreUiR.string.playback_error_unspecified
}
