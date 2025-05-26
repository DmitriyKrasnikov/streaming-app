
* # Основные классы и их роль

| Класс                           | Описание                                                                                                                                     |
| ------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| **`KafkaStreamsConfig`**        | Настройка потоков: Serde для JSON, топологии GlobalKTable + KStream, правила обработки. Собирает поток: читает `messages`, фильтрует через`BlockListService`, цензурит через `CensorshipService`, записывает в `filtered_messages`.                                                     |
| **`BlockListService`**          | Хранит в state-store список блокировок, фильтрует сообщения по заблокированным парам, публикует события.                                     |
| **`CensorshipService`**         | Управляет набором запрещённых слов (локально + через Kafka), маскирует их в тексте сообщений.                                                |
| **`StreamingDemoRunner`**       | Автоматически демонстрирует сценарии: базовая отправка, блокировка, цензура; проверяет результат.                                            |
| **`ChatMessage`** (модель)      | DTO входа/выхода: отправитель, получатель, текст, время.                                                                                     |
| **`BlockedUserEvent`** (модель) | DTO события блокировки: кто заблокировал, кого, время.                                                                                       |


* # Шаги для запуска Streaming App и просмотра тестового сценария

1. **Сборка приложения либо через терминал, либо средствами IDE**

   ```bash
   mvn clean package
   ```

2. **Запуск Kafka-кластера**

   ```bash
   docker-compose -f kafka-stack.yml up -d
   ```

3. **Проверка Kafka UI**
   Откройте в браузере:
   [http://localhost:8080](http://localhost:8080)
   Убедитесь, что кластер развёрнут, брокеры подняты.

4. **Запуск приложения**

   ```bash
   docker-compose -f app-stack.yml up -d streaming-app
   ```

5. **Инициализация списка запрещённых слов**

    * При старте приложение автоматически публикует слова из `application.initial-forbidden-words` в топик `forbidden_words`.

6. **Автоматический тестовый сценарий**
   Через \~30 секунд после старта `StreamingDemoRunner` прогонит три проверки:

    1. **Базовый сценарий:** отправка `"Hello World!"` → попадание в `filtered_messages`.
    2. **Проверка блокировки:**

        * публикуется `BlockedUserEvent(user2→user1)`
        * отправляется `"Try to block me"` от `user1→user2` → **не должно** попасть в `filtered_messages`.
    3. **Проверка цензуры:**

        * добавляется слово `secret`
        * отправляется сообщение с `secret` → в `filtered_messages` появляется `This is ****** information`.

   Логи проверяйте так:

   ```bash
   docker logs -f streaming-app-streaming-app-1
   ```

7. **Проверьте содержимое топика `filtered_messages` через UI или с помощью ручной верификации через консоль**

   ```bash
   docker exec -it kafka-1-1 \
    kafka-console-consumer \
    --bootstrap-server kafka-1:9092 \
    --topic filtered_messages \
    --from-beginning
   ```

   В итоговом `filtered_messages` должны быть:

    1. **"Hello World!"**
    2. *(нет "Try to block me")*
    3. **"This is \*\*\*\*\*\* information"**
