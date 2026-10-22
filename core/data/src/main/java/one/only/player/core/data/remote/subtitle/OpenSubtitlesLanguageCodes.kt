package one.only.player.core.data.remote.subtitle

import one.only.player.core.model.OnlineSubtitleLanguage

// OpenSubtitles 系接口使用 ISO639-2/B 语言码，REST 与 XML-RPC 共用同一份映射
internal fun String.toOpenSubtitlesLanguageCode(): String = OPEN_SUBTITLES_LANGUAGE_CODES.getValue(this)

// 键为归一化后的语言码，值为接口使用的 ISO639-2/B 语言码
private val OPEN_SUBTITLES_LANGUAGE_CODES = mapOf(
    OnlineSubtitleLanguage.SIMPLIFIED_CHINESE to "chi",
    OnlineSubtitleLanguage.TRADITIONAL_CHINESE to "zht",
    OnlineSubtitleLanguage.BILINGUAL_CHINESE to "zhe",
    OnlineSubtitleLanguage.ENGLISH to "eng",
    OnlineSubtitleLanguage.JAPANESE to "jpn",
    OnlineSubtitleLanguage.KOREAN to "kor",
    OnlineSubtitleLanguage.FRENCH to "fre",
    OnlineSubtitleLanguage.GERMAN to "ger",
    OnlineSubtitleLanguage.SPANISH to "spa",
    OnlineSubtitleLanguage.RUSSIAN to "rus",
    OnlineSubtitleLanguage.PORTUGUESE to "por",
    OnlineSubtitleLanguage.ITALIAN to "ita",
    OnlineSubtitleLanguage.ARABIC to "ara",
    OnlineSubtitleLanguage.THAI to "tha",
    OnlineSubtitleLanguage.VIETNAMESE to "vie",
    OnlineSubtitleLanguage.INDONESIAN to "ind",
    OnlineSubtitleLanguage.TURKISH to "tur",
    OnlineSubtitleLanguage.DUTCH to "dut",
    OnlineSubtitleLanguage.POLISH to "pol",
)
