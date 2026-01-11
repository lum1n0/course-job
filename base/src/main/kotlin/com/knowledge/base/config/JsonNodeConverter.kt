    // JsonNodeConverters.kt
package com.knowledge.base.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.stereotype.Component

@Converter
@Component
class JsonNodeConverter : AttributeConverter<JsonNode, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: JsonNode?): String? {
        return attribute?.let { objectMapper.writeValueAsString(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): JsonNode? {
        return dbData?.let { objectMapper.readTree(it) }
    }
}

@Converter
@Component
class StringListConverter : AttributeConverter<List<String>, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<String>?): String? {
        return attribute?.let { objectMapper.writeValueAsString(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): List<String>? {
        return dbData?.let {
            objectMapper.readValue(
                it,
                objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)
            )
        }
    }
}