package com.knowledge.base.controller

import org.springframework.web.bind.annotation.*
import com.knowledge.base.dto.UserDto
import com.knowledge.base.service.RefreshTokenService
import com.knowledge.base.service.UserService
import com.knowledge.base.util.JwtUtil
import jakarta.servlet.http.HttpServletRequest
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
import org.springframework.web.bind.annotation.*
import java.time.Duration
@RestController
@RequestMapping("/api/user")
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
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUserPaginated(pageable: Pageable): ResponseEntity<Page<UserDto>> {
        val users = userService.getAllUsers(pageable)
        return ResponseEntity.ok(users)
    }

    // Новый метод: фильтрация с поддержкой isDelete/isFromLdap/lastName/email
    @GetMapping("/all/filter")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUsersFiltered(
        pageable: Pageable,
        @RequestParam(required = false) lastName: String?,
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) isFromLdap: Boolean?,
        @RequestParam(required = false) isDelete: Boolean?
    ): ResponseEntity<Page<UserDto>> {
        val page = userService.getUsersFiltered(pageable, lastName, email, isFromLdap, isDelete)
        return ResponseEntity.ok(page)
    }

    @GetMapping("/all/is-delete-false")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUserIsDeleteFalse(pageable: Pageable): ResponseEntity<Page<UserDto>> {
        val user = userService.getAllUserIsDeleteFalse(pageable)
        return ResponseEntity.ok(user)
    }

    @GetMapping("/all/is-delete-true")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUserIsDeleteTrue(pageable: Pageable): ResponseEntity<Page<UserDto>> {
        val user = userService.getAllUserIsDeleteTrue(pageable)
        return ResponseEntity.ok(user)
    }

    @GetMapping("/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserByEmail(@RequestParam email: String): ResponseEntity<UserDto?> {
        return ResponseEntity.ok(userService.findByEmail(email))
    }

    @GetMapping("/{first-name}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserByFirstName(@RequestParam firstName: String): ResponseEntity<List<UserDto>> {
        return ResponseEntity.ok(userService.findByFirstName(firstName))
    }

    @GetMapping("/{last-name}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserByLastName(@RequestParam lastName: String): ResponseEntity<List<UserDto>> {
        return ResponseEntity.ok(userService.findByLastName(lastName))
    }

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

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    fun createUser(@RequestBody userDto: UserDto): ResponseEntity<UserDto> {
        val newUser: UserDto = userService.createUser(userDto)
        return ResponseEntity.ok(newUser)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateUserAdmin(@PathVariable id: Long, @RequestBody userDto: UserDto): ResponseEntity<UserDto> {
        val exUser = userService.updateUser(id, userDto)
        return if (exUser != null) ResponseEntity.ok(exUser) else ResponseEntity.notFound().build()
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    fun restoreUser(@PathVariable id: Long): ResponseEntity<UserDto> {
        val restoredUser = userService.restoreUser(id)
        return if (restoredUser != null) ResponseEntity.ok(restoredUser) else ResponseEntity.notFound().build()
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUserId(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<Map<String, Any>> {
        val sam = userDetails.username
        val user = userService.findByEmail(sam)
        return if (user != null) {
            ResponseEntity.ok(mapOf("userId" to user.id))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/all/from-ldap")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUsersFromLdap(pageable: Pageable): ResponseEntity<Page<UserDto>> {
        val users = userService.getUsersFromLdap(pageable)
        return ResponseEntity.ok(users)
    }

    @GetMapping("/get-id/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserDto> {
        val user = userService.getUserById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user)
    }
}
