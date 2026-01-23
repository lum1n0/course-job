package com.knowledge.base.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode as JsoupTextNode
import org.springframework.stereotype.Component

@Component
class HtmlToDeltaConverter(private val objectMapper: ObjectMapper) {

    fun convert(html: String): ObjectNode {
        // Очищаем от лишних кавычек, если строка была дважды сериализована
        val cleanHtml = if (html.startsWith("\"") && html.endsWith("\"")) {
            html.removeSurrounding("\"").replace("\\\"", "\"")
        } else {
            html
        }

        val doc = Jsoup.parse(cleanHtml)
        val ops = objectMapper.createArrayNode()

        // Рекурсивно обходим body
        traverse(doc.body(), ops, Attributes())

        // Формируем итоговый JSON: { "ops": [...] }
        val result = objectMapper.createObjectNode()
        result.set<ArrayNode>("ops", ops)
        return result
    }

    private data class Attributes(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strike: Boolean = false,
        val link: String? = null,
        val color: String? = null,
        val background: String? = null,
        val size: String? = null,
        val font: String? = null
    )

    private data class BlockAttrs(
        val header: Int? = null,
        val align: String? = null,
        val indent: Int? = null,
        val list: String? = null,
        val blockquote: Boolean = false,
        val codeBlock: Boolean = false
    )

    private fun traverse(node: Node, ops: ArrayNode, attrs: Attributes) {
        when (node) {
            is JsoupTextNode -> {
                val text = node.text()
                if (text.isNotEmpty()) {
                    addTextOp(ops, text, attrs)
                }
            }
            is Element -> {
                val styleMap = parseStyleMap(node.attr("style"))
                val fontColor = node.attr("color").takeIf { it.isNotBlank() }
                val fontFace = node.attr("face").takeIf { it.isNotBlank() }
                val fontSize = mapHtmlFontSize(node.attr("size"))

                val textDecoration = styleMap["text-decoration"]?.lowercase() ?: ""
                val isUnderline = textDecoration.contains("underline")
                val isStrike = textDecoration.contains("line-through")
                val fontWeight = styleMap["font-weight"]?.lowercase()
                val fontStyle = styleMap["font-style"]?.lowercase()

                // Обновляем атрибуты для дочерних элементов
                val newAttrs = attrs.copy(
                    bold = attrs.bold || node.tagName() in listOf("b", "strong"),
                    italic = attrs.italic || node.tagName() in listOf("i", "em"),
                    underline = attrs.underline || node.tagName() == "u" || isUnderline,
                    strike = attrs.strike || node.tagName() in listOf("s", "strike", "del") || isStrike,
                    link = if (node.tagName() == "a") node.attr("href") else attrs.link,
                    color = styleMap["color"] ?: fontColor ?: attrs.color,
                    background = styleMap["background-color"] ?: styleMap["background"] ?: attrs.background,
                    size = styleMap["font-size"] ?: fontSize ?: attrs.size,
                    font = styleMap["font-family"] ?: fontFace ?: attrs.font
                )

                // Обработка специфичных тегов
                when (node.tagName()) {
                    "br" -> ops.add(objectMapper.createObjectNode().put("insert", "\n"))
                    "img" -> {
                        val src = node.attr("src")
                        if (src.isNotEmpty()) {
                            val imageOp = objectMapper.createObjectNode()
                            val imgData = objectMapper.createObjectNode().put("image", src)
                            imageOp.set<ObjectNode>("insert", imgData)
                            ops.add(imageOp)
                        }
                    }
                    // Блочные элементы: сначала контент, потом перенос строки с атрибутами блока
                    "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "tr", "blockquote", "pre" -> {
                        node.childNodes().forEach { traverse(it, ops, newAttrs) }
                        addNewlineOp(ops, buildBlockAttrs(node, styleMap))
                    }
                    // Таблицы: просто обходим содержимое
                    "table", "tbody", "td", "th" -> {
                        node.childNodes().forEach { traverse(it, ops, newAttrs) }
                        if (node.tagName() == "td" || node.tagName() == "th") {
                            addTextOp(ops, " ", newAttrs) // Пробел между ячейками
                        }
                    }
                    else -> node.childNodes().forEach { traverse(it, ops, newAttrs) }
                }
            }
        }
    }

    private fun addTextOp(ops: ArrayNode, text: String, attrs: Attributes) {
        val op = objectMapper.createObjectNode()
        op.put("insert", text)

        val attrNode = objectMapper.createObjectNode()
        if (attrs.bold) attrNode.put("bold", true)
        if (attrs.italic) attrNode.put("italic", true)
        if (attrs.underline) attrNode.put("underline", true)
        if (attrs.strike) attrNode.put("strike", true)
        if (attrs.link != null) attrNode.put("link", attrs.link)
        if (attrs.color != null) attrNode.put("color", attrs.color)
        if (attrs.background != null) attrNode.put("background", attrs.background)
        if (attrs.size != null) attrNode.put("size", attrs.size)
        if (attrs.font != null) attrNode.put("font", attrs.font)

        if (!attrNode.isEmpty) {
            op.set<ObjectNode>("attributes", attrNode)
        }
        ops.add(op)
    }

    private fun addNewlineOp(ops: ArrayNode, blockAttrs: BlockAttrs?) {
        val op = objectMapper.createObjectNode()
        op.put("insert", "\n")

        if (blockAttrs != null) {
            val attrNode = objectMapper.createObjectNode()
            if (blockAttrs.header != null) attrNode.put("header", blockAttrs.header)
            if (blockAttrs.align != null) attrNode.put("align", blockAttrs.align)
            if (blockAttrs.indent != null) attrNode.put("indent", blockAttrs.indent)
            if (blockAttrs.list != null) attrNode.put("list", blockAttrs.list)
            if (blockAttrs.blockquote) attrNode.put("blockquote", true)
            if (blockAttrs.codeBlock) attrNode.put("code-block", true)
            if (!attrNode.isEmpty) {
                op.set<ObjectNode>("attributes", attrNode)
            }
        }

        ops.add(op)
    }

    private fun buildBlockAttrs(node: Element, styleMap: Map<String, String>): BlockAttrs? {
        val tag = node.tagName()
        val header = when (tag) {
            "h1" -> 1
            "h2" -> 2
            "h3" -> 3
            "h4" -> 4
            "h5" -> 5
            "h6" -> 6
            else -> null
        }

        val align = (styleMap["text-align"] ?: node.attr("align"))
            .lowercase()
            .takeIf { it in setOf("left", "right", "center", "justify") }

        val indent = parseIndent(styleMap)

        val list = if (tag == "li") {
            when (node.parent()?.tagName()) {
                "ol" -> "ordered"
                "ul" -> "bullet"
                else -> null
            }
        } else {
            null
        }

        val blockquote = tag == "blockquote"
        val codeBlock = tag == "pre"

        if (header == null && align == null && indent == null && list == null && !blockquote && !codeBlock) {
            return null
        }

        return BlockAttrs(
            header = header,
            align = align,
            indent = indent,
            list = list,
            blockquote = blockquote,
            codeBlock = codeBlock
        )
    }

    private fun parseStyleMap(style: String?): Map<String, String> {
        if (style.isNullOrBlank()) return emptyMap()
        return style.split(";")
            .mapNotNull { part ->
                val pieces = part.split(":", limit = 2)
                if (pieces.size != 2) return@mapNotNull null
                val key = pieces[0].trim().lowercase()
                val value = pieces[1].trim()
                if (key.isBlank() || value.isBlank()) null else key to value
            }
            .toMap()
    }

    private fun parseIndent(styleMap: Map<String, String>): Int? {
        val raw = styleMap["margin-left"] ?: styleMap["padding-left"] ?: styleMap["text-indent"] ?: return null
        val px = parsePx(raw) ?: return null
        if (px <= 0) return null
        val level = (px / 30.0).toInt().coerceAtLeast(1)
        return level
    }

    private fun parsePx(value: String): Double? {
        val match = Regex("""(-?\d+(\.\d+)?)""").find(value) ?: return null
        return match.value.toDoubleOrNull()
    }

    private fun mapHtmlFontSize(size: String?): String? {
        if (size.isNullOrBlank()) return null
        val normalized = size.trim()
        val numeric = normalized.toIntOrNull()
        if (numeric == null) return normalized
        return when (numeric) {
            1 -> "10px"
            2 -> "12px"
            3 -> "14px"
            4 -> "16px"
            5 -> "18px"
            6 -> "24px"
            7 -> "32px"
            else -> "16px"
        }
    }
}
