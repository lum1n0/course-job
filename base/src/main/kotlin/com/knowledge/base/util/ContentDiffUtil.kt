package com.knowledge.base.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.MissingNode
import com.flipkart.zjsonpatch.JsonDiff
import com.knowledge.base.dto.FilesDiffDto
import com.knowledge.base.dto.TextDeltaDto
import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType

object ContentDiffUtil {

    fun jsonPatch(oldNode: JsonNode?, newNode: JsonNode?): JsonNode {
        val a = oldNode ?: MissingNode.getInstance()
        val b = newNode ?: MissingNode.getInstance()
        return JsonDiff.asJson(a, b)
    }

    fun textDeltas(oldNode: JsonNode?, newNode: JsonNode?, quillParser: QuillParserUtil): List<TextDeltaDto> {
        val oldText = quillParser.extractText(oldNode).trim()
        val newText = quillParser.extractText(newNode).trim()

        val oldTokens = if (oldText.isEmpty()) emptyList() else oldText.split(Regex("\\s+"))
        val newTokens = if (newText.isEmpty()) emptyList() else newText.split(Regex("\\s+"))
        val patch = DiffUtils.diff(oldTokens, newTokens)

        return patch.deltas.map { d ->
            val src = oldTokens.subList(d.source.position, d.source.position + d.source.size()).joinToString(" ")
            val tgt = newTokens.subList(d.target.position, d.target.position + d.target.size()).joinToString(" ")
            val type = when (d.type) {
                DeltaType.INSERT -> "INSERT"
                DeltaType.DELETE -> "DELETE"
                else -> "CHANGE"
            }
            TextDeltaDto(
                type = type,
                sourcePosition = d.source.position,
                sourceSize = d.source.size(),
                targetPosition = d.target.position,
                targetSize = d.target.size(),
                source = src,
                target = tgt
            )
        }
    }

    fun filesDiff(oldList: List<String>?, newList: List<String>?): FilesDiffDto {
        val a = oldList?.toSet() ?: emptySet()
        val b = newList?.toSet() ?: emptySet()
        val added = (b - a).toList().sorted()
        val removed = (a - b).toList().sorted()
        val unchanged = (a intersect b).toList().sorted()
        return FilesDiffDto(added = added, removed = removed, unchanged = unchanged)
    }
}
