package one.only.player.core.model

import kotlinx.serialization.Serializable

// 在线字幕来源；同一份内容可能由不同来源给出，结果需要标注来源
@Serializable
enum class OnlineSubtitleProvider {
    OPEN_SUBTITLES,
    OPEN_SUBTITLES_XML_RPC,
    SUBTITLE_CAT,
}

enum class OnlineSubtitleProviderStatus {
    SEARCHING,
    SUCCEEDED,
    FAILED,
}

data class OnlineSubtitleSearchResult(
    val results: List<OnlineSubtitleResult> = emptyList(),
    val providerStates: Map<OnlineSubtitleProvider, OnlineSubtitleProviderStatus> = emptyMap(),
) {
    val isSearching: Boolean get() = OnlineSubtitleProviderStatus.SEARCHING in providerStates.values
    val hasFailures: Boolean get() = OnlineSubtitleProviderStatus.FAILED in providerStates.values
}

// providerId 由提供方解释（直链或字幕编号），不要跨来源复用
data class OnlineSubtitleResult(
    val provider: OnlineSubtitleProvider,
    val providerId: String,
    val languageCode: String,
    val languageName: String,
    val title: String,
    val format: String,
    val downloadCount: Int? = null,
) {
    val key: String get() = "${provider.name}:$providerId"
}

// 各来源的封装方式不同（gzip、base64 内联），统一在数据层解包成字节与真实扩展名
class OnlineSubtitlePayload(
    val bytes: ByteArray,
    val extension: String,
)

// 各来源语言码写法不一，归一化后再筛选与展示
object OnlineSubtitleLanguage {
    const val SIMPLIFIED_CHINESE = "zh-CN"
    const val TRADITIONAL_CHINESE = "zh-TW"
    const val BILINGUAL_CHINESE = "zh-BI"
    const val ENGLISH = "en"
    const val JAPANESE = "ja"
    const val KOREAN = "ko"
    const val FRENCH = "fr"
    const val GERMAN = "de"
    const val SPANISH = "es"
    const val RUSSIAN = "ru"
    const val PORTUGUESE = "pt"
    const val ITALIAN = "it"
    const val ARABIC = "ar"
    const val THAI = "th"
    const val VIETNAMESE = "vi"
    const val INDONESIAN = "id"
    const val TURKISH = "tr"
    const val DUTCH = "nl"
    const val POLISH = "pl"

    // 键为各来源可能给出的写法：ISO639-1、ISO639-2/B、OpenSubtitles 专用码
    private val aliases = mapOf(
        "zh" to SIMPLIFIED_CHINESE,
        "chs" to SIMPLIFIED_CHINESE,
        "chi" to SIMPLIFIED_CHINESE,
        "zho" to SIMPLIFIED_CHINESE,
        "zh-cn" to SIMPLIFIED_CHINESE,
        "zh-hans" to SIMPLIFIED_CHINESE,
        "cht" to TRADITIONAL_CHINESE,
        "zht" to TRADITIONAL_CHINESE,
        "zh-tw" to TRADITIONAL_CHINESE,
        "zh-hant" to TRADITIONAL_CHINESE,
        "zhe" to BILINGUAL_CHINESE,
        "zh-bi" to BILINGUAL_CHINESE,
        "en" to ENGLISH,
        "eng" to ENGLISH,
        "ja" to JAPANESE,
        "jpn" to JAPANESE,
        "ko" to KOREAN,
        "kor" to KOREAN,
        "fr" to FRENCH,
        "fre" to FRENCH,
        "fra" to FRENCH,
        "de" to GERMAN,
        "ger" to GERMAN,
        "deu" to GERMAN,
        "es" to SPANISH,
        "spa" to SPANISH,
        "ru" to RUSSIAN,
        "rus" to RUSSIAN,
        "pt" to PORTUGUESE,
        "por" to PORTUGUESE,
        "pob" to PORTUGUESE,
        "it" to ITALIAN,
        "ita" to ITALIAN,
        "ar" to ARABIC,
        "ara" to ARABIC,
        "th" to THAI,
        "tha" to THAI,
        "vi" to VIETNAMESE,
        "vie" to VIETNAMESE,
        "id" to INDONESIAN,
        "ind" to INDONESIAN,
        "tr" to TURKISH,
        "tur" to TURKISH,
        "nl" to DUTCH,
        "dut" to DUTCH,
        "nld" to DUTCH,
        "pl" to POLISH,
        "pol" to POLISH,
    )

    fun normalize(code: String): String {
        val normalized = code.trim().lowercase().replace('_', '-')
        aliases[normalized]?.let { return it }
        // 带地区后缀时按主语言归类，如 pt-BR、en-US
        return aliases[normalized.substringBefore('-')] ?: normalized
    }
}

// 界面可筛选的语言；provider 各自的语言码写法在数据层映射
// AUTO 不是具体语言，取值时解析为首选字幕语言
@Serializable
enum class OnlineSubtitleLanguageFilter(val languageCode: String?) {
    AUTO(null),
    ALL(null),
    CHINESE_SIMPLIFIED(OnlineSubtitleLanguage.SIMPLIFIED_CHINESE),
    CHINESE_TRADITIONAL(OnlineSubtitleLanguage.TRADITIONAL_CHINESE),
    CHINESE_BILINGUAL(OnlineSubtitleLanguage.BILINGUAL_CHINESE),
    ENGLISH(OnlineSubtitleLanguage.ENGLISH),
    JAPANESE(OnlineSubtitleLanguage.JAPANESE),
    KOREAN(OnlineSubtitleLanguage.KOREAN),
    FRENCH(OnlineSubtitleLanguage.FRENCH),
    GERMAN(OnlineSubtitleLanguage.GERMAN),
    SPANISH(OnlineSubtitleLanguage.SPANISH),
    RUSSIAN(OnlineSubtitleLanguage.RUSSIAN),
    PORTUGUESE(OnlineSubtitleLanguage.PORTUGUESE),
    ITALIAN(OnlineSubtitleLanguage.ITALIAN),
    ARABIC(OnlineSubtitleLanguage.ARABIC),
    THAI(OnlineSubtitleLanguage.THAI),
    VIETNAMESE(OnlineSubtitleLanguage.VIETNAMESE),
    INDONESIAN(OnlineSubtitleLanguage.INDONESIAN),
    TURKISH(OnlineSubtitleLanguage.TURKISH),
    DUTCH(OnlineSubtitleLanguage.DUTCH),
    POLISH(OnlineSubtitleLanguage.POLISH),
}

@Serializable
data class OnlineSubtitleSearchPreferences(
    val languageFilter: OnlineSubtitleLanguageFilter = OnlineSubtitleLanguageFilter.AUTO,
    val providers: Set<OnlineSubtitleProvider> = setOf(
        OnlineSubtitleProvider.OPEN_SUBTITLES,
        OnlineSubtitleProvider.SUBTITLE_CAT,
    ),
) {
    fun withProviderToggled(provider: OnlineSubtitleProvider): OnlineSubtitleSearchPreferences {
        if (providers == setOf(provider)) return this
        return copy(providers = if (provider in providers) providers - provider else providers + provider)
    }

    // 返回传给各来源的语言码；null 表示不限定语言
    fun resolveLanguageCode(preferredSubtitleLanguage: String): String? = when (languageFilter) {
        OnlineSubtitleLanguageFilter.AUTO ->
            preferredSubtitleLanguage
                .takeIf { it.isNotBlank() }
                ?.let(OnlineSubtitleLanguage::normalize)

        else -> languageFilter.languageCode
    }
}

// 排序依据，取自当前片源文件名；缺项只会让该维度失效，不影响其余维度
data class OnlineSubtitleMatchHint(
    val releaseTokens: Set<String> = emptySet(),
    val episodeTag: String = "",
    val year: String = "",
    val preferredLanguageCode: String = "",
) {

    // 语言、季集、年份依次优先于片源版本重合度，下载量只作末位参考
    fun resultComparator(): Comparator<OnlineSubtitleResult> = compareByDescending<OnlineSubtitleResult> { languageScoreOf(it) }
        .thenByDescending { episodeScoreOf(it) }
        .thenByDescending { yearScoreOf(it) }
        .thenByDescending { tokenOverlapOf(it) }
        .thenByDescending { it.downloadCount ?: 0 }

    private fun languageScoreOf(result: OnlineSubtitleResult): Int {
        if (preferredLanguageCode.isEmpty()) return 0
        val normalized = OnlineSubtitleLanguage.normalize(result.languageCode)
        if (normalized == preferredLanguageCode) return 2
        return if (normalized.substringBefore('-') == preferredLanguageCode.substringBefore('-')) 1 else 0
    }

    private fun episodeScoreOf(result: OnlineSubtitleResult): Int {
        if (episodeTag.isEmpty()) return 0
        return if (result.title.contains(episodeTag, ignoreCase = true)) 1 else 0
    }

    private fun yearScoreOf(result: OnlineSubtitleResult): Int {
        if (year.isEmpty()) return 0
        return if (result.title.contains(year)) 1 else 0
    }

    private fun tokenOverlapOf(result: OnlineSubtitleResult): Int {
        if (releaseTokens.isEmpty()) return 0
        return tokenize(result.title).count { it in releaseTokens }
    }

    companion object {
        private val TOKEN_SEPARATOR_REGEX = Regex("[^\\p{L}\\p{N}]+")
        private val YEAR_REGEX = Regex("(19|20)\\d{2}")
        private val EPISODE_REGEX = Regex("s\\d{1,2}e\\d{1,3}", RegexOption.IGNORE_CASE)

        fun from(
            releaseName: String,
            preferredSubtitleLanguage: String,
        ): OnlineSubtitleMatchHint = OnlineSubtitleMatchHint(
            releaseTokens = tokenize(releaseName),
            episodeTag = EPISODE_REGEX.find(releaseName)?.value?.lowercase().orEmpty(),
            year = YEAR_REGEX.find(releaseName)?.value.orEmpty(),
            preferredLanguageCode = preferredSubtitleLanguage
                .takeIf { it.isNotBlank() }
                ?.let(OnlineSubtitleLanguage::normalize)
                .orEmpty(),
        )

        // 单字符碎片没有区分度，只保留两字符以上的词
        private fun tokenize(text: String): Set<String> = text
            .lowercase()
            .split(TOKEN_SEPARATOR_REGEX)
            .filter { it.length >= 2 }
            .toSet()
    }
}
