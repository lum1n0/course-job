#!/bin/bash
set -e

# Запускаем основной процесс Ollama в фоновом режиме
/bin/ollama serve &
pid=$!

# Ждем, пока сервер запустится и будет готов принимать запросы
echo "Waiting for Ollama server to start..."
sleep 5

# Скачиваем модели, если они еще не скачаны
echo "Pulling models..."
ollama pull llama3
ollama pull nomic-embed-text
echo "Models are ready."

# Выводим фоновый процесс на передний план, чтобы контейнер не завершился
wait $pid
