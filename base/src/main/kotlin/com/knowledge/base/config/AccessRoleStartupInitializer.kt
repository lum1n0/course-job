package com.knowledge.base.config

import com.knowledge.base.service.AccessRoleMaintenanceService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
class AccessRoleStartupInitializer(
    private val maintenanceService: AccessRoleMaintenanceService
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(AccessRoleStartupInitializer::class.java)

    override fun run(args: ApplicationArguments?) {
        try {
            val patched = maintenanceService.fixAllMissingTranslits()
            logger.info("AccessRoleStartupInitializer finished. Patched: {}", patched)
        } catch (e: Exception) {
            logger.warn("AccessRoleStartupInitializer failed: {}", e.message)
        }
    }
}