package com.knowledge.base.service

import com.knowledge.base.dto.LdapUserDto
import org.springframework.ldap.core.AttributesMapper
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.query.LdapQueryBuilder
import org.springframework.stereotype.Service

@Service
class LdapService(private val ldapTemplate: LdapTemplate) {

    /**
     * Получает список всех пользователей из LDAP.
     */
    fun getAllLdapUsers(): List<LdapUserDto> {
        val query = LdapQueryBuilder.query()
            .base("ou=users") // Ищем в организационной единице 'users'
            .where("objectClass").`is`("person") // Нас интересуют только объекты класса 'person'

        val attributesMapper = AttributesMapper<LdapUserDto> { attrs ->
            LdapUserDto(
                uid = attrs.get("uid")?.get()?.toString() ?: "N/A",
                fullName = attrs.get("cn")?.get()?.toString() ?: "N/A",
                email = attrs.get("mail")?.get()?.toString() ?: "N/A"
            )
        }
        return ldapTemplate.search(query, attributesMapper)
    }
}
