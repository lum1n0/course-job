// base/src/main/kotlin/com/knowledge/base/config/LdapConfig.kt
package com.knowledge.base.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy
import org.springframework.ldap.core.support.LdapContextSource

@Configuration
class LdapConfig(private val env: Environment) {

    // DEV — embedded LDAP, без StartTLS
    @Bean
    @Profile("dev")
    fun ldapContextSourceDev(): LdapContextSource {
        val cs = LdapContextSource()
        cs.setUrl(env.getRequiredProperty("spring.ldap.urls"))      // ldap://10.15.23.244:8389 или 10.15.22.141:8389
        cs.setBase(env.getRequiredProperty("spring.ldap.base"))     // dc=example,dc=com
        cs.setUserDn(env.getRequiredProperty("spring.ldap.username"))
        cs.setPassword(env.getRequiredProperty("spring.ldap.password"))
        cs.setPooled(false)
        return cs
    }

    // PROD — внешний AD с StartTLS
    @Bean
    @Profile("prod")
    fun ldapContextSourceProd(): LdapContextSource {
        val cs = LdapContextSource()
        cs.setUrl(env.getRequiredProperty("spring.ldap.urls"))      // ldaps://llc01.llc.tagras.corp:636
        cs.setBase(env.getRequiredProperty("spring.ldap.base"))     // DC=llc,DC=tagras,DC=corp
        cs.setUserDn(env.getRequiredProperty("spring.ldap.username"))
        cs.setPassword(env.getRequiredProperty("spring.ldap.password"))
        cs.setPooled(false)

        // ВАЖНО: НИКАКОГО DefaultTlsDirContextAuthenticationStrategy здесь
        // LDAPS уже даёт TLS на уровне транспорта
        return cs
    }
}