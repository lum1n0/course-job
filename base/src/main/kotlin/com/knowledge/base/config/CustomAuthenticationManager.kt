package com.knowledge.base.config

import com.knowledge.base.repository.UserRepository
import com.knowledge.base.service.UserDetailsServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider
import org.springframework.stereotype.Component

// CustomAuthenticationManager.kt

@Component
class CustomAuthenticationManager(
    private val env: Environment,
    private val ldapProvider: LdapAuthenticationProvider?, // один провайдер для dev/prod
    private val daoProvider: DaoAuthenticationProvider,
    private val userDetailsService: UserDetailsServiceImpl,
    private val userRepository: UserRepository
) : AuthenticationManager {

    private val logger = LoggerFactory.getLogger(CustomAuthenticationManager::class.java)

    private val localOnlyUsers = setOf("admin@gmail.com")

    override fun authenticate(authentication: Authentication): Authentication {
        val raw = authentication.name
        val password = authentication.credentials
        logger.debug("CustomAuthenticationManager: start auth for '$raw'.")

        // --- ИЗМЕНЕНИЕ: Быстрая проверка с игнорированием регистра и обработкой дубликатов ---
        try {
            // Используем findAll, чтобы избежать ошибки NonUniqueResultException
            val users = userRepository.findAllByEmailIgnoreCase(raw)
            // Если есть хотя бы один локальный пользователь — используем DAO
            if (users.any { !it.isFromLdap }) {
                logger.debug("User '$raw' found in DB as local (isFromLdap=false). Authenticating via DAO.")
                return authenticateDao(raw, password)
            }
        } catch (e: Exception) {
            logger.error("Error during quick DB check for '$raw'", e)
        }
        // -------------------------------------------------------------

        val isProd = env.activeProfiles.contains("prod")
        val normalized = normalizeLogin(raw)
        logger.debug("Normalized login for auth (sAM/uid): '$normalized' (isProd=$isProd)")

        if (isLocalLoginCandidate(raw, normalized)) {
            logger.debug("Local-only candidate '$raw'. Trying DAO first, skipping LDAP.")
            return authenticateDao(raw, password)
        }

        return authenticateViaLdapOrFail(raw, normalized, password, isProd)
    }

    private fun authenticateDao(usernameRaw: String, password: Any?): Authentication {
        val daoToken = UsernamePasswordAuthenticationToken(usernameRaw, password)
        return try {
            val result = daoProvider.authenticate(daoToken)
            logger.info("DAO auth successful for '$usernameRaw'.")
            result
        } catch (e: AuthenticationException) {
            logger.warn("DAO auth failed for '$usernameRaw': ${e.message}")
            throw e
        }
    }

    private fun authenticateViaLdapOrFail(
        raw: String,
        normalized: String,
        password: Any?,
        isProd: Boolean
    ): Authentication {
        if (ldapProvider == null) {
            logger.error("LDAP provider is null in profile=${env.activeProfiles}. Check @Profile and bean creation.")
            throw BadCredentialsException("Authentication service unavailable (LDAP not configured)")
        }

        val token = UsernamePasswordAuthenticationToken(normalized, password)
        return try {
            ldapProvider.authenticate(token)
            logger.info("LDAP auth successful for '$normalized' (profile=${if (isProd) "prod" else "dev"}).")

            val userDetails = userDetailsService.loadUserByUsername(normalized)
            UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        } catch (e: AuthenticationException) {
            logger.warn("LDAP auth failed for '$normalized': ${e.message}")

            if (!isProd && raw.contains("@")) {
                logger.info("Dev profile and email-like login '$raw' – trying DAO fallback.")
                return authenticateDao(raw, password)
            }

            throw e
        }
    }

    private fun isLocalLoginCandidate(raw: String, normalized: String): Boolean {
        val lowerRaw = raw.lowercase()
        val lowerNorm = normalized.lowercase()
        return localOnlyUsers.contains(lowerRaw) || localOnlyUsers.contains(lowerNorm)
    }

    private fun normalizeLogin(input: String): String {
        val backslash = input.indexOf('\\')
        val base = if (backslash >= 0 && backslash < input.length - 1) {
            input.substring(backslash + 1)
        } else {
            input
        }
        return base.substringBefore("@")
    }
}
