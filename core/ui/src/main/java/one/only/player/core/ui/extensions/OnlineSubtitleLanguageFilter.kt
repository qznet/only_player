package one.only.player.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import one.only.player.core.model.OnlineSubtitleLanguageFilter
import one.only.player.core.ui.R

@Composable
fun OnlineSubtitleLanguageFilter.label(): String = stringResource(
    when (this) {
        OnlineSubtitleLanguageFilter.AUTO -> R.string.online_subtitle_language_auto
        OnlineSubtitleLanguageFilter.ALL -> R.string.online_subtitle_language_all
        OnlineSubtitleLanguageFilter.CHINESE_SIMPLIFIED -> R.string.online_subtitle_language_chinese_simplified
        OnlineSubtitleLanguageFilter.CHINESE_TRADITIONAL -> R.string.online_subtitle_language_chinese_traditional
        OnlineSubtitleLanguageFilter.CHINESE_BILINGUAL -> R.string.online_subtitle_language_chinese_bilingual
        OnlineSubtitleLanguageFilter.ENGLISH -> R.string.online_subtitle_language_english
        OnlineSubtitleLanguageFilter.JAPANESE -> R.string.online_subtitle_language_japanese
        OnlineSubtitleLanguageFilter.KOREAN -> R.string.online_subtitle_language_korean
        OnlineSubtitleLanguageFilter.FRENCH -> R.string.online_subtitle_language_french
        OnlineSubtitleLanguageFilter.GERMAN -> R.string.online_subtitle_language_german
        OnlineSubtitleLanguageFilter.SPANISH -> R.string.online_subtitle_language_spanish
        OnlineSubtitleLanguageFilter.RUSSIAN -> R.string.online_subtitle_language_russian
        OnlineSubtitleLanguageFilter.PORTUGUESE -> R.string.online_subtitle_language_portuguese
        OnlineSubtitleLanguageFilter.ITALIAN -> R.string.online_subtitle_language_italian
        OnlineSubtitleLanguageFilter.ARABIC -> R.string.online_subtitle_language_arabic
        OnlineSubtitleLanguageFilter.THAI -> R.string.online_subtitle_language_thai
        OnlineSubtitleLanguageFilter.VIETNAMESE -> R.string.online_subtitle_language_vietnamese
        OnlineSubtitleLanguageFilter.INDONESIAN -> R.string.online_subtitle_language_indonesian
        OnlineSubtitleLanguageFilter.TURKISH -> R.string.online_subtitle_language_turkish
        OnlineSubtitleLanguageFilter.DUTCH -> R.string.online_subtitle_language_dutch
        OnlineSubtitleLanguageFilter.POLISH -> R.string.online_subtitle_language_polish
    },
)
