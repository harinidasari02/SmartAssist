# SmartAssist – AI-Powered Chatbot Application

A Spring Boot chatbot that routes messages to either a local model (Ollama) or Grok (xAI API), keeps context across a conversation, and stores the full history in MySQL.

## Features
- Chat with **Ollama** (local, free) or **Grok** (cloud) from the same API; pick the model per message
- **Context-aware replies:** the last 20 messages of the conversation are sent with every request
- **Conversation history in MySQL:** list, reopen and delete conversations
- **Provider abstraction:** `AiProvider` interface, so a new model backend is one new class
- Clear errors: provider down, missing API key, unknown provider, empty replies (HTTP 502/400/404 as JSON)
- Nothing is saved if the AI call fails (the transaction rolls back)
- Small built-in chat page at http://localhost:8080

## Architecture
```
Browser / API client
        |
  ChatController            REST endpoints, request validation
        |
  ChatService               chat logic: routing, context window, saving history
     |         |
AiProvider   Repositories   API integration layer | data layer (Spring Data JPA)
 |      |          |
Ollama Grok      MySQL
```

## Tech Stack
Java 17, Spring Boot 3, Spring Data JPA, MySQL, Spring `RestClient`, Ollama, Grok (xAI), Maven, JUnit 5, Mockito

## Setup
Needs Java 17+, Maven, MySQL 8+, and at least one provider.

**1. Ollama (local model)**
1. Install Ollama from https://ollama.com and start it.
2. Pull a model: `ollama pull llama3.2` (or set `OLLAMA_MODEL` to any model you have).

**2. Grok (optional)**
Create an API key in the xAI console and set `GROK_API_KEY`. Set `GROK_MODEL` to a model your account can use.

**3. Run**
```bash
# Windows (cmd)
set DB_PASSWORD=your_mysql_password
set GROK_API_KEY=your_key          (optional)
mvn spring-boot:run

# Mac / Linux
export DB_PASSWORD=your_mysql_password
export GROK_API_KEY=your_key       # optional
mvn spring-boot:run
```
The `smartassist` database is created automatically. Open http://localhost:8080.

## Configuration
| Variable | Default | Purpose |
|----------|---------|---------|
| DB_URL / DB_USER / DB_PASSWORD | local MySQL / root / empty | Database |
| OLLAMA_URL | http://localhost:11434 | Ollama server |
| OLLAMA_MODEL | llama3.2 | Ollama model name |
| GROK_API_KEY | empty | Enables Grok |
| GROK_MODEL | grok-4 | Grok model name |

## API
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/chat | Send a message. Body: `{ "conversationId": null, "message": "Hi", "provider": "ollama" }` |
| GET | /api/providers | Available providers |
| GET | /api/conversations | List conversations |
| GET | /api/conversations/{id}/messages | Full history of one conversation |
| DELETE | /api/conversations/{id} | Delete a conversation |

Leave `conversationId` empty to start a new chat. Reuse the returned id to continue it.

## Tests
```bash
mvn test
```
Covers conversation creation, context ordering, default and unknown providers, rollback on provider failure, and the Ollama and Grok clients against mocked HTTP servers (request format, auth header, reply parsing, errors).

## Screenshots
Add screenshots to `docs/` and link them here.

## Future Improvements
- Streaming responses
- Authentication and per-user conversations
- Docker Compose setup
