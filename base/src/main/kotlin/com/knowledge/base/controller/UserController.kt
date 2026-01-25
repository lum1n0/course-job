package com.knowledge.base.controller

import org.springframework.web.bind.annotation.*
import com.knowledge.base.dto.CreateUserRequest
import com.knowledge.base.dto.UserDto
import com.knowledge.base.service.RefreshTokenService
import com.knowledge.base.service.UserService
import com.knowledge.base.util.JwtUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import java.time.Duration

@RestController
@RequestMapping("/api/user")
@Tag(name = "Users", description = "Управление пользователями")
class UserController(
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager,
    private val jwtUtil: JwtUtil,
    private val refreshTokenService: RefreshTokenService,
    @Value("\${app.cookie.secure:false}") private val cookieSecureDefault: Boolean
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)
    private val cookieName = "refreshToken"

    private fun isSecure(request: HttpServletRequest): Boolean {
        val xfProto = request.getHeader("X-Forwarded-Proto") ?: ""
        return cookieSecureDefault || request.isSecure || xfProto.equals("https", ignoreCase = true)
    }

    @Operation(
        summary = "Получить всех пользователей",
        description = "Возвращает всех пользователей с пагинацией (только для ADMIN)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список пользователей успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Page::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUserPaginated(@Parameter(hidden = true) pageable: Pageable): ResponseEntity<Page<UserDto>> {
        val users = userService.getAllUsers(pageable)
        return ResponseEntity.ok(users)
    }

    @Operation(
        summary = "Получить пользователей с фильтрацией",
        description = "Возвращает пользователей с фильтрацией по фамилии, email, LDAP и статусу удаления (только для ADMIN)",
        parameters = [
            Parameter(name = "lastName", description = "Фамилия для поиска", required = false, `in` = ParameterIn.QUERY),
            Parameter(name = "email", description = "Email для поиска", required = false, `in` = ParameterIn.QUERY),
            Parameter(name = "isFromLdap", description = "Фильтр по LDAP", required = false, `in` = ParameterIn.QUERY),
            Parameter(name = "isDelete", description = "Фильтр по статусу удаления", required = false, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список пользователей успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Page::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @GetMapping("/all/filter")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUsersFiltered(
        @Parameter(hidden = true) pageable: Pageable,
        @Parameter(description = "Фамилия для поиска", required = false) @RequestParam(required = false) lastName: String?,
        @Parameter(description = "Email для поиска", required = false) @RequestParam(required = false) email: String?,
        @Parameter(description = "Фильтр по LDAP", required = false) @RequestParam(required = false) isFromLdap: Boolean?,
        @Parameter(description = "Фильтр по статусу удаления", required = false) @RequestParam(required = false) isDelete: Boolean?
    ): ResponseEntity<Page<UserDto>> {
        val page = userService.getUsersFiltered(pageable, lastName, email, isFromLdap, isDelete)
        return ResponseEntity.ok(page)
    }

    @Operation(
        summary = "Получить активных пользователей",
        description = "Возвращает пользователей, которые не помечены как удаленные (только для ADMIN)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список активных пользователей успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Page::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @GetMapping("/all/is-delete-false")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUserIsDeleteFalse(@Parameter(hidden = true) pageable: Pageable): ResponseEntity<Page<UserDto>> {
        val user = userService.getAllUserIsDeleteFalse(pageable)
        return ResponseEntity.ok(user)
    }

    @Operation(
        summary = "Получить удаленных пользователей",
        description = "Возвращает пользователей, которые помечены как удаленные (только для ADMIN)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список удаленных пользователей успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Page::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @GetMapping("/all/is-delete-true")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUserIsDeleteTrue(@Parameter(hidden = true) pageable: Pageable): ResponseEntity<Page<UserDto>> {
        val user = userService.getAllUserIsDeleteTrue(pageable)
        return ResponseEntity.ok(user)
    }

    @Operation(
        summary = "Получить пользователя по email",
        description = "Возвращает пользователя с указанным email (только для ADMIN)",
        parameters = [
            Parameter(name = "email", description = "Email пользователя", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Пользователь успешно найден",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @GetMapping("/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserByEmail(@Parameter(description = "Email пользователя", required = true) @RequestParam email: String): ResponseEntity<UserDto> {
        return ResponseEntity.ok(userService.findByEmail(email))
    }

    @Operation(
        summary = "Получить пользователей по имени",
        description = "Возвращает список пользователей с указанным именем (только для ADMIN)",
        parameters = [
            Parameter(name = "firstName", description = "Имя пользователя", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список пользователей успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @GetMapping("/{first-name}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserByFirstName(@Parameter(description = "Имя пользователя", required = true) @RequestParam firstName: String): ResponseEntity<List<UserDto>> {
        return ResponseEntity.ok(userService.findByFirstName(firstName))
    }

    @Operation(
        summary = "Получить пользователей по фамилии",
        description = "Возвращает список пользователей с указанной фамилией (только для ADMIN)",
        parameters = [
            Parameter(name = "lastName", description = "Фамилия пользователя", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список пользователей успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @GetMapping("/{last-name}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserByLastName(@Parameter(description = "Фамилия пользователя", required = true) @RequestParam lastName: String): ResponseEntity<List<UserDto>> {
        return ResponseEntity.ok(userService.findByLastName(lastName))
    }

    @Operation(
        summary = "Вход в систему",
        description = "Аутентифицирует пользователя и возвращает JWT токен",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные для входа (email и пароль)",
            required = true,
            content = [Content(
                mediaType = "application/json"
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Успешный вход, токен возвращен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Map::class)
                )]
            ),
            ApiResponse(responseCode = "401", description = "Неверные учетные данные")
        ]
    )
    @PostMapping("/login")
    fun login(
        @RequestBody authRequest: com.knowledge.base.dto.AuthRequest,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, String>> {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(authRequest.email, authRequest.password)
        )
        val userDetails = authentication.principal as UserDetails
        val ua = request.getHeader("User-Agent")
        val ip = request.remoteAddr
        val (rawRefresh, savedRt) = refreshTokenService.issueOnLogin(userDetails.username, ua, ip)
        val access = jwtUtil.generateAccessToken(userDetails, savedRt.tokenFamily)
        val setCookie = ResponseCookie.from(cookieName, rawRefresh)
            .httpOnly(true)
            .secure(isSecure(request))
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofSeconds(refreshTokenService.refreshTtlSeconds()))
            .build()
        if (logger.isDebugEnabled) {
            logger.debug("Login JWT head='${access.take(30)}...', len=${access.length}")
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, setCookie.toString())
            .body(mapOf("token" to access))
    }

    @Operation(
        summary = "Создать пользователя",
        description = "Создает нового пользователя в системе (только для ADMIN). Валидация: email должен содержать @, пароль минимум 6 символов.",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные нового пользователя",
            required = true,
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = CreateUserRequest::class)
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Пользователь успешно создан",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Ошибка валидации данных"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserDto> {
        val newUser: UserDto = userService.createUser(request.toUserDto())
        return ResponseEntity.ok(newUser)
    }

    @Operation(
        summary = "Обновить пользователя",
        description = "Обновляет данные существующего пользователя (только для ADMIN)",
        parameters = [
            Parameter(name = "id", description = "ID пользователя", required = true, `in` = ParameterIn.PATH)
        ],
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Обновленные данные пользователя",
            required = true,
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = UserDto::class)
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Пользователь успешно обновлен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateUserAdmin(
        @Parameter(description = "ID пользователя", required = true) @PathVariable id: Long,
        @RequestBody userDto: UserDto
    ): ResponseEntity<UserDto> {
        val exUser = userService.updateUser(id, userDto)
        return if (exUser != null) ResponseEntity.ok(exUser) else ResponseEntity.notFound().build()
    }

    @Operation(
        summary = "Восстановить пользователя",
        description = "Восстанавливает удаленного пользователя (только для ADMIN)",
        parameters = [
            Parameter(name = "id", description = "ID пользователя", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Пользователь успешно восстановлен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    fun restoreUser(@Parameter(description = "ID пользователя", required = true) @PathVariable id: Long): ResponseEntity<UserDto> {
        val restoredUser = userService.restoreUser(id)
        return if (restoredUser != null) ResponseEntity.ok(restoredUser) else ResponseEntity.notFound().build()
    }

    @Operation(
        summary = "Получить ID текущего пользователя",
        description = "Возвращает ID текущего авторизованного пользователя",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "ID пользователя успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Map::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Пользователь не найден")
        ]
    )
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUserId(@Parameter(hidden = true) @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<Map<String, Long>> {
        val sam = userDetails.username
        val user = userService.findByEmail(sam)
        return if (user != null) {
            ResponseEntity.ok(mapOf("userId" to user.id))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(
        summary = "Получить пользователей из LDAP",
        description = "Возвращает пользователей, импортированных из LDAP/Active Directory (только для ADMIN)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список пользователей успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Page::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @GetMapping("/all/from-ldap")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUsersFromLdap(@Parameter(hidden = true) pageable: Pageable): ResponseEntity<Page<UserDto>> {
        val users = userService.getUsersFromLdap(pageable)
        return ResponseEntity.ok(users)
    }

    @Operation(
        summary = "Получить пользователя по ID",
        description = "Возвращает пользователя с указанным ID",
        parameters = [
            Parameter(name = "id", description = "ID пользователя", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Пользователь успешно найден",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Пользователь не найден")
        ]
    )
    @GetMapping("/get-id/{id}")
    fun getUserById(@Parameter(description = "ID пользователя", required = true) @PathVariable id: Long): ResponseEntity<UserDto> {
        val user = userService.getUserById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user)
    }
}
