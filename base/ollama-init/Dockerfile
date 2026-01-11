# Используем официальный образ Ollama как основу
FROM ollama/ollama

# Копируем наш скрипт внутрь контейнера
COPY entrypoint.sh /entrypoint.sh

# Делаем скрипт исполняемым
RUN chmod +x /entrypoint.sh

# Указываем, что при запуске контейнера нужно выполнить наш скрипт
ENTRYPOINT [ "/entrypoint.sh" ]
