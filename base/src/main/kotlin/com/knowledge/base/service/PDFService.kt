package com.knowledge.base.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.Color
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.knowledge.base.model.Article
import org.jsoup.Jsoup
import org.jsoup.nodes.Document as HtmlDoc
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

@Service
class PDFService(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(PDFService::class.java)

    @Value("\${file.upload-dir}")
    private lateinit var uploadDir: String

    @Value("\${app.backend.url:http://localhost:8080}")
    private lateinit var backendUrl: String

    private val imageBaseHttpPrefixes: List<String>
        get() {
            val normalizedBackend = if (backendUrl.endsWith("/")) backendUrl else "$backendUrl/"
            return listOf(
                "${normalizedBackend}images/",
                "http://localhost:8080/images/",
                "https://localhost:8080/images/",
                "http://backend:8080/images/",
                "https://backend:8080/images/",
                "http://10.15.23.244:8080/images/",
                "https://10.15.23.244:8080/images/",
                "http://pro-znania:8080/images/",
                "https://pro-znania:8080/images/"
            )
        }

    private data class Fonts(
        val regular: PdfFont,
        val bold: PdfFont,
        val italic: PdfFont?,
        val boldItalic: PdfFont?
    )

    private data class StyleInfo(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val fontSize: Float? = null,
        val color: Color? = null,
        val backgroundColor: Color? = null,
        val textAlign: TextAlignment? = null
    )

    private fun loadFontFromResources(path: String): PdfFont? {
        return try {
            val res = ClassPathResource(path)
            if (!res.exists()) {
                logger.warn("Font not found: $path")
                return null
            }
            PdfFontFactory.createFont(res.inputStream.readAllBytes(), PdfEncodings.IDENTITY_H)
        } catch (e: Exception) {
            logger.error("Error loading font $path: ${e.message}", e)
            null
        }
    }

    private fun loadFonts(): Fonts {
        val regular = loadFontFromResources("fonts/DejaVuSans.ttf") ?: error("fonts/DejaVuSans.ttf not found")
        val bold = loadFontFromResources("fonts/DejaVuSans-Bold.ttf") ?: regular
        val italic = loadFontFromResources("fonts/DejaVuSans-Oblique.ttf")
        val boldItalic = loadFontFromResources("fonts/DejaVuSans-BoldOblique.ttf")
        return Fonts(regular, bold, italic, boldItalic)
    }

    // ============ ПАРСИНГ СТИЛЕЙ ============
    private fun parseStyleAttribute(styleStr: String?): StyleInfo {
        if (styleStr.isNullOrBlank()) return StyleInfo()

        var bold = false
        var italic = false
        var underline = false
        var fontSize: Float? = null
        var color: Color? = null
        var backgroundColor: Color? = null
        var textAlign: TextAlignment? = null

        styleStr.split(";").forEach { rule ->
            val parts = rule.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()

                when {
                    key == "font-size" -> {
                        fontSize = value.replace("px", "").toFloatOrNull()
                    }
                    key == "color" -> {
                        color = parseColor(value)
                    }
                    key == "background-color" -> {
                        backgroundColor = parseColor(value)
                    }
                    key == "text-align" -> {
                        textAlign = when (value.lowercase()) {
                            "center" -> TextAlignment.CENTER
                            "right" -> TextAlignment.RIGHT
                            "justify" -> TextAlignment.JUSTIFIED
                            else -> TextAlignment.LEFT
                        }
                    }
                    key == "font-weight" && value.toIntOrNull() ?: 0 >= 700 -> {
                        bold = true
                    }
                    key == "font-style" && value.lowercase() == "italic" -> {
                        italic = true
                    }
                    key == "text-decoration" && value.lowercase().contains("underline") -> {
                        underline = true
                    }
                }
            }
        }

        return StyleInfo(bold, italic, underline, fontSize, color, backgroundColor, textAlign)
    }

    private fun parseColor(colorStr: String): Color? {
        return try {
            when {
                colorStr.startsWith("rgb(") -> {
                    val nums = colorStr.replace("rgb(", "").replace(")", "").split(",")
                    if (nums.size >= 3) {
                        DeviceRgb(nums[0].trim().toInt(), nums[1].trim().toInt(), nums[2].trim().toInt())
                    } else null
                }
                colorStr.startsWith("#") -> {
                    val hex = colorStr.removePrefix("#")
                    if (hex.length == 6) {
                        val r = hex.substring(0, 2).toInt(16)
                        val g = hex.substring(2, 4).toInt(16)
                        val b = hex.substring(4, 6).toInt(16)
                        DeviceRgb(r, g, b)
                    } else null
                }
                else -> null
            }
        } catch (e: Exception) {
            logger.warn("Error parsing color: $colorStr", e)
            null
        }
    }

    private fun styledText(
        text: String,
        fonts: Fonts,
        style: StyleInfo = StyleInfo()
    ): Text {
        val font = when {
            style.bold && style.italic && fonts.boldItalic != null -> fonts.boldItalic
            style.bold -> fonts.bold
            style.italic && fonts.italic != null -> fonts.italic
            else -> fonts.regular
        }!!

        val t = Text(text).setFont(font)

        if (style.italic && (font == fonts.regular || font == fonts.bold)) {
            t.setItalic()
        }
        if (style.underline) {
            t.setUnderline()
        }
        if (style.fontSize != null) {
            t.setFontSize(style.fontSize!!)
        }
        if (style.color != null) {
            t.setFontColor(style.color!!)
        }
        if (style.backgroundColor != null) {
            t.setBackgroundColor(style.backgroundColor!!)
        }

        return t
    }

    // ============ ОСНОВНОЙ МЕТОД ============
    fun generateArticlePdf(article: Article): ByteArray {
        val out = ByteArrayOutputStream()
        val writer = PdfWriter(out)
        val pdf = PdfDocument(writer)
        val doc = Document(pdf)
        val fonts = loadFonts()

        try {
            doc.add(
                Paragraph(article.title)
                    .setFont(fonts.bold).setFontSize(24f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f)
                    .setFontColor(ColorConstants.DARK_GRAY)
            )
            doc.add(
                Paragraph("Категория: ${article.category.description}")
                    .setFont(fonts.regular).setFontSize(12f)
                    .setMarginBottom(10f)
                    .setFontColor(ColorConstants.GRAY)
            )
            doc.add(
                Paragraph("Дата создания: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}")
                    .setFont(fonts.regular).setFontSize(10f)
                    .setMarginBottom(30f)
                    .setFontColor(ColorConstants.GRAY)
            )
            doc.add(
                Paragraph("─".repeat(80))
                    .setFont(fonts.regular)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f)
                    .setFontColor(ColorConstants.LIGHT_GRAY)
            )

            val desc = article.description
            if (desc != null) {
                if (desc.has("ops")) {
                    processDelta(doc, desc, fonts)
                } else {
                    val raw = desc.asText()
                    val html = unescapeHtmlKeepingTags(raw).trim()
                    if (looksLikeHtml(html)) {
                        processHtml(doc, html, fonts)
                    } else {
                        addText(doc, raw, fonts.regular)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error generating PDF: ${e.message}", e)
            e.printStackTrace()
            doc.add(
                Paragraph("Ошибка при обработке содержимого статьи: ${e.message}")
                    .setFont(fonts.regular)
                    .setFontSize(12f)
                    .setFontColor(ColorConstants.RED)
            )
        } finally {
            doc.close()
        }

        return out.toByteArray()
    }

    // ============ ОБРАБОТКА DELTA ============
    private fun processDelta(document: Document, descriptionNode: JsonNode, fonts: Fonts) {
        val ops = descriptionNode.get("ops") ?: return
        for (op in ops) {
            if (!op.has("insert")) continue
            val insert = op.get("insert")
            if (insert.isTextual) {
                val textRaw = insert.asText()
                val htmlCandidate = unescapeHtmlKeepingTags(textRaw).trim()
                if (looksLikeHtml(htmlCandidate)) {
                    processHtml(document, htmlCandidate, fonts)
                    continue
                }
                val p = Paragraph(textRaw.trim())
                    .setFont(fonts.regular).setFontSize(14f).setMarginBottom(12f)
                if (op.has("attributes")) {
                    val a = op.get("attributes")
                    if (a.has("bold") && a.get("bold").asBoolean()) p.setFont(fonts.bold)
                    if (a.has("italic") && a.get("italic").asBoolean()) p.setItalic()
                    if (a.has("header")) {
                        val fs = when (a.get("header").asInt()) {
                            1 -> 20f; 2 -> 18f; 3 -> 16f; else -> 14f
                        }
                        p.setFont(fonts.bold).setFontSize(fs)
                    }
                }
                document.add(p)
            } else if (insert.isObject && insert.has("image")) {
                addImageSmart(document, insert.get("image").asText(), null, null)
            }
        }
    }

    // ============ ОБРАБОТКА HTML ============
    private fun processHtml(document: Document, html: String, fonts: Fonts) {
        val doc: HtmlDoc = Jsoup.parse(html)
        for (element in doc.body().children()) {
            processElement(document, element, fonts)
        }
    }

    private fun processElement(pdfDocument: Document, element: Element, fonts: Fonts) {
        val tag = element.tagName().lowercase()

        when {
            tag == "figure" && element.hasClass("table") -> {
                val tableEl = element.selectFirst("table")
                if (tableEl != null) {
                    processFigureTable(pdfDocument, element, tableEl, fonts)
                }
            }
            tag == "table" -> {
                addHtmlTable(pdfDocument, element, fonts)
            }
            tag == "img" -> {
                addImageSmart(
                    pdfDocument,
                    element.attr("src"),
                    element.attr("width").toIntOrNull(),
                    element.attr("height").toIntOrNull()
                )
            }
            tag == "hr" || tag == "hr /" -> {
                pdfDocument.add(
                    Paragraph("─".repeat(40))
                        .setFont(fonts.regular)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(12f)
                        .setFontColor(ColorConstants.LIGHT_GRAY)
                )
            }
            tag == "p" || tag == "div" || tag == "span" -> {
                val containerStyle = parseStyleAttribute(element.attr("style"))
                val para = Paragraph().setFont(fonts.regular).setFontSize(14f).setMarginBottom(12f)

                // 🔥 ГЛАВНОЕ: применяем стили к параграфу
                if (containerStyle.textAlign != null) {
                    para.setTextAlignment(containerStyle.textAlign!!)
                }
                if (containerStyle.backgroundColor != null) {
                    para.setBackgroundColor(containerStyle.backgroundColor!!)
                    // 🔥 Добавляем padding чтобы фон был виден
                    para.setPadding(8f)
                }

                val built = processChildrenIntoParagraph(pdfDocument, para, element, fonts)
                if (!built.isEmpty) {
                    pdfDocument.add(built)
                }
            }
            tag == "br" -> {
                pdfDocument.add(Paragraph(""))
            }
            else -> {
                val text = element.text().trim()
                if (text.isNotEmpty()) {
                    addText(pdfDocument, text, fonts.regular)
                }
            }
        }
    }

    private fun processFigureTable(pdfDocument: Document, figureEl: Element, tableEl: Element, fonts: Fonts) {
        var leadPara = Paragraph().setFont(fonts.regular).setFontSize(14f).setMarginBottom(12f)
        var leadAdded = false

        for (n in figureEl.childNodes()) {
            if (n is Element && n.tagName().equals("table", true)) break
            when (n) {
                is TextNode -> {
                    val t = n.text().replace("\u00a0", " ").trim()
                    if (t.isNotEmpty()) {
                        leadPara.add(styledText(t, fonts)); leadAdded = true
                    }
                }
                is Element -> {
                    when (n.tagName().lowercase()) {
                        "strong", "b" -> {
                            val t = n.text().replace("\u00a0", " ").trim()
                            if (t.isNotEmpty()) {
                                val style = parseStyleAttribute(n.attr("style"))
                                leadPara.add(styledText(t, fonts, style.copy(bold = true))); leadAdded = true
                            }
                        }
                        "em", "i" -> {
                            val t = n.text().replace("\u00a0", " ").trim()
                            if (t.isNotEmpty()) {
                                val style = parseStyleAttribute(n.attr("style"))
                                leadPara.add(styledText(t, fonts, style.copy(italic = true))); leadAdded = true
                            }
                        }
                        "u" -> {
                            val t = n.text().replace("\u00a0", " ").trim()
                            if (t.isNotEmpty()) {
                                val style = parseStyleAttribute(n.attr("style"))
                                leadPara.add(styledText(t, fonts, style.copy(underline = true))); leadAdded = true
                            }
                        }
                        "span" -> {
                            val t = n.text().replace("\u00a0", " ").trim()
                            if (t.isNotEmpty()) {
                                val style = parseStyleAttribute(n.attr("style"))
                                leadPara.add(styledText(t, fonts, style)); leadAdded = true
                            }
                        }
                        "br" -> {
                            leadPara.add("\n"); leadAdded = true
                        }
                        "img" -> {
                            if (leadAdded && !leadPara.isEmpty) {
                                pdfDocument.add(leadPara)
                                leadPara = Paragraph().setFont(fonts.regular).setFontSize(14f).setMarginBottom(12f)
                                leadAdded = false
                            }
                            addImageSmart(
                                pdfDocument,
                                n.attr("src"),
                                n.attr("width").toIntOrNull(),
                                n.attr("height").toIntOrNull()
                            )
                        }
                    }
                }
            }
        }

        if (leadAdded && !leadPara.isEmpty) {
            pdfDocument.add(leadPara)
        }
        addHtmlTable(pdfDocument, tableEl, fonts)
    }

    private fun processChildrenIntoParagraph(
        pdfDocument: Document,
        para: Paragraph,
        parent: Element,
        fonts: Fonts
    ): Paragraph {
        var currentPara = para

        for (node in parent.childNodes()) {
            when (node) {
                is TextNode -> {
                    val text = node.text().replace("\u00a0", " ").trim()
                    if (text.isNotEmpty()) {
                        currentPara.add(styledText(text, fonts))
                    }
                }
                is Element -> {
                    val style = parseStyleAttribute(node.attr("style"))

                    when (node.tagName().lowercase()) {
                        "strong", "b" -> {
                            val t = node.text().replace("\u00a0", " ").trim()
                            if (t.isNotEmpty()) {
                                currentPara.add(styledText(t, fonts, style.copy(bold = true)))
                            }
                        }
                        "em", "i" -> {
                            val t = node.text().replace("\u00a0", " ").trim()
                            if (t.isNotEmpty()) {
                                currentPara.add(styledText(t, fonts, style.copy(italic = true)))
                            }
                        }
                        "u" -> {
                            val t = node.text().replace("\u00a0", " ").trim()
                            if (t.isNotEmpty()) {
                                currentPara.add(styledText(t, fonts, style.copy(underline = true)))
                            }
                        }
                        "span", "font" -> {
                            val t = node.text().replace("\u00a0", " ").trim()
                            if (t.isNotEmpty()) {
                                currentPara.add(styledText(t, fonts, style))
                            }
                        }
                        "br" -> {
                            currentPara.add("\n")
                        }
                        "img" -> {
                            if (!currentPara.isEmpty) {
                                pdfDocument.add(currentPara)
                            }
                            addImageSmart(
                                pdfDocument,
                                node.attr("src"),
                                node.attr("width").toIntOrNull(),
                                node.attr("height").toIntOrNull()
                            )
                            // 🔥 восстанавливаем параграф ПОСЛЕ изображения
                            currentPara = Paragraph().setFont(fonts.regular).setFontSize(14f).setMarginBottom(12f)
                        }
                        "table" -> {
                            if (!currentPara.isEmpty) {
                                pdfDocument.add(currentPara)
                            }
                            addHtmlTable(pdfDocument, node, fonts)
                            // 🔥 восстанавливаем параграф ПОСЛЕ таблицы
                            currentPara = Paragraph().setFont(fonts.regular).setFontSize(14f).setMarginBottom(12f)
                        }
                        else -> {
                            currentPara = processChildrenIntoParagraph(pdfDocument, currentPara, node, fonts)
                        }
                    }
                }
            }
        }

        return currentPara
    }

    // ============ ТАБЛИЦЫ ============
    private fun addHtmlTable(pdfDocument: Document, tableEl: Element, fonts: Fonts) {
        val firstRow = tableEl.selectFirst("thead > tr") ?: tableEl.selectFirst("tbody > tr") ?: tableEl.selectFirst("tr")

        // ИСПРАВЛЕНИЕ: Используем select() вместо filter
        val colCount = firstRow?.select("td, th")?.size ?: 0

        if (colCount == 0) return

        val table = Table(colCount)
            .setWidth(UnitValue.createPercentValue(100f))
            .setHorizontalAlignment(HorizontalAlignment.CENTER)
            .setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
            .setMarginTop(5f)
            .setMarginBottom(10f)

        val rows = tableEl.select("tr")
        for (row in rows) {
            // ИСПРАВЛЕНИЕ: Используем select() вместо filter с лямбдой
            val cells = row.select("td, th")

            for (cellEl in cells) {
                val isHeader = cellEl.tagName().equals("th", true)
                val text = cellEl.text().replace("\u00a0", " ").trim()
                val colspan = cellEl.attr("colspan").toIntOrNull() ?: 1
                val rowspan = cellEl.attr("rowspan").toIntOrNull() ?: 1

                // style
                val cellStyle = parseStyleAttribute(cellEl.attr("style"))

                val p = Paragraph(text)
                    .setFont(if (isHeader) fonts.bold else fonts.regular)
                    .setFontSize(12f)
                    .setMargin(0f)
                    .setMultipliedLeading(1.1f)

                val cell = Cell(rowspan, colspan)
                    .add(p)
                    .setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                    .setPadding(6f)

                if (isHeader) {
                    cell.setBackgroundColor(ColorConstants.LIGHT_GRAY)
                }

                if (cellStyle.backgroundColor != null) {
                    cell.setBackgroundColor(cellStyle.backgroundColor!!)
                }

                table.addCell(cell)
            }
        }

        pdfDocument.add(table)
    }

    // ============ ИЗОБРАЖЕНИЯ ============
    private fun addImageSmart(pdfDocument: Document, src: String, width: Int?, height: Int?) {
        if (src.isBlank()) {
            logger.warn("Empty image source")
            return
        }

        logger.info("Processing image: $src")

        try {
            val imageBytes = when {
                src.startsWith("data:image") -> {
                    logger.info("Loading base64 image")
                    val base64Data = src.substringAfter("base64,")
                    Base64.getDecoder().decode(base64Data)
                }
                // 🔥 НОВОЕ: обработка относительного пути /images/...
                src.startsWith("/images/") -> {
                    logger.info("Loading local image from relative path: $src")
                    val decodedPath = URLDecoder.decode(src.substringAfterLast("/"), StandardCharsets.UTF_8)
                    val filePath = Paths.get(uploadDir, "images", decodedPath)
                    logger.info("Resolved file path: $filePath")

                    if (Files.exists(filePath)) {
                        logger.info("File exists, reading...")
                        Files.readAllBytes(filePath)
                    } else {
                        logger.warn("File not found: $filePath")
                        return
                    }
                }
                isLocalPath(src) -> {
                    logger.info("Loading local image from: $src")
                    val decodedPath = URLDecoder.decode(src.substringAfterLast("/"), StandardCharsets.UTF_8)
                    val filePath = Paths.get(uploadDir, "images", decodedPath)
                    logger.info("Resolved file path: $filePath")

                    if (Files.exists(filePath)) {
                        logger.info("File exists, reading...")
                        Files.readAllBytes(filePath)
                    } else {
                        logger.warn("File not found: $filePath")
                        return
                    }
                }
                src.startsWith("http://") || src.startsWith("https://") -> {
                    logger.info("Loading remote image from: $src")
                    try {
                        URL(src).readBytes()
                    } catch (e: Exception) {
                        logger.error("Failed to load remote image: ${e.message}", e)
                        return
                    }
                }
                else -> {
                    logger.warn("Unknown image source format: $src")
                    return
                }
            }

            if (imageBytes.isNotEmpty()) {
                logger.info("Image loaded successfully, size: ${imageBytes.size} bytes")
                val img = Image(ImageDataFactory.create(imageBytes))
                    .setWidth(UnitValue.createPercentValue(80f))
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setMarginTop(8f)
                    .setMarginBottom(12f)

                pdfDocument.add(img)
                logger.info("Image added to PDF successfully")
            } else {
                logger.warn("Image data is empty")
            }
        } catch (e: Exception) {
            logger.error("Error processing image: $src - ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun isLocalPath(src: String): Boolean {
        return imageBaseHttpPrefixes.any { src.startsWith(it) }
    }

    // ============ УТИЛИТЫ ============
    private fun addText(document: Document, text: String, font: PdfFont) {
        document.add(
            Paragraph(text)
                .setFont(font)
                .setFontSize(14f)
                .setMarginBottom(12f)
        )
    }

    private fun unescapeHtmlKeepingTags(s: String): String {
        return s
            .replace("&nbsp;", "\u00a0")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
    }

    private fun looksLikeHtml(s: String): Boolean {
        val t = s.trim()
        return (t.contains("<") && t.contains(">"))
    }
}