# 🛒 Shopping List Backend

## 📌 Описание проекта
Серверная часть мобильного приложения для планирования и анализа покупок. Позволяет пользователям создавать списки покупок, управлять ими, анализировать расходы и делиться списками с другими пользователями.

## 🚀 Технологии и стек
- **Язык программирования**: Kotlin
- **Фреймворк**: Ktor
- **База данных**: PostgreSQL
- **ORM**: Exposed
- **Аутентификация**: JWT
- **DI**: Koin
- **WebSockets**: для real-time обновлений
- **Сериализация**: kotlinx.serialization

## 📁 Структура проекта
```
src/main/kotlin/
├── auth/           # Модуль аутентификации и авторизации
├── database/       # Настройки и миграции базы данных
├── di/            # Dependency Injection (Koin)
├── dto/           # Data Transfer Objects
├── models/        # Модели данных
├── routes/        # Маршруты API
├── services/      # Бизнес-логика
├── utils/         # Вспомогательные функции
├── Application.kt # Точка входа приложения
├── Routing.kt     # Настройка маршрутизации
├── Serialization.kt # Настройка сериализации
└── Sockets.kt     # WebSocket обработчики
```

## 🧩 Основные зависимости
- Ktor: 2.3.7
- Exposed: 0.44.1
- PostgreSQL: 42.6.0
- Koin: 3.5.0
- JWT: 9.0.1
- kotlinx.serialization: 1.6.0

## 🔐 Аутентификация
Приложение использует JWT (JSON Web Tokens) для аутентификации. После успешного входа пользователь получает токен, который должен быть включен в заголовок `Authorization` всех последующих запросов. Токен имеет ограниченный срок действия и может быть обновлен.

## 🛠️ Как запустить проект

### Предварительные требования
- JDK 17+
- PostgreSQL 14+
- Gradle 8.0+

### Локальный запуск
1. Клонировать репозиторий
2. Создать базу данных PostgreSQL
3. Настроить переменные окружения:
   ```
   DB_URL=jdbc:postgresql://localhost:5432/your_database
   DB_USER=your_username
   DB_PASSWORD=your_password
   JWT_SECRET=your_secret_key
   ```
4. Запустить приложение:
   ```bash
   ./gradlew run
   ```

## 🗃️ Структура базы данных

### Основные таблицы
| Таблица | Описание |
|---------|----------|
| users | Пользователи системы |
| shopping_lists | Списки покупок |
| items | Элементы списка |
| groups | Группы пользователей |
| user_groups | Связь пользователей с группами |

## 📡 API Endpoints

### Аутентификация
- `POST /auth/register` - Регистрация нового пользователя
- `POST /auth/login` - Вход в систему
- `POST /auth/refresh` - Обновление токена

### Списки покупок
- `GET /lists` - Получение всех списков пользователя
- `POST /lists` - Создание нового списка
- `PUT /lists/{id}` - Обновление списка
- `DELETE /lists/{id}` - Удаление списка

### Элементы списка
- `GET /lists/{id}/items` - Получение элементов списка
- `POST /lists/{id}/items` - Добавление элемента
- `PUT /lists/{id}/items/{itemId}` - Обновление элемента
- `DELETE /lists/{id}/items/{itemId}` - Удаление элемента

### Группы
- `GET /groups` - Получение групп пользователя
- `POST /groups` - Создание новой группы
- `POST /groups/{id}/members` - Добавление участника в группу

## 🧪 Планы на будущее / TODO
- [ ] Добавить аналитику расходов
- [ ] Реализовать систему уведомлений
- [ ] Добавить интеграцию с популярными магазинами
- [ ] Реализовать систему рекомендаций
- [ ] Добавить поддержку нескольких языков
- [ ] Улучшить систему безопасности

## 📜 Лицензия
MIT License

Copyright (c) 2024 Your Name

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

