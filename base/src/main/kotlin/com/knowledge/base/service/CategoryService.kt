package com.knowledge.base.service;

import com.knowledge.base.dto.ArticleDto;
import com.knowledge.base.dto.CategoryContentDto;
import com.knowledge.base.dto.CategoryDto;
import com.knowledge.base.dto.CategoryTreeDto;
import com.knowledge.base.mapper.AccessRoleMapper;
import com.knowledge.base.mapper.ArticleMapper;
import com.knowledge.base.mapper.CategoryMapper;
import com.knowledge.base.model.Category;
import com.knowledge.base.repository.AccessRoleRepository;
import com.knowledge.base.repository.ArticleRepository;
import com.knowledge.base.repository.CategoryRepository;
import com.knowledge.base.repository.ModeratorPermissionRepository
import com.knowledge.base.repository.UserRepository;
import com.knowledge.base.repository.WriterPermissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val categoryMapper: CategoryMapper,
    private val accessRoleMapper: AccessRoleMapper,
    private val userRepository: UserRepository,
    private val accessRoleRepository: AccessRoleRepository,
    private val articleRepository: ArticleRepository,
    private val articleMapper: ArticleMapper,
    private val writerPermissionRepository: WriterPermissionRepository,
    private val moderatorPermissionRepository: ModeratorPermissionRepository,

) {
    @Transactional(readOnly = true)
    fun getGuestCategories(): List<CategoryDto> {
        val guestRole = accessRoleRepository.findByTitle("GUEST")
            ?: throw IllegalArgumentException("GUEST role not found")
        val categories = categoryRepository.findAllByIsDeleteFalse()
            .filter { category -> category.accessRoles.any { it.id == guestRole.id } }
        return categories.map { categoryMapper.toDto(it) }
    }

    @Transactional
    fun addCategory(categoryDto: CategoryDto): CategoryDto {
        val accessRoleEntities = categoryDto.accessRolesDto.map { roleDto ->
            accessRoleRepository.findById(roleDto.id).orElseThrow {
                IllegalArgumentException("AccessRole with id ${roleDto.id} not found")
            }
        }
        val parent = categoryDto.parentId?.let {
            categoryRepository.findById(it).orElseThrow { IllegalArgumentException("Parent category not found") }
        }

        val siblingDescriptions: Set<String> = if (parent != null) {
            categoryRepository.findAllByParentIdAndIsDeleteFalse(parent.id).map { it.description }.toSet()
        } else {
            categoryRepository.findAllByIsDeleteFalseAndParentIsNull().map { it.description }.toSet()
        }

        val uniqueDescription = generateUniqueDescription(categoryDto.description, siblingDescriptions)

        val category = Category(
            description = uniqueDescription,
            iconPath = categoryDto.iconPath,
            isDelete = categoryDto.isDelete,
            parent = parent,
            accessRoles = accessRoleEntities.toMutableList()
        )
        val saved = categoryRepository.save(category)
        return categoryMapper.toDto(saved)
    }

    @Transactional
    fun updateCategory(id: Long, categoryDto: CategoryDto): CategoryDto? {
        val exCategory = categoryRepository.findById(id).orElse(null) ?: return null
        val accessRoleEntities = categoryDto.accessRolesDto.map { roleDto ->
            accessRoleRepository.findById(roleDto.id).orElseThrow {
                IllegalArgumentException("AccessRole with id ${roleDto.id} not found")
            }
        }
        val parent = categoryDto.parentId?.let {
            categoryRepository.findById(it).orElseThrow { IllegalArgumentException("Parent category not found") }
        }

        if (categoryDto.parentId != null && categoryDto.parentId == id) {
            throw IllegalArgumentException("Cannot set parent to self")
        }
        if (categoryDto.parentId != null && categoryRepository.isDescendant(id, categoryDto.parentId)) {
            throw IllegalArgumentException("Cannot move into own subtree")
        }

        val updated = exCategory.copy(
            description = categoryDto.description,
            iconPath = categoryDto.iconPath,
            isDelete = categoryDto.isDelete,
            accessRoles = accessRoleEntities.toMutableList()
        )
        updated.parent = parent
        val saved = categoryRepository.save(updated)
        return categoryMapper.toDto(saved)
    }

    @Transactional
    fun softDeleteCategory(categoryId: Long, newIsDeleteStatus: Boolean): CategoryDto? {
        val exCategory = categoryRepository.findById(categoryId).orElse(null) ?: return null
        val updateStatus = exCategory.copy(isDelete = newIsDeleteStatus)
        val saveStatus = categoryRepository.save(updateStatus)
        return categoryMapper.toDto(saveStatus)
    }

    @Transactional(readOnly = true)
    fun getAllCategory(pageable: Pageable): Page<CategoryDto> {
        val pageCategory: Page<Category> = categoryRepository.findAll(pageable)
        return pageCategory.map { category ->
            category.accessRoles.size
            categoryMapper.toDto(category)
        }
    }

    @Transactional(readOnly = true)
    fun getCategoryIsDeleteFalse(userId: Long): List<CategoryDto> {
        val user = userRepository.findById(userId).orElse(null) ?: return emptyList()
        val userAccessRoles = user.accessRoles.map { it.title }.toSet()
        if ("FULL_ACCESS" in userAccessRoles) {
            val allCategories = categoryRepository.findAllByIsDeleteFalse()
            return allCategories.map { categoryMapper.toDto(it) }
        }
        val userAccessRoleIds = user.accessRoles.map { it.id }.toSet()
        val categories = categoryRepository.findAllByIsDeleteFalse()
        val filterCategory = categories.filter { category ->
            category.accessRoles.any { it.id in userAccessRoleIds }
        }
        return filterCategory.map { category ->
            category.accessRoles.size
            categoryMapper.toDto(category)
        }
    }

    @Transactional(readOnly = true)
    fun findCategoryByDescription(description: String, userId: Long): List<CategoryDto> {
        val user = userRepository.findById(userId).orElse(null) ?: return emptyList()
        val userAccessRole = user.accessRoles.map { it.id }.toSet()
        val categories: List<Category> =
            categoryRepository.findByDescriptionContainingIgnoreCaseAndIsDeleteFalse(description)
        val filterCategory = categories.filter { category ->
            category.accessRoles.any { it.id in userAccessRole }
        }
        return filterCategory.map { category ->
            category.accessRoles.size
            categoryMapper.toDto(category)
        }
    }

    @Transactional(readOnly = true)
    fun findCategoryByDescriptionToAdmin(description: String): List<CategoryDto> {
        val categories: List<Category> =
            categoryRepository.findByDescriptionContainingIgnoreCase(description)
        return categories.map { category ->
            category.accessRoles.size
            categoryMapper.toDto(category)
        }
    }

    fun deleteCategoryById(id: Long) {
        categoryRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun getCategoryEntityById(id: Long): Category? =
        categoryRepository.findById(id).orElse(null)

    @Transactional
    fun moveCategory(currentUserEmail: String, id: Long, newParentId: Long?): CategoryDto {
        val user = userRepository.findByEmail(currentUserEmail)
            ?: throw AccessDeniedException("Forbidden")

        val category = categoryRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Category not found") }

        val newParent = newParentId?.let {
            categoryRepository.findById(it)
                .orElseThrow { IllegalArgumentException("New parent not found") }
        }

        val role = user.role.title
        if (role != "ADMIN" && role != "WRITER") {
            throw AccessDeniedException("Forbidden")
        }

        if (newParentId != null && id == newParentId) {
            throw IllegalArgumentException("Cannot set parent to self")
        }
        if (newParentId != null && categoryRepository.isDescendant(id, newParentId)) {
            throw IllegalArgumentException("Cannot move into own subtree")
        }

        category.parent = newParent
        val saved = categoryRepository.save(category)
        return categoryMapper.toDto(saved)
    }

    // Нефильтрованная версия оставлена для админских сценариев
    @Transactional(readOnly = true)
    fun getChildCategories(categoryId: Long): List<CategoryDto> {
        return categoryRepository.findAllByParentIdAndIsDeleteFalse(categoryId)
            .map { categoryMapper.toDto(it) }
    }

    // Дочерние категории с учётом ролей текущего пользователя (email из Authentication)
    @Transactional(readOnly = true)
    fun getChildCategoriesForUserEmail(categoryId: Long, email: String): List<CategoryDto> {
        val user = userRepository.findByEmail(email) ?: return emptyList()
        val titles = user.accessRoles.map { it.title }.toSet()
        if ("FULL_ACCESS" in titles) {
            return getChildCategories(categoryId)
        }
        val userRoleIds = user.accessRoles.map { it.id }.toSet()
        // Важно: показываем только тех "детей", у которых есть пересечение ролей
        val children = categoryRepository.findAllByParentIdAndIsDeleteFalse(categoryId)
        val filtered = children.filter { child ->
            child.accessRoles.any { it.id in userRoleIds }
        }
        return filtered.map { categoryMapper.toDto(it) }
    }

    @Transactional(readOnly = true)
    fun getArticlesInCategory(categoryId: Long): List<ArticleDto> {
        val category = categoryRepository.findById(categoryId).orElse(null) ?: return emptyList()
        val articles = articleRepository.findAllByCategoryAndIsDeleteFalse(category)
        return articles.map { articleMapper.toDto(it) }
    }

    // Статьи с учётом ролей текущего пользователя (email из Authentication)
    @Transactional(readOnly = true)
    fun getArticlesInCategoryForUserEmail(categoryId: Long, email: String): List<ArticleDto> {
        val user = userRepository.findByEmail(email) ?: return emptyList()
        val titles = user.accessRoles.map { it.title }.toSet()
        if ("FULL_ACCESS" in titles) {
            return getArticlesInCategory(categoryId)
        }
        val category = categoryRepository.findById(categoryId).orElse(null) ?: return emptyList()
        val userRoleIds = user.accessRoles.map { it.id }.toSet()
        val hasAccessToCategory = category.accessRoles.any { it.id in userRoleIds }
        if (!hasAccessToCategory) return emptyList()
        val articles = articleRepository.findAllByCategoryAndIsDeleteFalse(category)
        return articles.map { articleMapper.toDto(it) }
    }

    @Transactional(readOnly = true)
    fun getCategoryContent(categoryId: Long): CategoryContentDto {
        val cats = getChildCategories(categoryId)
        val arts = getArticlesInCategory(categoryId)
        return CategoryContentDto(categories = cats, articles = arts)
    }

    // Контент категории с учётом ролей текущего пользователя (email из Authentication)
    @Transactional(readOnly = true)
    fun getCategoryContentForUserEmail(categoryId: Long, email: String): CategoryContentDto {
        val cats = getChildCategoriesForUserEmail(categoryId, email)
        val arts = getArticlesInCategoryForUserEmail(categoryId, email)
        return CategoryContentDto(categories = cats, articles = arts)
    }

    // Полное дерево без фильтра (для админских задач)
    @Transactional(readOnly = true)
    fun getCategoryTree(): List<CategoryTreeDto> {
        val categories = categoryRepository.findAllByIsDeleteFalse()
        val byParent = categories.groupBy { it.parent?.id }
        fun build(node: Category): CategoryTreeDto {
            val children = byParent[node.id].orEmpty().map { build(it) }
            return CategoryTreeDto(
                id = node.id,
                description = node.description,
                iconPath = node.iconPath,
                isDelete = node.isDelete,
                parentId = node.parent?.id,
                children = children
            )
        }
        val roots = byParent[null].orEmpty()
        return roots.map { build(it) }
    }

    // Дерево категорий с фильтрацией по ролям (email из Authentication)
    @Transactional(readOnly = true)
    fun getCategoryTreeForUser(email: String): List<CategoryTreeDto> {
        val user = userRepository.findByEmail(email) ?: return emptyList()
        val titles = user.accessRoles.map { it.title }.toSet()
        if ("FULL_ACCESS" in titles) {
            return getCategoryTree()
        }
        val userRoleIds = user.accessRoles.map { it.id }.toSet()
        val all = categoryRepository.findAllByIsDeleteFalse()

        // Сначала оставляем только доступные узлы
        val allowed = all.filter { cat -> cat.accessRoles.any { it.id in userRoleIds } }
        val byParentAllowed = allowed.groupBy { it.parent?.id }
        val roots = allowed.filter { it.parent == null }

        fun buildAllowed(node: Category): CategoryTreeDto {
            val children = byParentAllowed[node.id].orEmpty().map { buildAllowed(it) }
            return CategoryTreeDto(
                id = node.id,
                description = node.description,
                iconPath = node.iconPath,
                isDelete = node.isDelete,
                parentId = node.parent?.id,
                children = children
            )
        }
        return roots.map { buildAllowed(it) }
    }

    @Transactional(readOnly = true)
    fun getEditableCategoriesForWriter(writerId: Long): List<CategoryDto> {
        val perms = writerPermissionRepository.findAllByWriterIdAndEnabledTrue(writerId)
        if (perms.isEmpty()) return emptyList()
        val allowedRoleIds = perms.map { it.accessRole.id }.toSet()
        val categories = categoryRepository.findAllByIsDeleteFalse()
            .filter { cat -> cat.accessRoles.any { it.id in allowedRoleIds } }
        return categories.map { categoryMapper.toDto(it) }
    }

    private fun generateUniqueDescription(base: String, existing: Set<String>): String {
        if (base !in existing) return base

        val prefix = "${base}_"
        var max = 1

        existing.forEach { name ->
            if (name.startsWith(prefix)) {
                val suffix = name.removePrefix(prefix)
                val n = suffix.toIntOrNull()
                if (n != null && n >= 2 && n > max) {
                    max = n
                }
            }
        }
        return prefix + (max + 1)
    }

    @Transactional(readOnly = true)
    fun getEditableCategoriesForModerator(moderatorId: Long): List<CategoryDto> {
        val perms = moderatorPermissionRepository.findAllByModeratorIdAndEnabledTrue(moderatorId)
        if (perms.isEmpty()) return emptyList()

        val allowedRoleIds = perms.map { it.accessRole.id }.toSet()
        val categories = categoryRepository.findAllByIsDeleteFalse()
            .filter { cat -> cat.accessRoles.any { it.id in allowedRoleIds } }

        return categories.map { categoryMapper.toDto(it) }
    }


}