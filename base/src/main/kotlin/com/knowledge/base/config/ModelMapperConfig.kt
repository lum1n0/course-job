package com.knowledge.base.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.modelmapper.ModelMapper
import org.modelmapper.convention.MatchingStrategies

@Configuration
class ModelMapperConfig {



    @Configuration
    class ModelMapperConfig {

        @Bean
        fun modelMapper(): ModelMapper {
            val modelMapper = ModelMapper()

            // Устанавливаем стратегию сопоставления - STRICT для более точного маппинга
            modelMapper.configuration.matchingStrategy = MatchingStrategies.STRICT

            // Включаем поддержку маппинга приватных полей
            modelMapper.configuration.isFieldMatchingEnabled = true
            modelMapper.configuration.fieldAccessLevel = org.modelmapper.config.Configuration.AccessLevel.PRIVATE

            // Пропускаем null значения при маппинге
            modelMapper.configuration.isSkipNullEnabled = true

            // Разрешаем неполное сопоставление полей
            modelMapper.configuration.isAmbiguityIgnored = true

            return modelMapper
        }
    }
//
//    @Bean
//    fun modelMapper(): ModelMapper{
//        val modelMapper = ModelMapper()
//        return modelMapper
//    }
}