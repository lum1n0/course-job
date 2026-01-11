// base/src/main/kotlin/com/knowledge/base/config/DataInitializer.kt
package com.knowledge.base.config

import com.knowledge.base.model.AccessRole
import com.knowledge.base.model.Role
import com.knowledge.base.model.User
import com.knowledge.base.repository.AccessRoleRepository
import com.knowledge.base.repository.RoleRepository
import com.knowledge.base.repository.UserRepository
import com.knowledge.base.service.ArticleVersionService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener

@Component
class DataInitializer(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val accessRoleRepository: AccessRoleRepository,
    private val articleVersionService: ArticleVersionService
): CommandLineRunner {

    private val log = LoggerFactory.getLogger(DataInitializer::class.java)

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun onReady() {
        log.info("Ensuring initial snapshots for all articles...")
        articleVersionService.ensureInitialSnapshotForAll()
        log.info("Initial snapshots check completed.")
    }

    override fun run(vararg args: String) {
        if (roleRepository.findByTitle("ADMIN") == null) {
            roleRepository.save(Role(title = "ADMIN"))
        }
        if (roleRepository.findByTitle("USER") == null) {
            roleRepository.save(Role(title = "USER"))
        }
        if (roleRepository.findByTitle("WRITER") == null) {
            roleRepository.save(Role(title = "WRITER"))
        }
        if (roleRepository.findByTitle("SUPER-WRITER") == null) {
            roleRepository.save(Role(title = "SUPER-WRITER"))
        }
        if (roleRepository.findByTitle("MODERATOR") == null) {
            roleRepository.save(Role(title = "MODERATOR"))
        }
        if (roleRepository.findByTitle("ROOT") == null) {
            roleRepository.save(Role(title = "ROOT"))
        }

        if (accessRoleRepository.findByTitle("FULL_ACCESS") == null) {
            accessRoleRepository.save(AccessRole(title = "FULL_ACCESS",
                translitTitle = "FULL_ACCESS"))
        }
        if (accessRoleRepository.findByTitle("GUEST") == null) {
            accessRoleRepository.save(AccessRole(title = "GUEST",
                translitTitle = "GUEST"))
        }
        if (accessRoleRepository.findByTitle("PUBLIC") == null) {
            accessRoleRepository.save(AccessRole(title = "PUBLIC",
                translitTitle = "PUBLIC"))
        }
        if (accessRoleRepository.findByTitle("USER") == null) {
            accessRoleRepository.save(AccessRole(title = "USER",
                translitTitle = "USER"))
        }
        val adminRole = roleRepository.findByTitle("ADMIN")
        val allAccess = accessRoleRepository.findByTitle("FULL_ACCESS")
        if (userRepository.findByEmail("admin@gmail.com") == null && adminRole != null) {
            userRepository.save(
                User(
                    firstName = "admin",
                    email = "admin@gmail.com",
                    password = "\$2a\$10\$llO9fZrWTXF7t3kv7BlvOu9FehXatOi1gEe/UOcpNpGExA1EQiGyK",
                    lastName = "Adminov",
                    isDelete = false,
                    role = adminRole,
                    accessRoles = if (allAccess != null) mutableListOf(allAccess) else mutableListOf()
                )
            )
        }
        val rootRole = roleRepository.findByTitle("ROOT")

    }
}