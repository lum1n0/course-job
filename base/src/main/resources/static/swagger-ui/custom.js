// ====================================
// Pro Znania API - Custom Swagger UI JavaScript
// ====================================

window.addEventListener('load', function() {
    const TOKEN_KEY = "pro-znania.jwt";

    // Добавляем кастомный заголовок
    const topbar = document.querySelector('.topbar');
    if (topbar) {
        const customHeader = document.createElement('div');
        customHeader.innerHTML = '<h3 style="color: white; margin: 0; padding: 10px 20px; font-size: 1.2em;">Pro Znania API Documentation</h3>';
        topbar.prepend(customHeader);
        const topbarWrapper = document.querySelector('.topbar-wrapper') || topbar;
        const demoLink = document.createElement('a');
        demoLink.href = '/demo/chat.html';
        demoLink.textContent = 'Демо чата';
        demoLink.className = 'pz-demo-link';
        topbarWrapper.appendChild(demoLink);
    }

    // Анимация для кнопок при клике
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('btn')) {
            e.target.style.transform = 'scale(0.95)';
            setTimeout(() => {
                e.target.style.transform = '';
            }, 150);
        }
    });

    // Добавляем индикатор загрузки
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.addedNodes.length) {
                mutation.addedNodes.forEach(function(node) {
                    if (node.nodeType === 1 && node.classList.contains('opblock')) {
                        // Анимация для новых блоков
                        node.style.opacity = '0';
                        node.style.transform = 'translateY(20px)';
                        setTimeout(() => {
                            node.style.transition = 'all 0.4s ease-out';
                            node.style.opacity = '1';
                            node.style.transform = 'translateY(0)';
                        }, 50);
                    }
                });
            }
        });
    });

    // Наблюдаем за изменениями в DOM
    const targetNode = document.querySelector('.swagger-ui');
    if (targetNode) {
        observer.observe(targetNode, {
            childList: true,
            subtree: true
        });
    }

    // Добавляем эффект ripple для кнопок
    function createRipple(event) {
        const button = event.currentTarget;
        const ripple = document.createElement('span');
        const diameter = Math.max(button.clientWidth, button.clientHeight);
        const radius = diameter / 2;

        ripple.style.width = ripple.style.height = `${diameter}px`;
        ripple.style.left = `${event.clientX - button.offsetLeft - radius}px`;
        ripple.style.top = `${event.clientY - button.offsetTop - radius}px`;
        ripple.classList.add('ripple');

        const rippleElement = button.querySelector('.ripple');
        if (rippleElement) {
            rippleElement.remove();
        }

        button.appendChild(ripple);
    }

    // Применяем ripple эффект ко всем кнопкам
    setTimeout(() => {
        const buttons = document.querySelectorAll('.swagger-ui .btn');
        buttons.forEach(button => {
            button.style.position = 'relative';
            button.style.overflow = 'hidden';
            button.addEventListener('click', createRipple);
        });
    }, 1000);

    // Добавляем CSS для ripple эффекта
    const style = document.createElement('style');
    style.textContent = `
        .ripple {
            position: absolute;
            border-radius: 50%;
            background-color: rgba(255, 255, 255, 0.5);
            transform: scale(0);
            animation: ripple-animation 0.6s ease-out;
            pointer-events: none;
        }

        @keyframes ripple-animation {
            to {
                transform: scale(4);
                opacity: 0;
            }
        }

        /* Smooth scroll */
        html {
            scroll-behavior: smooth;
        }

        /* Highlight active section */
        .swagger-ui .opblock.is-open {
            border-left-width: 6px !important;
            box-shadow: 0 8px 24px rgba(0, 154, 163, 0.2) !important;
        }

        /* Loading animation */
        .swagger-ui .loading-container {
            position: relative;
        }

        .swagger-ui .loading-container::before {
            content: '';
            position: absolute;
            top: 50%;
            left: 50%;
            width: 50px;
            height: 50px;
            margin: -25px 0 0 -25px;
            border: 4px solid #f3f3f3;
            border-top: 4px solid #009AA3;
            border-radius: 50%;
            animation: spin 1s linear infinite;
        }
    `;
    document.head.appendChild(style);

    // Добавляем прогресс-бар для запросов
    const addProgressBar = () => {
        const progressBar = document.createElement('div');
        progressBar.id = 'api-progress-bar';
        progressBar.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            height: 3px;
            background: linear-gradient(90deg, #009AA3, #E4002F);
            width: 0%;
            transition: width 0.3s ease;
            z-index: 9999;
            box-shadow: 0 2px 4px rgba(0, 154, 163, 0.3);
        `;
        document.body.appendChild(progressBar);

        // Слушаем клики на кнопки Execute
        document.addEventListener('click', function(e) {
            if (e.target.classList.contains('execute')) {
                const progress = document.getElementById('api-progress-bar');
                progress.style.width = '0%';
                setTimeout(() => progress.style.width = '30%', 10);
                setTimeout(() => progress.style.width = '60%', 300);
                setTimeout(() => progress.style.width = '100%', 600);
                setTimeout(() => progress.style.width = '0%', 1000);
            }
        });
    };

    setTimeout(addProgressBar, 500);

    // Добавляем счетчик эндпоинтов
    setTimeout(() => {
        const opblocks = document.querySelectorAll('.opblock');
        const info = document.querySelector('.information-container');
        if (info && opblocks.length > 0) {
            const counter = document.createElement('div');
            counter.style.cssText = `
                background: linear-gradient(135deg, #009AA3, #67767D);
                color: white;
                padding: 15px 25px;
                border-radius: 8px;
                margin: 20px 0;
                font-weight: 600;
                box-shadow: 0 4px 12px rgba(0, 154, 163, 0.2);
                animation: fadeInUp 0.6s ease-out;
            `;
            counter.innerHTML = `
                <span style="font-size: 1.2em;">📡</span>
                Доступно эндпоинтов: <strong>${opblocks.length}</strong>
            `;
            info.appendChild(counter);
        }
    }, 1500);

    // Улучшенная навигация
    document.querySelectorAll('.opblock-tag').forEach(tag => {
        tag.style.cursor = 'pointer';
        tag.addEventListener('click', function() {
            this.style.transform = 'translateX(10px)';
            setTimeout(() => {
                this.style.transform = '';
            }, 300);
        });
    });

    console.log('%c Pro Znania API ', 'background: #009AA3; color: white; font-size: 16px; padding: 10px;');
    console.log('%c Документация успешно загружена! ', 'background: #E4002F; color: white; font-size: 12px; padding: 5px;');

    // Синхронизация токена из Swagger UI в localStorage для демо-страницы
    const normalizeToken = (value) => {
        if (!value) return '';
        const trimmed = String(value).trim();
        return trimmed.toLowerCase().startsWith('bearer ')
            ? trimmed.slice(7).trim()
            : trimmed;
    };

    const extractToken = () => {
        if (!window.ui || !window.ui.getState) return '';
        try {
            const state = window.ui.getState();
            const auth = state.get ? state.get('auth') : state.auth;
            const authorized = auth && auth.get ? auth.get('authorized') : auth?.authorized;
            const bearer = authorized && authorized.get ? authorized.get('bearerAuth') : authorized?.bearerAuth;
            const value = bearer && bearer.get ? bearer.get('value') : bearer?.value;
            return normalizeToken(value);
        } catch (e) {
            return '';
        }
    };

    const syncInterval = setInterval(() => {
        const token = extractToken();
        if (token) {
            localStorage.setItem(TOKEN_KEY, token);
        }
    }, 1500);

    window.addEventListener('beforeunload', () => clearInterval(syncInterval));
});
