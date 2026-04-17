# AI Agent — Java 21 + Spring Boot 3 + llama.cpp

Локальный AI агент с долгосрочной памятью, инструментами и ReAct-оркестратором.

## Стек

- **Java 21**, **Spring Boot 3.2**
- **llama.cpp** — локальный LLM runtime (OpenAI-совместимый API)
- **H2** (embedded) — долгосрочная память / история сессий
- **Flyway** — миграции БД
- **Springdoc OpenAPI** — автодокументация

## Запуск

### 1. Запустить llama.cpp сервер

```bash
./llama-server \
  -m Ministral-3-3B-Instruct-2512-Q5_K_M.gguf \
  --port 8080 \
  -c 4096 \
  -ngl 35
```
```bash
./llama-server.exe -m ../models/Ministral-3-3B-Instruct-2512-Q5_K_M.gguf --port 8080 -c 4096
```

> Сервер должен быть доступен на `http://localhost:8080`

Скачать последнюю версию llama.cpp можно с [официального репозитория](https://github.com/ggml-org/llama.cpp/releases)

Скачать модель можно с [huggingface](https://huggingface.co/mistralai/Ministral-3-3B-Instruct-2512)

### 2. Запустить приложение

```bash
mvn spring-boot:run
```

Приложение стартует на порту **8081**.

## REST API

### Быстрый старт (новая сессия + вопрос)

```bash
curl -X POST http://localhost:8081/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Сколько будет 2847 умножить на 193?", "userId": "user1"}'
```

### Продолжить существующую сессию

```bash
# Создать сессию
curl -X POST "http://localhost:8081/api/agent/sessions?userId=user1"

# Отправить сообщение в сессию
curl -X POST http://localhost:8081/api/agent/sessions/{sessionId}/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Какая сейчас дата?"}'

# Получить историю сессии
curl http://localhost:8081/api/agent/sessions/{sessionId}/messages
```

### Все эндпоинты

| Метод | URL | Описание |
|-------|-----|----------|
| POST | `/api/agent/chat` | Новая сессия + первый вопрос |
| POST | `/api/agent/sessions` | Создать сессию |
| GET | `/api/agent/sessions` | Список сессий |
| GET | `/api/agent/sessions/{id}` | Получить сессию |
| DELETE | `/api/agent/sessions/{id}` | Удалить сессию |
| POST | `/api/agent/sessions/{id}/chat` | Сообщение в сессию |
| GET | `/api/agent/sessions/{id}/messages` | История сообщений |
| GET | `/api/agent/tools` | Доступные инструменты |

## Swagger UI

`http://localhost:8081/swagger-ui.html`

## Запуск frontend application
```
# Перейти в папку frontend
cd frontend
# Установить зависимости
npm install
# Запустить дев-сервер
npm run dev
```
> UI должен быть доступен на `http://localhost:5173`

## H2 Console

`http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:file:./data/ai_agent_db`
- User: `sa`, Password: (пусто)

## Инструменты агента

| Инструмент | Описание |
|-----------|---------|
| `calculator` | Математические вычисления |
| `datetime` | Текущая дата и время |
| `web_search` | Поиск через DuckDuckGo |

## Архитектура ReAct

```
User message
     │
     ▼
AgentService
     │
     ▼
AgentLoop (ReAct)
  ┌──────────────────────────────┐
  │  1. Build prompt + history   │
  │  2. Call LLM                 │◄──── LlmClient → llama.cpp
  │  3. Parse response           │
  │     ├─ TOOL_CALL → execute   │◄──── ToolRegistry
  │     │   └─ add TOOL_RESULT   │
  │     └─ FINAL_ANSWER → return │
  └──────────────────────────────┘
     │
     ▼
MemoryService → H2 DB (сессии + история)
```
