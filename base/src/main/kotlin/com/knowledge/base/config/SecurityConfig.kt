package com.knowledge.base.config

import com.knowledge.base.service.UserDetailsServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.ldap.core.support.BaseLdapPathContextSource
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.ldap.authentication.BindAuthenticator
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val userDetailsService: UserDetailsServiceImpl,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val env: Environment,
    private val contextSource: BaseLdapPathContextSource
) {

    @Bean
    fun daoAuthenticationProvider(passwordEncoder: PasswordEncoder): DaoAuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder)
        return provider
    }

    @Bean
    @Profile("dev")
    fun ldapAuthenticationProviderDev(contextSource: BaseLdapPathContextSource): LdapAuthenticationProvider {
        val userSearch = FilterBasedLdapUserSearch(
            "ou=users",
            "(uid={0})",
            contextSource
        )
        val authenticator = BindAuthenticator(contextSource).apply {
            setUserSearch(userSearch)
        }
        val authoritiesPopulator = DefaultLdapAuthoritiesPopulator(contextSource, null)
        return LdapAuthenticationProvider(authenticator, authoritiesPopulator)
    }

    @Bean
    @Profile("prod")
    fun ldapAuthenticationProviderProd(
        contextSource: BaseLdapPathContextSource
    ): LdapAuthenticationProvider {
        val userSearch = FilterBasedLdapUserSearch(
            "",                      // ищем во всём base, заданном в LdapContextSource (DC=llc,DC=tagras,DC=corp)
            "(sAMAccountName={0})",  // используем только sAMAccountName
            contextSource
        )
        val authenticator = BindAuthenticator(contextSource).apply {
            setUserSearch(userSearch)
        }
        val authoritiesPopulator = DefaultLdapAuthoritiesPopulator(contextSource, null)
        return LdapAuthenticationProvider(authenticator, authoritiesPopulator)
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        customAuthenticationManager: CustomAuthenticationManager
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authenticationManager(customAuthenticationManager)
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/moderator-permissions/me/**").authenticated()
                    .requestMatchers("/api/moderator-permissions/**").hasRole("ADMIN")


                    .requestMatchers(HttpMethod.POST, "/api/public/articles/submit").authenticated()

                    .requestMatchers("/api/stats/counters-articles",
                        "/api/stats/counters-categories"
                    ).authenticated()
                    .requestMatchers("/api/stats/articles/frequency",
                        "/api/stats/categories/frequency"
                        ).hasRole("ADMIN")




                    .requestMatchers("/api/notifications/send").hasAnyRole("ADMIN", "MODERATOR")
                    .requestMatchers("/api/notifications").permitAll()

                    .requestMatchers("/api/user/login").permitAll()
                    .requestMatchers("/api/auth/refresh").permitAll()
                    .requestMatchers("/api/auth/logout").permitAll()

                    .requestMatchers(HttpMethod.GET, "/api/articles/*/views").permitAll()


                    // Новые правила для версий статей:
                    .requestMatchers(HttpMethod.GET, "/api/articles/*/versions/*/author").permitAll()
                    // Просмотр списка версий и конкретной версии
                    .requestMatchers(HttpMethod.GET, "/api/articles/*/versions/**").authenticated()
                    // Сравнение двух версий одной статьи
                    .requestMatchers(HttpMethod.GET, "/api/articles/*/compare").authenticated()
                    // Восстановление версии
                    .requestMatchers(HttpMethod.POST, "/api/articles/*/versions/*/restore").hasAnyRole("ADMIN", "WRITER", "MODERATOR, SUPER-WRITER")
                    // Удаление версии
                    .requestMatchers(HttpMethod.DELETE, "/api/articles/*/versions/*").hasAnyRole("ADMIN", "WRITER", "MODERATOR", "SUPER-WRITER")

                    // Модерация
                    .requestMatchers(HttpMethod.POST, "/api/moderation/submit/create").hasAnyRole("WRITER", "SUPER-WRITER")
                    .requestMatchers(HttpMethod.POST, "/api/moderation/submit/update/*").hasAnyRole("WRITER", "SUPER-WRITER")
                    .requestMatchers(HttpMethod.GET, "/api/moderation/pending").hasAnyRole("ADMIN", "MODERATOR")
                    .requestMatchers(HttpMethod.GET, "/api/moderation/completed").hasAnyRole("ADMIN", "MODERATOR")
                    .requestMatchers(HttpMethod.GET, "/api/moderation/proposals/*").hasAnyRole("ADMIN", "MODERATOR")
                    .requestMatchers(HttpMethod.POST, "/api/moderation/proposals/*/approve").hasAnyRole("ADMIN", "MODERATOR")
                    .requestMatchers(HttpMethod.POST, "/api/moderation/proposals/*/reject").hasAnyRole("ADMIN", "MODERATOR")

                    // Мои работы
                    .requestMatchers(HttpMethod.GET, "/api/my/work").authenticated()

                    // Перемещение категории (смена родителя) - только для ADMIN и WRITER
                    .requestMatchers(HttpMethod.PUT, "/api/category/{id}/move").hasRole("ADMIN")
                    // Получение дочерних категорий - для всех аутентифицированных
                    .requestMatchers(HttpMethod.GET, "/api/category/{id}/children").authenticated()
                    // Получение статей категории - для всех аутентифицированных
                    .requestMatchers(HttpMethod.GET, "/api/category/{id}/articles").authenticated()
                    // Получение полного содержимого категории (статьи + подкатегории)
                    .requestMatchers(HttpMethod.GET, "/api/category/{id}/content").authenticated()
                    // Получение полного дерева категорий - для всех аутентифицированных
                    .requestMatchers(HttpMethod.GET, "/api/category/tree").authenticated()

                    .requestMatchers("/api/writer-permissions/me/**").authenticated()
                    .requestMatchers("/api/writer-permissions/me/categories-editable/**").authenticated()
                    .requestMatchers("/api/writer-permissions/me/can-edit/**").hasAnyRole("ADMIN", "WRITER", "MODERATOR")
                    .requestMatchers("/api/writer-permissions/**").hasRole("ADMIN")

                    .requestMatchers("/error/**").permitAll()
                    .requestMatchers("/api/user/login").permitAll()
                    .requestMatchers("/api/ldap/users").hasRole("ADMIN")
                    .requestMatchers("/api/files/**").permitAll()
                    .requestMatchers("/files/**").permitAll()
                    .requestMatchers("/images/**").permitAll()
                    .requestMatchers("/api/chat/session/**").permitAll()
                    .requestMatchers("/chat/**").permitAll()
                    .requestMatchers("/ws/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/guest/categories").permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/api/guest/categories").permitAll()
                    .requestMatchers("/api/guest/articles/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/guest/articles/{id}").permitAll()
                    .requestMatchers("/api/user/me").authenticated()
                    .requestMatchers("/api/articles/{id}/pdf").permitAll()
                    .requestMatchers("/api/user/current").authenticated()
                    .requestMatchers("/api/articles/slidebar/**").authenticated()
                    .requestMatchers(HttpMethod.GET, "/videos/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/files/**").authenticated()
                    .requestMatchers(HttpMethod.GET, "/files/**").authenticated()
                    .requestMatchers("/api/access-role/full/**").authenticated()
                    .requestMatchers("/api/articles/admin/by-category").hasAnyRole("ADMIN", "WRITER", "MODERATOR", "SUPER-WRITER")
                    .requestMatchers("/api/user/get-id/**").hasAnyRole("ADMIN", "WRITER", "MODERATOR","SUPER-WRITER")
                    .requestMatchers(
                        "/api/user/all",
                        "/api/user/all/is-delete-false",
                        "/api/user/all/is-delete-true",
                        "/api/user/add",
                        "/api/user/{email}",
                        "/api/user/{first-name}",
                        "/api/user/{last-name}"
                    ).hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/user/{id}", "/api/user/{id}/restore").hasRole("ADMIN")
                    .requestMatchers("/api/articles/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/feedback/all").hasRole("ADMIN")
                    .requestMatchers("/api/user/all/from-ldap").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/access-role/delete/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/articles/delete/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/category/delete/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/category").hasAnyRole("ADMIN", "SUPER-WRITER")
                    .requestMatchers(HttpMethod.GET, "/api/articles/all").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/articles/{id}/soft-delete").hasAnyRole("ADMIN", "WRITER", "MODERATOR", "SUPER-WRITER")

                    .requestMatchers(HttpMethod.PUT, "/api/category/{id}").hasAnyRole("ADMIN", "SUPER-WRITER")
                    .requestMatchers(HttpMethod.PUT, "/api/category/{id}/soft-delete").hasAnyRole("ADMIN", "SUPER-WRITER")
                    .requestMatchers("/api/category/search-admin/").hasAnyRole("ADMIN", "WRITER", "MODERATOR")
                    .requestMatchers(HttpMethod.GET, "/api/category/all").hasAnyRole("ADMIN", "WRITER", "MODERATOR", "SUPER-WRITER")
                    .requestMatchers(HttpMethod.GET, "/api/access-role/all").hasAnyRole("ADMIN", "WRITER", "MODERATOR", "SUPER-WRITER")
                    .requestMatchers(HttpMethod.GET, "/api/access-role").hasAnyRole("ADMIN", "WRITER", "MODERATOR", "SUPER-WRITER")
                    .requestMatchers(HttpMethod.POST, "/api/access-role").hasAnyRole("ADMIN", "WRITER", "MODERATOR", "SUPER-WRITER")
                    .requestMatchers("/api/access-role/**").hasAnyRole("ADMIN", "WRITER", "MODERATOR", "SUPER-WRITER")
                    .requestMatchers(HttpMethod.POST, "/api/articles/upload-image").hasAnyRole("ADMIN", "WRITER", "MODERATOR", "SUPER-WRITER")
                    .requestMatchers(HttpMethod.GET, "/api/articles/{id}").authenticated()

                    // ВАЖНО: публикацию и обновление статей теперь могут делать только ADMIN и MODERATOR
                    .requestMatchers(HttpMethod.POST, "/api/articles").hasAnyRole("ADMIN", "MODERATOR", "SUPER-WRITER" )
                    .requestMatchers(HttpMethod.PUT, "/api/articles/{id}").hasAnyRole("ADMIN", "MODERATOR", "SUPER-WRITER")

                    .requestMatchers("/api/feedback").authenticated()
                    .requestMatchers("/api/category/{userId}/for-user-all").authenticated()
                    .requestMatchers("/api/category/{description}/search-by/{userId}").authenticated()
                    .requestMatchers("/api/articles/by-category").authenticated()
                    .requestMatchers("/api/articles/search").authenticated()
                    // В SecurityConfig.kt добавьте:
                    .requestMatchers(HttpMethod.POST, "/api/articles/admin/fix-image-urls").hasRole("ADMIN")
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/demo/**"
                    ).permitAll()

                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
 configuration.allowedOrigins = listOf(
        "http://localhost:4200",
        "http://pro-znania:4200", 
        "https://pro-znania.llc.tagras.corp",
        "http://10.15.22.141:3000",
        "http://10.221.107.83:4200",
        "http://172.20.10.3:4200"
    )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "X-Requested-With", "X-Auth-Token")
        configuration.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
