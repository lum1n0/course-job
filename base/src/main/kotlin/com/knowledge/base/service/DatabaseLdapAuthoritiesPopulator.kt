package com.knowledge.base.service

import org.slf4j.LoggerFactory
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator
import org.springframework.stereotype.Service

@Service
class DatabaseLdapAuthoritiesPopulator(
    private val userDetailsService: UserDetailsServiceImpl
) : LdapAuthoritiesPopulator {

    private val logger = LoggerFactory.getLogger(DatabaseLdapAuthoritiesPopulator::class.java)

    override fun getGrantedAuthorities(
        userData: DirContextOperations,
        username: String
    ): MutableCollection<out GrantedAuthority> {
        logger.debug("Populating authorities for LDAP user '{}' from database.", username)
        val userDetails: UserDetails = userDetailsService.loadUserByUsername(username)
        return userDetails.authorities.toMutableList()
    }
}
