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
        val link: String? = null
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
                // Обновляем атрибуты для дочерних элементов
                val newAttrs = attrs.copy(
                    bold = attrs.bold || node.tagName() in listOf("b", "strong"),
                    italic = attrs.italic || node.tagName() in listOf("i", "em"),
                    underline = attrs.underline || node.tagName() == "u",
                    strike = attrs.strike || node.tagName() in listOf("s", "strike"),
                    link = if (node.tagName() == "a") node.attr("href") else attrs.link
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
                    // Блочные элементы: сначала контент, потом перенос строки
                    "p", "div", "h1", "h2", "h3", "li", "tr" -> {
                        node.childNodes().forEach { traverse(it, ops, newAttrs) }
                        ops.add(objectMapper.createObjectNode().put("insert", "\n"))
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

        if (!attrNode.isEmpty) {
            op.set<ObjectNode>("attributes", attrNode)
        }
        ops.add(op)
    }
}
