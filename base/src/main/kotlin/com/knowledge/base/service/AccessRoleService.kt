package com.knowledge.base.service

import com.knowledge.base.dto.AccessRoleDto
import com.knowledge.base.mapper.AccessRoleMapper
import com.knowledge.base.model.AccessRole
import com.knowledge.base.repository.AccessRoleRepository
import com.knowledge.base.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccessRoleService(
    private val accessRoleRepository: AccessRoleRepository,
    private val accessRoleMapper: AccessRoleMapper,
    private val userRepository: UserRepository,
    private val maintenance: AccessRoleMaintenanceService
) {

    @Transactional
    fun createAccessRole(accessRoleDto: AccessRoleDto): AccessRoleDto {
        val base = accessRoleMapper.toEntity(accessRoleDto)
        val fixed = maintenance.ensureTranslit(base)
        val saved = accessRoleRepository.save(fixed)
        return accessRoleMapper.toDto(saved)
    }

    @Transactional(readOnly = true)
    fun findAccessRoleByTitle(title: String): AccessRoleDto? {
        val findAR = accessRoleRepository.findByTitle(title)
        return findAR?.let { accessRoleMapper.toDto(it) }
    }

    @Transactional(readOnly = true)
    fun getAllAccessRole(): List<AccessRoleDto> {
        return accessRoleRepository.findAll().map { accessRoleMapper.toDto(it) }
    }

    @Transactional(readOnly = true)
    fun checkUserHasAccessRole(userId: Long, accessRoleTitle: String): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false
        return user.accessRoles.any { it.title == accessRoleTitle }
    }
    @Transactional(readOnly = true)
    fun getAccessRolesVisibleForUser(email: String): List<AccessRoleDto> {
        val user = userRepository.findByEmail(email) ?: return emptyList()
        val isAdmin = user.role.title == "ADMIN"

        val userAccessRoles = user.accessRoles
        val hasFullAccess = userAccessRoles.any { it.title == "FULL_ACCESS" }

        return when {
            isAdmin -> {
                accessRoleRepository.findAll().map { accessRoleMapper.toDto(it) }
            }
            hasFullAccess -> {
                accessRoleRepository.findAll()
                    .asSequence()
                    .filter { it.title != "FULL_ACCESS" }
                    .map { accessRoleMapper.toDto(it) }
                    .toList()
            }
            else -> {
                userAccessRoles.map { accessRoleMapper.toDto(it) }
            }
        }
    }

    @Transactional
    fun deleteAccessRoleById(id: Long) {
        accessRoleRepository.deleteById(id)
    }

    // Обновление AccessRole (заполнит/обновит translitTitle)
    @Transactional
    fun updateAccessRole(id: Long, dto: AccessRoleDto): AccessRoleDto {
        val accessRole = accessRoleRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Роль доступа с ID $id не найдена") }

        val newTitle = dto.title.ifBlank { accessRole.title }
        val candidate = AccessRole(
            id = accessRole.id,
            title = newTitle,
            translitTitle = dto.translitTitle.ifBlank { accessRole.translitTitle },
            users = accessRole.users,
            categories = accessRole.categories
        )
        val ensured = maintenance.ensureTranslit(candidate)
        val saved = accessRoleRepository.save(ensured)
        return accessRoleMapper.toDto(saved)
    }
}