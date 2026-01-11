package com.knowledge.base.service

import com.ibm.icu.text.Transliterator
import com.knowledge.base.model.AccessRole
import com.knowledge.base.model.User
import com.knowledge.base.repository.AccessRoleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccessRoleMaintenanceService(
    private val accessRoleRepository: AccessRoleRepository
) {
    private val logger = LoggerFactory.getLogger(AccessRoleMaintenanceService::class.java)
    private val cyrToLat = Transliterator.getInstance("Cyrillic-Latin")

    fun normalizeAuthority(input: String): String {
        if (input.isBlank()) return ""
        val translit = cyrToLat.transliterate(input)
        val upper = translit.uppercase()
        return upper.map { ch ->
            when {
                ch in 'A'..'Z' -> ch
                ch in '0'..'9' -> ch
                ch == '_' || ch == '-' -> ch
                ch == ' ' -> '_'
                else -> '_'
            }
        }.joinToString("")
            .replace(Regex("_+"), "_")
            .trim('_')
    }

    fun ensureTranslit(ar: AccessRole): AccessRole {
        val code = ar.translitTitle?.trim().orEmpty()
        if (code.isNotBlank()) return ar
        val computed = normalizeAuthority(ar.title)
        return ar.copy(translitTitle = computed)
    }

    @Transactional
    fun ensureTranslitForUserRoles(user: User) {
        var changed = false
        val fixed = user.accessRoles.map { ar ->
            if (ar.translitTitle.isNullOrBlank()) {
                val computed = normalizeAuthority(ar.title)
                changed = true
                ar.copy(translitTitle = computed)
            } else ar
        }
        if (changed) {
            fixed.forEach { fr ->
                if (!fr.translitTitle.isNullOrBlank()) {
                    try {
                        accessRoleRepository.save(fr)
                    } catch (e: Exception) {
                        logger.warn(
                            "Failed to save fixed translitTitle for AccessRole id={}, title='{}': {}",
                            fr.id, fr.title, e.message
                        )
                    }
                }
            }
        }
    }

    // Массовая починка всей таблицы при старте приложения
    @Transactional
    fun fixAllMissingTranslits(): Int {
        val all = accessRoleRepository.findAll()
        var patched = 0
        all.forEach { ar ->
            if (ar.translitTitle.isNullOrBlank()) {
                val computed = normalizeAuthority(ar.title)
                val toSave = ar.copy(translitTitle = computed)
                try {
                    accessRoleRepository.save(toSave)
                    patched++
                } catch (e: Exception) {
                    logger.warn(
                        "Failed to set translitTitle for AccessRole id={}, title='{}': {}",
                        ar.id, ar.title, e.message
                    )
                }
            }
        }
        if (patched > 0) {
            logger.info("AccessRole translitTitle bootstrap fix: patched {} record(s).", patched)
        } else {
            logger.info("AccessRole translitTitle bootstrap fix: no records needed patching.")
        }
        return patched
    }
}

