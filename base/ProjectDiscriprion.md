***

# Полное описание логики работы бэкенда Knowledge Base

## Общая архитектура

Spring Boot 3.2.0 приложение на Kotlin 2.1.0 (Java 17) для корпоративной базы знаний с модульной архитектурой:

### Технологический стек
- **Backend**: Spring Boot 3.2.0, Kotlin 2.1.0, Java 17
- **БД**: PostgreSQL с pgvector, MongoDB (чат-сообщения)
- **Аутентификация**: JWT + LDAP (Active Directory)
- **AI**: Spring AI + Ollama (chat, embeddings)
- **Поиск**: Apache Lucene (BM25) + pgvector (семантический)
- **Другое**: WebSocket (STOMP), iText7 (PDF), ModelMapper

### Структура проекта
- **Пакеты**: `com.knowledge.base` → config, controller, dto, mapper, model, repository, service, util
- **Слои**: Repository (JPA/Mongo) → Service → Controller + Mapper
- **Главный класс**: `BaseApplication.kt` с `@SpringBootApplication`, `@EnableScheduling`, `@EnableAsync`
- **Конфигурация**: `application.properties` (профили dev/prod)

***

## 1. Базы данных и хранилища

### 1.1. PostgreSQL (основная БД: `knowledge`)

**Сущности JPA**:
- `User`: id, email, password(BCrypt), roles, isFromLdap, isDelete(soft)
- `Role`: ADMIN, MODERATOR, WRITER, USER
- `AccessRole`: кастомные роли доступа к категориям (USER, GUEST, FULL_ACCESS и др.)
- `Article`: id, title, content(Quill Delta JSON), categoryId, authorId, videoPath, filePath, isDelete
- `ArticleVersion`: снимки версий для истории изменений
- `ArticleProposal`: заявки на модерацию (создание/обновление)
- `Category`: древовидная структура (parent-child), связи с AccessRole
- `WriterPermission`: права WRITER по категориям
- `Notification`: типы уведомлений, получатели
- `ArticleViewHit`: запись просмотров по JTI токена
- `Favorite`: избранные статьи пользователей
- `Feedback`: обратная связь
- `RefreshToken`: хранение refresh токенов (хеш SHA-256)
- `Statistics`: счетчики статей/категорий, частота просмотров

**Vector Store (pgvector)**:
- **Dev**: HNSW индекс, **768 измерений** (nomic-embed-text)
- **Prod**: HNSW индекс, **1024 измерения** (bge-m3)
- Индексирование: COSINE distance для семантического поиска

### 1.2. MongoDB (БД: `chatdb`)
- `ChatMessage`: хранение сообщений чата с AI
- Структура: sessionId, userId, message, timestamp, role(user/assistant)

### 1.3. Файловое хранилище
**Структура**: `./uploads/{files,images,videos}`

**Лимиты**:
- Изображения: до 10MB (jpg, png, gif, webp)
- Видео: до 100MB (mp4, avi, mov, wmv)
- Документы: до 50MB (pdf, doc, docx, txt, xls, xlsx, ppt, pptx и др.)

**Именование**:
- Изображения/видео: UUID + оригинальное имя
- Документы: оригинальное имя
- Автоисправление абсолютных путей → относительные при старте

***

## 2. Аутентификация и авторизация

### 2.1. Аутентификация

**Dual-mode провайдеры**:
1. **LDAP** (основной)
    - **Dev**: Embedded LDAP (LDIF файл)
    - **Prod**: Active Directory (`DC=llc,DC=tagras,DC=corp`, LDAPS:636)
2. **DAO** (локальная БД)
    - Для локальных пользователей (например, `admin@gmail.com`)
    - Пароли в BCrypt

**`CustomAuthenticationManager`**:
- Локальные пользователи → только DAO
- Остальные → LDAP, в dev fallback на DAO для email-логинов
- Нормализация логина: `DOMAIN\user` → `user`, `user@domain.com` → `user`
- LDAP пользователи создаются автоматически при первом входе (shadow users, `isFromLdap = true`)
- Автоматическое добавление базовых AccessRole (USER, GUEST) для LDAP-пользователей

### 2.2. JWT

**Access Token**:
- Время жизни: 1 час (по умолчанию)
- Payload: username, roles, jti(JWT ID), rtf(refresh token family)
- Используется для аутентификации запросов

**Refresh Token**:
- Время жизни: 30 дней
- Хранение: в БД хешированным (SHA-256)
- Ротация: при каждом обновлении создается новый refresh token
- Семейства токенов: защита от replay-атак

**`JwtAuthenticationFilter`**:
- Извлечение токена из заголовка `Authorization: Bearer <token>`
- Валидация токена
- Установка `SecurityContext` для Spring Security

### 2.3. Роли и права доступа

**Системные роли (Role)**:
- `ADMIN`: полный доступ ко всем операциям
- `MODERATOR`: модерация заявок, управление статьями
- `WRITER`: создание заявок на модерацию, ограниченный доступ
- `USER`: базовый доступ на чтение

**AccessRole** (роли доступа к категориям):
- Кастомные роли: USER, GUEST, FULL_ACCESS и другие
- Связь many-to-many с категориями
- FULL_ACCESS видит все категории без ограничений

**WriterPermission**:
- Ограничения для WRITER по конкретным категориям
- Управление через `/api/writer-permissions` (ADMIN)
- Проверка прав через `WriterPermissionService`

***

## 3. Управление пользователями

### 3.1. Типы пользователей

**Локальные**:
- Создаются в БД напрямую
- Пароль хранится в BCrypt
- `isFromLdap = false`

**LDAP (shadow users)**:
- Создаются автоматически при первой аутентификации
- Без пароля в БД
- `isFromLdap = true`
- Синхронизация через `DatabaseLdapAuthoritiesPopulator`

### 3.2. Основные операции

**CRUD** (`UserService`):
- Создание/обновление/восстановление (soft delete)
- Фильтрация: по email, фамилии, `isFromLdap`, `isDelete`
- Управление ролями и правами доступа

**API эндпоинты** (`UserController`, `AuthController`):
- `POST /api/user/login` — аутентификация, получение JWT
- `POST /api/auth/refresh` — обновление access token
- `POST /api/auth/logout` — invalidation refresh token
- `GET /api/user/me` — профиль текущего пользователя
- `GET /api/ldap/users` — синхронизация пользователей из LDAP
- `GET/POST/PUT/DELETE /api/user/**` — управление (ADMIN only)

***

## 4. Управление категориями

### 4.1. Иерархическая структура

**Древовидная модель**:
- Parent-child связи (`parentId`)
- Защита от циклов при перемещении
- Рекурсивная загрузка дерева категорий

### 4.2. Система доступа

**Связь с AccessRole**:
- Many-to-many связь категорий с AccessRole
- Фильтрация категорий по ролям пользователя
- FULL_ACCESS видит все категории
- GUEST имеет ограниченный доступ к публичным категориям

### 4.3. Операции

**CRUD** (`CategoryService`):
- Создание/обновление/soft delete/перемещение
- Автогенерация уникальных описаний при дубликатах
- Управление правами доступа к категориям

**API эндпоинты** (`CategoryController`):
- `GET /api/category/tree` — полное дерево категорий
- `GET /api/category/{id}/content` — содержимое категории (статьи + подкатегории)
- `GET /api/category/{id}/children` — дочерние категории
- `GET /api/category/{id}/articles` — статьи категории
- `GET /api/category/all` — все категории (плоский список)
- `GET /api/category/search-admin` — поиск для администраторов
- `POST/PUT/DELETE /api/category/**` — управление (ADMIN/MODERATOR/WRITER)
- `PUT /api/category/{id}/move` — перемещение в другую категорию
- `PUT /api/category/{id}/soft-delete` — мягкое удаление

***

## 5. Управление статьями

### 5.1. Модель данных

**Article**:
- `title`: заголовок статьи
- `content`: Quill Delta JSON (пример: `{"ops": [{"insert": "текст <b>жирный</b> "}]}`)[1][2]
- `categoryId`: связь с категорией
- `videoPath`: путь к видео (опционально)
- `filePath`: путь к прикрепленным файлам
- `isDelete`: флаг мягкого удаления

**Версионирование**:
- `ArticleVersion`: снимки содержимого при каждом изменении
- Поля: content, timestamp, authorId, comment
- Сравнение версий: JSON Patch и текстовые дельты (zjsonpatch, java-diff-utils)
- Восстановление: откат к любой версии (создается новая версия)

### 5.2. Права доступа

**Просмотр**:
- По AccessRole категории
- Публичный доступ: `/api/guest/articles/{id}` (не удаленные)
- Привилегированные роли видят удаленные статьи

**Создание/обновление**:
- ADMIN, MODERATOR: напрямую без модерации
- WRITER: через систему модерации (`ArticleProposal`)

**Удаление**:
- Soft delete: ADMIN, MODERATOR, WRITER
- Hard delete: только ADMIN

### 5.3. Версионирование

**Автоматические снимки**:
- При создании статьи
- При каждом обновлении
- При soft delete
- При восстановлении из удаленных

**Операции** (`ArticleVersionService`):
- Сравнение версий: diff между двумя версиями
- Восстановление версии: создание новой версии с содержимым из старой
- Удаление версий: только ADMIN
- Получение автора версии

**API эндпоинты** (`ArticleVersionController`):
- `GET /api/articles/{articleId}/versions` — список версий
- `GET /api/articles/{articleId}/versions/{version}` — конкретная версия
- `GET /api/articles/{articleId}/versions/compare?v1={v}&v2={v}` — сравнение
- `GET /api/articles/{articleId}/versions/{version}/author` — автор версии
- `POST /api/articles/{articleId}/versions/restore?version={v}` — восстановление
- `DELETE /api/articles/{articleId}/versions/{version}` — удаление версии (ADMIN)

### 5.4. Система модерации

**ArticleProposal**:
- Заявки на создание/обновление статей от WRITER
- Статусы: PENDING, APPROVED, REJECTED
- Хранение: предложенное содержимое, комментарий модератора

**Workflow**:
1. WRITER создает заявку: `POST /api/moderation/submit/create` или `/update/{id}`
2. ADMIN/MODERATOR просматривает: `GET /api/moderation/pending`
3. Одобрение/отклонение: `POST /api/moderation/approve` или `/reject`
4. При одобрении: создается/обновляется статья + создается версия
5. Уведомление WRITER о результате

**API эндпоинты** (`ModerationController`):
- `POST /api/moderation/submit/create` — заявка на создание
- `POST /api/moderation/submit/update/{id}` — заявка на обновление
- `GET /api/moderation/pending` — список ожидающих заявок
- `GET /api/moderation/proposals/{id}` — детали заявки
- `POST /api/moderation/approve` — одобрение заявки
- `POST /api/moderation/reject` — отклонение заявки

***

## 6. Поиск и индексация

### 6.1. Гибридный поиск

**Lucene (BM25)** (`LuceneService`):
- Полнотекстовый поиск по `title` и `body`
- Индекс: файловая система
- Токенизация, стемминг, стоп-слова

**Vector Store (pgvector)**:
- Семантический поиск через embeddings
- **Dev**: nomic-embed-text (768 dim, temperature=0.7)
- **Prod**: bge-m3 (1024 dim, temperature=0.3)
- HNSW индекс с COSINE distance

**Объединение результатов**:
- Реранкинг по relevance score
- Комбинация keyword-based (Lucene) + semantic (vector)
- Дедупликация результатов

### 6.2. Индексация

**Полная индексация** (`IndexingService`):
- По расписанию: каждую пятницу 23:00 МСК
- Пересоздание всех индексов
- Асинхронная обработка (`@Async`)

**Инкрементальная индексация**:
- Триггеры: создание/обновление/удаление статьи
- Автоматическое обновление индексов
- Чанкинг текста: 900 символов, overlap 150

**Чанкинг** (`ChatService`):
- Разбиение длинных текстов на чанки
- Размер чанка: 900 символов
- Overlap: 150 символов
- Эмбеддинг каждого чанка отдельно

### 6.3. Query Rewriter

**Функции**:
- Переписывание запросов для улучшения релевантности
- Извлечение ключевых фраз
- Expansion запросов (синонимы, связанные термины)

***

## 7. Чат с AI (Ollama)

### 7.1. WebSocket

**Конфигурация** (`WebSocketConfig`):
- STOMP через SockJS: endpoint `/ws/chat`
- Message broker: `/topic` (публикация ответов)
- Application destination: `/app/chat` (отправка сообщений)
- JWT аутентификация: `JwtChannelInterceptor`

**Подключение**:
1. Клиент подключается к `/ws/chat`
2. JWT токен передается через `Authorization` header
3. Подписка на `/topic/chat/{sessionId}`
4. Отправка сообщений в `/app/chat`

### 7.2. Генерация ответов

**OllamaService**:
- **Dev**: llama3 (chat), nomic-embed-text (embeddings, 768 dim)
- **Prod**: qwen2.5:14b (chat), bge-m3 (embeddings, 1024 dim)
- **Temperature**: 0.7 (dev), 0.3 (prod)
- Spring AI интеграция: `OllamaChatModel`, `OllamaEmbeddingModel`

**ChatService workflow**:
1. Получение сообщения от пользователя
2. Гибридный поиск по статьям (контекст для RAG)
3. Формирование промпта с контекстом
4. Отправка в Ollama
5. Форматирование ответа (Markdown, ссылки на статьи)
6. Сохранение в MongoDB
7. Отправка через WebSocket

**Форматирование ответов**:
- Markdown разметка
- Ссылки на релевантные статьи: `[Название статьи](/articles/{id})`
- Code blocks с подсветкой синтаксиса

### 7.3. Хранение

**MongoDB** (`ChatMessage`):
- Поля: sessionId, userId, message, timestamp, role(user/assistant)
- Индексы: sessionId, userId, timestamp
- TTL для старых сообщений (опционально)

**API эндпоинты** (`ChatController`, `ChatRestController`):
- `POST /api/chat/session/start` — создание новой сессии
- `POST /api/chat/message/send` — отправка сообщения (REST альтернатива WebSocket)
- `GET /api/chat/session/{sessionId}/messages` — история чата
- WebSocket: `/ws/chat` + STOMP `/app/chat` → `/topic/chat/{sessionId}`

***

## 8. Уведомления

### 8.1. Типы уведомлений

**NotificationType**:
- `NEW_ARTICLE`: новая статья опубликована
- `ARTICLE_UPDATED`: статья обновлена
- `PROPOSAL_APPROVED`: заявка на модерацию одобрена
- `PROPOSAL_REJECTED`: заявка на модерацию отклонена
- `CUSTOM_MESSAGE`: ручная рассылка от администратора

### 8.2. Система доставки

**Автоматические уведомления**:
- Получатели: пользователи с доступом к категории статьи (по AccessRole)
- Триггеры: публикация/обновление статьи, модерация заявок
- Асинхронная отправка (`@Async`)

**Ручная рассылка** (ADMIN):
- По ролям: отправка всем пользователям с определенной ролью
- По AccessRole: пользователям с определенной ролью доступа
- Конкретным пользователям: по списку ID

**API эндпоинты** (`NotificationController`):
- `GET /api/notifications` — список уведомлений текущего пользователя
- `POST /api/notifications/send` — ручная рассылка (ADMIN)
- `PUT /api/notifications/{id}/read` — пометка как прочитанное
- `DELETE /api/notifications/{id}` — удаление уведомления

***

## 9. Статистика и аналитика

### 9.1. Просмотры статей

**ArticleViewHit**:
- Запись просмотров по JTI токена (дедупликация)
- Один пользователь = один просмотр на статью
- Поля: articleId, userId, jti(JWT ID), timestamp

**Метрики**:
- Общее количество просмотров
- Просмотры за последние 24 часа
- Уникальные пользователи

**Учет просмотров**:
- Interceptor: `ArticleViewInterceptor`
- Триггер: GET запрос на `/api/articles/{id}`
- Инкремент счетчика: `ArticleService.incrementViews()`

### 9.2. Статистика

**Statistics** (сущность):
- Счетчики: общее количество статей, категорий, пользователей
- Частота просмотров: топ статей по просмотрам
- Активность: статистика по датам

**API эндпоинты** (`StatsController`, ADMIN only):
- `GET /api/stats/counters` — общие счетчики
- `GET /api/stats/frequency` — частота просмотров
- `GET /api/stats/top-articles` — топ популярных статей
- `GET /api/stats/activity` — статистика активности

***

## 10. PDF-генерация

**PDFService** (iText7 + HTML2PDF):
- Генерация PDF из статей
- Конвертация Quill Delta JSON → HTML → PDF
- Поддержка форматирования: жирный, курсив, списки, заголовки

**Доступ**:
- Публичный endpoint: `/api/articles/{id}/pdf`
- Не удаленные статьи: доступны всем
- Удаленные статьи: только ADMIN, MODERATOR

**Функции**:
- Автоматическая генерация обложки
- Оглавление (если есть заголовки)
- Метаданные: автор, дата создания, категория

***

## 11. Безопасность

### 11.1. CORS

**Конфигурация** (`SecurityConfig`):
- Разрешенные origins: `localhost:4200`, `pro-znania:4200`, `pro-znania.llc.tagras.corp:4200`
- Credentials: разрешены (cookies, authorization headers)
- Методы: GET, POST, PUT, DELETE, PATCH, OPTIONS
- Headers: `Authorization`, `Content-Type`, `X-Requested-With`

### 11.2. Защита маршрутов

**Публичные** (permitAll):
- `/api/guest/**` — публичный доступ к статьям/категориям
- `/api/files/**`, `/images/**`, `/videos/**` — статические файлы
- `/api/articles/{id}/pdf` — генерация PDF
- `/api/user/login`, `/api/auth/refresh` — аутентификация

**Аутентифицированные** (authenticated):
- Большинство API: требуется валидный JWT токен
- Доступ к персональным данным: `/api/user/me`

**Ролевые** (hasRole):
- `ADMIN`: полный доступ, управление пользователями, статистика
- `MODERATOR`: модерация заявок, управление статьями
- `WRITER`: создание заявок, ограниченное редактирование
- `USER`: базовый доступ на чтение

### 11.3. JWT Filter

**`JwtAuthenticationFilter`**:
- Извлечение токена из `Authorization: Bearer <token>`
- Валидация: подпись, expiration, формат
- Установка `SecurityContext` для Spring Security
- Обработка ошибок: 401 при невалидном токене

**Защита от атак**:
- CSRF: отключен (stateless API)
- XSS: санитизация HTML в Quill Delta
- SQL Injection: параметризованные запросы JPA
- JWT replay: refresh token families, jti tracking

***

## 12. Инициализация данных

### 12.1. При старте приложения

**`AccessRoleStartupInitializer`**:
- Создание базовых AccessRole: USER, GUEST, FULL_ACCESS
- Проверка существования перед созданием
- Сохранение в БД

**`DataInitializer`** (опционально):
- Начальные данные: демо-статьи, категории
- Тестовые пользователи (только dev profile)
- Seed данные для разработки

**Проверка Ollama**:
- Доступность сервера: `http://localhost:11434` (dev), prod URL (prod)
- Наличие моделей: llama3/qwen2.5, nomic-embed-text/bge-m3
- Логирование ошибок при недоступности

***

## 13. Конфигурация

### 13.1. Профили (application.properties)

**Dev**:
- Embedded LDAP: LDIF файл
- Localhost БД: Postgres:5132, Mongo:27017
- Ollama: localhost:11434
- Модели: llama3 (chat), nomic-embed-text (embeddings, 768 dim)
- Temperature: 0.7
- Debug логирование

**Prod**:
- External LDAP: Active Directory (DC=llc,DC=tagras,DC=corp), LDAPS:636
- Продакшн БД: env vars (`DB_USER`, `DB_PASSWORD`, `DB_HOST`)
- Ollama: продакшн URL
- Модели: qwen2.5:14b (chat), bge-m3 (embeddings, 1024 dim)
- Temperature: 0.3
- JWT Secret: env var (`JWT_SECRET`)
- Error logging only

### 13.2. Базы данных

**PostgreSQL**:
- БД: `knowledge`
- Extensions: pgvector (для embeddings)
- Connection pool: HikariCP (default)
- JPA properties: `hibernate.ddl-auto=update` (dev), `validate` (prod)

**MongoDB**:
- БД: `chatdb`
- Коллекция: `chat_messages`
- Connection: `mongodb://localhost:27017/chatdb`

***

## 14. Особенности реализации

### 14.1. Soft Delete

**Статьи и категории**:
- Флаг `isDelete: Boolean`
- Фильтрация в запросах: `WHERE isDelete = false`
- Восстановление: `isDelete = false`

**Пользователи**:
- Флаг `isDelete: Boolean`
- Восстановление: сброс флага + генерация нового пароля (для локальных)
- LDAP пользователи: восстановление без пароля

### 14.2. Транзакции

**Критичные операции** (`@Transactional`):
- Создание/обновление статей с версионированием
- Модерация заявок
- Управление пользователями и правами
- Batch операции

**Асинхронные задачи** (`@Async`):
- Индексация статей
- Отправка уведомлений
- Генерация embeddings
- Обработка больших файлов

### 14.3. Маппинг

**ModelMapper**:
- Entity ↔ DTO преобразования
- Автоматический маппинг полей
- Custom mappers: для сложных преобразований (например, Quill Delta JSON)

**Кастомные мапперы**:
- `ArticleMapper`: Article ↔ ArticleDTO (с версиями)
- `CategoryMapper`: Category ↔ CategoryTreeDto (рекурсивное дерево)
- `UserMapper`: User ↔ UserDTO (без пароля)

***

## 15. API Endpoints (сводка)

### Auth/Users (`/api/user`, `/api/auth`)
- `POST /api/user/login` — аутентификация
- `POST /api/auth/refresh` — обновление токена
- `POST /api/auth/logout` — выход
- `GET /api/user/me` — текущий пользователь
- `GET /api/ldap/users` — синхронизация LDAP
- `GET/POST/PUT/DELETE /api/user/**` — CRUD пользователей (ADMIN)

### Articles (`/api/articles`)
- `POST /api/articles` — создание (ADMIN/MODERATOR)
- `PUT /api/articles/{id}` — обновление (ADMIN/MODERATOR)
- `GET /api/articles/{id}` — получение статьи
- `GET /api/articles/all` — список всех статей (с ролями)
- `GET /api/articles/search?q={query}` — поиск
- `GET /api/articles/by-category/{categoryId}` — статьи категории
- `POST /api/articles/upload-image` — загрузка изображений
- `GET /api/articles/{id}/pdf` — генерация PDF
- `POST /api/articles/{id}/views` — инкремент просмотров
- `DELETE /api/articles/delete/{id}` — hard delete (ADMIN)
- `PATCH /api/articles/{id}/soft-delete` — soft delete
- `POST /api/articles/admin/fix-image-urls` — исправление URL изображений

### Versions (`/api/articles/{articleId}/versions`)
- `GET /versions` — список версий
- `GET /{version}` — конкретная версия
- `GET /compare?v1={v}&v2={v}` — сравнение версий
- `GET /{version}/author` — автор версии
- `POST /restore?version={v}` — восстановление версии
- `DELETE /{version}` — удаление версии (ADMIN)

### Moderation (`/api/moderation`)
- `POST /submit/create` — заявка на создание (WRITER)
- `POST /submit/update/{id}` — заявка на обновление (WRITER)
- `GET /pending` — ожидающие заявки (MODERATOR)
- `GET /proposals/{id}` — детали заявки
- `POST /approve` — одобрение (MODERATOR)
- `POST /reject` — отклонение (MODERATOR)

### Categories (`/api/category`)
- `GET /tree` — дерево категорий
- `GET /{id}/content` — содержимое категории
- `GET /{id}/children` — дочерние категории
- `GET /{id}/articles` — статьи категории
- `GET /all` — все категории (плоский список)
- `GET /search-admin` — поиск (ADMIN)
- `POST /api/category` — создание
- `PUT /api/category/{id}` — обновление
- `DELETE /api/category/{id}` — удаление
- `PUT /{id}/move` — перемещение
- `PUT /{id}/soft-delete` — мягкое удаление

### Chat (`/api/chat`, WebSocket `/ws/chat`)
- `POST /session/start` — создание сессии
- `POST /message/send` — отправка сообщения (REST)
- `GET /session/{sessionId}/messages` — история
- WebSocket: `/ws/chat` (STOMP `/app/chat` → `/topic/chat/{sessionId}`)

### Permissions (`/api/writer-permissions`)
- `POST /grant` — выдача прав (ADMIN)
- `DELETE /revoke` — отзыв прав (ADMIN)
- `GET /me/can-edit?categoryId={id}` — проверка прав
- `GET /me/categories-editable` — категории для редактирования

### Other
- `GET/POST /api/stats/**` — статистика (ADMIN)
- `GET/POST /api/feedback/**` — обратная связь
- `GET/POST/PUT/DELETE /api/notifications/**` — уведомления
- `GET /api/guest/articles/{id}` — публичный доступ
- `GET /api/guest/categories` — публичные категории
- `GET /api/files/**`, `/images/**`, `/videos/**` — статические файлы

***

## 16. Развертывание

### 16.1. Docker

**Dockerfile** (multi-stage build):
1. Build stage: Gradle + Kotlin → JAR
2. Runtime stage: Temurin 17 Alpine
3. Профиль: prod (env vars)
4. Порт: 8080

**docker-compose.yml**:
- `postgres`: pgvector:pg17, порт 5132, volume для данных
- `ollama`: кастомный образ (ollama-init), pull llama3 + nomic-embed-text, порт 11434
- `mongo`: latest, порт 27017, volume для данных
- `app`: Spring Boot, зависит от postgres/ollama/mongo

**ollama-init**:
- Dockerfile: официальный ollama image
- Скрипт: `ollama serve &` + `ollama pull llama3` + `ollama pull nomic-embed-text`
- Для prod: `qwen2.5:14b` + `bge-m3`

### 16.2. Переменные окружения (prod)

- `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD` — PostgreSQL
- `MONGO_HOST`, `MONGO_PORT` — MongoDB
- `OLLAMA_URL` — Ollama API URL
- `JWT_SECRET` — секрет для подписи JWT
- `LDAP_URL`, `LDAP_BASE` — LDAP конфигурация
- `SPRING_PROFILES_ACTIVE=prod` — профиль

***

## Заключение

**Knowledge Base** — корпоративная система управления знаниями с:

- **Гибридным поиском**: Lucene (keyword) + pgvector (semantic)
- **Версионированием статей**: полная история изменений, сравнение, откат
- **Системой модерации**: контроль качества контента через WRITER → MODERATOR workflow
- **AI-чатом**: Ollama (llama3/qwen2.5) с RAG для контекстных ответов
- **Гибкой системой прав**: роли (ADMIN/MODERATOR/WRITER/USER) + AccessRole по категориям
- **Интеграцией с LDAP**: автоматическая синхронизация пользователей из Active Directory
- **Уведомлениями**: автоматические и ручные рассылки
- **Аналитикой**: статистика просмотров, популярность статей

**Архитектура**: модульная, с четким разделением ответственности между слоями (Controller → Service → Repository). Профили dev/prod для разных сред. Асинхронная обработка тяжелых задач. Транзакционность критичных операций.
