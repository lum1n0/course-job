package com.knowledge.base.service

import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserDetailsServiceImpl(
    private val userService: UserService,
    private val accessRoleMaintenance: AccessRoleMaintenanceService
) : UserDetailsService {

    private val logger = LoggerFactory.getLogger(UserDetailsServiceImpl::class.java)

    @Transactional
    override fun loadUserByUsername(username: String): UserDetails {
        logger.info("Loading user details for SAM: {}", username)
        val shadowUser = userService.findOrCreateShadowUser(username)
        logger.info("Building UserDetails for SAM: {}. Role: {}", shadowUser.email, shadowUser.role.title)

        // Ленивая починка для его ролей
        accessRoleMaintenance.ensureTranslitForUserRoles(shadowUser)

        val authorities = mutableListOf(SimpleGrantedAuthority("ROLE_${shadowUser.role.title}"))
        shadowUser.accessRoles.forEach { ar ->
            val code = ar.translitTitle?.trim().orEmpty()
            val authority = if (code.isBlank()) accessRoleMaintenance.normalizeAuthority(ar.title) else code
            authorities.add(SimpleGrantedAuthority(authority))
        }

        val password = shadowUser.password ?: ""
        return User(shadowUser.email, password, authorities)
    }
}

