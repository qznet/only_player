package one.only.player.core.data.remote.subtitle

import java.io.StringReader
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

// XML-RPC 响应只有 struct/array/标量三种形态，这里解析成通用的 Map/List
internal object XmlRpcParser {

    fun parseResponse(xml: String): Map<String, Any?> {
        val parser = newParser(xml)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name != TAG_VALUE) continue

            if (parser.nextTag() != XmlPullParser.START_TAG || parser.name != TAG_STRUCT) {
                throw SubtitleSearchFailedException("Invalid XML-RPC response")
            }
            return parseStruct(parser)
        }
        throw SubtitleSearchFailedException("Empty XML-RPC response")
    }

    // 进入时位于 <value>，标量读完即返回，struct/array 由各自函数消费到对应结束标签
    private fun parseValue(parser: XmlPullParser): Any? {
        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> return when (parser.name) {
                    TAG_STRUCT -> parseStruct(parser)
                    TAG_ARRAY -> parseArray(parser)
                    TAG_INT, TAG_INT_ALT -> readText(parser).trim().toIntOrNull()
                    TAG_DOUBLE -> readText(parser).trim().toDoubleOrNull()
                    TAG_BOOLEAN -> readText(parser).trim() == "1"
                    else -> readText(parser)
                }

                // 空 <value/>
                XmlPullParser.END_TAG, XmlPullParser.END_DOCUMENT -> return null
            }
        }
    }

    // 进入时位于 <struct>，返回时停在 </struct>
    private fun parseStruct(parser: XmlPullParser): Map<String, Any?> {
        val result = linkedMapOf<String, Any?>()
        var currentName = ""
        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    TAG_NAME -> currentName = readText(parser)
                    TAG_VALUE -> result[currentName] = parseValue(parser)
                }

                XmlPullParser.END_TAG -> if (parser.name == TAG_STRUCT) return result
                XmlPullParser.END_DOCUMENT -> return result
            }
        }
    }

    // 进入时位于 <array>，返回时停在 </array>
    private fun parseArray(parser: XmlPullParser): List<Any?> {
        val result = mutableListOf<Any?>()
        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> if (parser.name == TAG_VALUE) result += parseValue(parser)
                XmlPullParser.END_TAG -> if (parser.name == TAG_ARRAY) return result
                XmlPullParser.END_DOCUMENT -> return result
            }
        }
    }

    // 读取当前标签的文本，返回时停在结束标签上
    private fun readText(parser: XmlPullParser): String {
        var text = ""
        while (true) {
            when (parser.next()) {
                XmlPullParser.TEXT -> text += parser.text.orEmpty()
                XmlPullParser.END_TAG, XmlPullParser.END_DOCUMENT -> return text
            }
        }
    }

    private fun newParser(xml: String): XmlPullParser {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))
        return parser
    }

    private const val TAG_VALUE = "value"
    private const val TAG_STRUCT = "struct"
    private const val TAG_ARRAY = "array"
    private const val TAG_NAME = "name"
    private const val TAG_INT = "int"
    private const val TAG_INT_ALT = "i4"
    private const val TAG_DOUBLE = "double"
    private const val TAG_BOOLEAN = "boolean"
}
