# TaskPulse AI — Complete Project Context File
> Last updated: 2026-09-13
> Purpose: Hand-off context for AI assistants when switching sessions/credits.
> Always give this file to the AI assistant at the start of a new session.

---

## 1. What Is This Project?

**TaskPulse AI** is an AI-powered task management system.

- Users describe tasks in natural language → Google Gemini extracts structured task data → User confirms → Task saved to PostgreSQL.
- Users can also create tasks manually via a form.
- React frontend + Java Spring Boot backend + PostgreSQL + Redis.

---

## 2. Tech Stack

| Layer     | Technology                                                  |
|-----------|-------------------------------------------------------------|
| Frontend  | React (Vite), JavaScript/JSX, Axios, Vanilla CSS            |
| Backend   | Java 21, Spring Boot 3.4.2, Spring Security, JWT, Spring AI |
| Database  | PostgreSQL (localhost:5432, DB: `taskpulse`)                |
| Cache     | Redis (localhost:6379) — used for AI response caching       |
| AI        | Google Gemini API via Spring AI (`spring-ai-starter-model-google-genai` v1.1.2) |
| Auth      | JWT (`jjwt` 0.12.5), HS256                                  |
| Docs      | SpringDoc OpenAPI (Swagger UI)                              |

---

## 3. Project Directory Structure

```
taskpulse-ai/                          ← root
├── backend/                           ← Spring Boot backend
│   ├── pom.xml
│   └── src/main/java/com/zubair/taskpulse/
│       ├── BackendApplication.java
│       ├── config/                    ← Spring configs (Security, CORS, Cache, etc.)
│       ├── controller/
│       │   ├── AIController.java      ← POST /api/ai/extract-task, POST /api/ai/tasks
│       │   ├── AuthController.java    ← POST /api/auth/register, POST /api/auth/login
│       │   ├── TaskController.java    ← CRUD: /api/tasks
│       │   ├── HealthController.java
│       │   └── UserController.java
│       ├── dto/
│       │   ├── ai/                    ← AITaskRequest, AITaskResponse, AIConfirmTaskRequest
│       │   ├── task/                  ← CreateTaskRequest, UpdateTaskRequest, TaskResponse
│       │   └── request/ + response/   ← auth DTOs
│       ├── entity/
│       │   ├── Task.java
│       │   ├── TaskStatus.java        ← TODO, IN_PROGRESS, COMPLETED
│       │   ├── TaskPriority.java      ← LOW, MEDIUM, HIGH, URGENT
│       │   ├── User.java
│       │   └── UserRole.java
│       ├── repository/                ← TaskRepository, UserRepository (Spring Data JPA)
│       ├── security/                  ← JWT filter, UserDetailsService, etc.
│       ├── service/
│       │   ├── AIService.java         ← Gemini extraction with retry + cache
│       │   ├── AIRateLimitService.java
│       │   ├── AIMetricsService.java
│       │   ├── DeadlineParser.java    ← Parses "by Friday", "tomorrow at 5pm", ISO dates, etc.
│       │   ├── TaskService.java       ← Interface
│       │   └── impl/
│       │       ├── TaskServiceImpl.java
│       │       └── AuthenticationService.java
│       ├── exception/                 ← AIProcessingException, global handler
│       └── util/
│   └── src/main/resources/
│       └── application.properties     ← All config here (DB, JWT, Redis, Gemini key)
│
├── taskpulse-frontend/                ← React (Vite) frontend
│   └── src/
│       ├── App.jsx                    ← Top-level router (Login / Dashboard)
│       ├── components/
│       │   ├── AITaskInput.jsx        ← AI task creation (natural language → preview → confirm)
│       │   ├── ManualTaskInput.jsx    ← Manual task creation form (Phase 1 DONE)
│       │   ├── TaskCard.jsx           ← Individual task card (view + edit + delete)
│       │   ├── TaskList.jsx           ← Renders list of TaskCards
│       │   ├── Login.jsx
│       │   └── Register.jsx
│       ├── pages/
│       │   └── Dashboard.jsx          ← Main dashboard page
│       ├── services/
│       │   └── api.js                 ← Axios API layer (JWT auto-attached)
│       └── styles/
│           ├── dashboard.css
│           ├── manual-task.css
│           └── task-card.css
│
├── docker-compose.yml                 ← Containers: frontend, backend, postgres, redis
├── TaskPulse_AI_Project_Roadmap.txt   ← Full roadmap
└── CONTEXT.md                         ← THIS FILE
```

---

## 4. Backend API Endpoints

### Auth
| Method | Endpoint                | Description          |
|--------|-------------------------|----------------------|
| POST   | `/api/auth/register`    | Register new user    |
| POST   | `/api/auth/login`       | Login, returns JWT   |

### Tasks (JWT required)
| Method | Endpoint            | Description                      |
|--------|---------------------|----------------------------------|
| POST   | `/api/tasks`        | Create task manually             |
| GET    | `/api/tasks`        | Get all tasks for current user   |
| GET    | `/api/tasks/{id}`   | Get single task                  |
| PUT    | `/api/tasks/{id}`   | Update task                      |
| DELETE | `/api/tasks/{id}`   | Delete task                      |

Query params on GET /api/tasks: `?status=TODO&priority=HIGH` (optional)

### Gmail (JWT required)
| Method | Endpoint                | Description                                         |
|--------|-------------------------|-----------------------------------------------------|
| GET    | `/api/gmail/auth-url`   | Returns Google OAuth2 authorization URL             |
| GET    | `/api/gmail/callback`   | Google OAuth callback (public)                      |
| GET    | `/api/gmail/status`     | Get Gmail connection status for user                |
| POST   | `/api/gmail/sync`       | Sync unread emails and extract task suggestions     |
| DELETE | `/api/gmail/disconnect` | Disconnect Gmail account and delete tokens          |

---

## 5. Key Data Models

### Task Entity (Task.java)
```java
Long id
String title              // max 200 chars, required
String description        // max 5000 chars, optional (TEXT column)
TaskStatus status         // TODO | IN_PROGRESS | COMPLETED (default: TODO)
TaskPriority priority     // LOW | MEDIUM | HIGH | URGENT (default: MEDIUM)
LocalDateTime deadline    // optional, must be future
Integer estimatedDurationMinutes  // 1-1440, optional
LocalDateTime completedAt // set when status -> COMPLETED
LocalDateTime createdAt   // auto
LocalDateTime updatedAt   // auto
User user                 // owner (ManyToOne, FetchType.LAZY)
```

### CreateTaskRequest (DTO with validation)
```java
@NotBlank @Size(max=200) String title
@Size(max=5000) String description
TaskPriority priority
@Future LocalDateTime deadline
@Min(1) @Max(1440) Integer estimatedDurationMinutes
```

### TaskResponse (DTO returned to frontend)
```java
Long id, String title, String description
TaskStatus status, TaskPriority priority
LocalDateTime deadline, Integer estimatedDurationMinutes
LocalDateTime completedAt, createdAt, updatedAt
```

### AITaskResponse (from Gemini extraction)
```java
String title
TaskPriority priority
String deadlineExpression   // raw expression like "by Friday"
LocalDateTime deadline      // parsed by DeadlineParser
Integer estimatedDurationMinutes
```

---

## 6. Frontend API Layer (api.js)

```js
baseURL: "http://localhost:8080/api"
// JWT auto-attached from localStorage("token") on every request

login(email, password)
register(firstName, lastName, email, password)
createTask(task)               → POST /tasks
getTasks()                     → GET /tasks
updateTask(id, task)           → PUT /tasks/{id}
deleteTask(id)                 → DELETE /tasks/{id}
createAITask(prompt)           → POST /ai/extract-task
confirmAITask(task)            → POST /ai/tasks
```

---

## 7. AI Pipeline (Current Implementation)

```
User types natural language
        ↓
POST /api/ai/extract-task
        ↓
AIService.extractTask() → callGeminiWithRetry() [3 attempts]
        ↓
Gemini returns JSON (cleaned of markdown fences)
        ↓
AITaskResponse deserialized + validated
        ↓
DeadlineParser.parse(deadlineExpression) → LocalDateTime
        ↓
Frontend shows editable AI Preview
        ↓
User edits if needed → confirms
        ↓
POST /api/ai/tasks  (does NOT call Gemini again)
        ↓
TaskService.createTask() → PostgreSQL
```

IMPORTANT RULE: The confirm step (/api/ai/tasks) NEVER calls Gemini again. It uses the already-extracted data directly.

### AIService features:
- @Cacheable("ai-task-extraction") — caches by prompt + today's date in Redis
- Retry: 3 attempts, 1s/2s delay between retries
- Validates: title not blank, title <= 200 chars, priority not null, duration 1-1440

### DeadlineParser supports:
- today, tomorrow, yesterday
- in N days/weeks
- Day names: Monday, next Monday
- by Friday, before Monday
- tomorrow at 5 PM, Monday at 3:30
- ISO date strings

---

## 8. Application Configuration (application.properties)

```properties
server.port=8080

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/taskpulse
spring.datasource.username=postgres
spring.datasource.password=postgrespassword

# JPA
spring.jpa.hibernate.ddl-auto=update

# JWT
jwt.secret=myVerySecretKeyThatIsAtLeast32CharactersLongForHS256Algorithm
jwt.expiration=86400000   # 24 hours in ms

# Gemini
spring.ai.google.genai.api-key=<YOUR_GEMINI_API_KEY>
spring.ai.google.genai.chat.options.model=gemini-3.6-flash

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.type=redis
```

SECURITY NOTE: API key and JWT secret are in application.properties for dev only. Move to env vars before production.

---

## 9. Phase Completion Status

### DONE — Phase 0 (Core Foundation)
- [x] Spring Boot backend + PostgreSQL + JPA
- [x] User registration + login + JWT authentication
- [x] Task entity with all fields (id, title, description, status, priority, deadline, estimatedDurationMinutes, completedAt, createdAt, updatedAt, user)
- [x] Full Task CRUD: POST/GET/PUT/DELETE /api/tasks
- [x] Task validation (title required, deadline must be future, duration 1-1440)
- [x] Task ownership: users can only access their own tasks
- [x] Task filtering by status and priority (query params)
- [x] React frontend (Vite) with Login + Register
- [x] Dashboard: stats, search, status/priority/deadline filters, sort (newest/oldest/priority/deadline/title)
- [x] Task cards with inline edit, delete, status-change dropdown
- [x] Deadline awareness: OVERDUE / TODAY / UPCOMING labels
- [x] Gemini AI task extraction with editable preview + confirm flow
- [x] DeadlineParser (natural language → LocalDateTime)
- [x] AI validation, retry (3 attempts), rate limiting, Redis caching

### DONE — Phase 1 (Manual Task Creation)
- [x] ManualTaskInput.jsx — form with title, description, priority, deadline, estimatedDurationMinutes
- [x] Integrated into Dashboard.jsx with tab switcher ("Create with AI" | "Manual Task")
- [x] creationMode state: "AI" or "MANUAL" switches which form renders
- [x] On task created: prepended to task list, mode resets to "AI"
- [x] Calls POST /api/tasks directly (no AI)
- [x] Client validation: title required
- [x] Backend validation: all constraints enforced
- [x] CSS: manual-task.css in src/styles/

### DONE — Phase 2 (Gmail Integration)
- [x] Gmail Token entity (`GmailToken.java`) & repository (`GmailTokenRepository.java`)
- [x] Gmail OAuth2 Service (`GmailOAuthService.java`) with code exchange & token auto-refresh
- [x] Gmail Service (`GmailService.java`) reading unread emails & running Gemini task extraction
- [x] REST Controller (`GmailController.java`) with auth-url, callback, status, sync, disconnect
- [x] Spring Security configured for public `/api/gmail/callback`
- [x] Frontend `GmailSync.jsx` component + `gmail-sync.css` dark mode styles
- [x] Popup OAuth window flow + postMessage connection detection
- [x] Task suggestion cards with full inline edit, confirm, and dismiss actions
- [x] Full build verification: backend Java compiled & frontend Vite production build succeeded

### NEXT — Phase 3 (Google Calendar Integration)
- [ ] Read calendar events, detect deadlines, and schedule tasks
- Phase 4: Intelligent AI scheduling engine
- Phase 5: Reminder system (deadline, overdue, daily summary)
- Phase 6: WhatsApp → Task integration
- Phase 7: Multi-source ingestion layer (Slack, Teams, Telegram, Outlook)
- Phase 8: AI Chat Assistant (conversational task management)
- Phase 9: Smart Task Intelligence (decomposition, priority reasoning, duration estimation)
- Phase 10: Task Dependencies
- Phase 11: Multi-channel Notifications
- Phase 12: Redis (expand beyond caching — session mgmt, background jobs)
- Phase 13: Background Jobs (email sync, reminder processing, daily summaries)
- Phase 14: Docker & Deployment
- Phase 15: Testing (unit, service, controller, integration, E2E)
- Phase 16: Production Security (secrets, CORS, audit logging)
- Phase 17: Observability & Metrics (AI request metrics, error tracking, Gemini usage)

---

## 10. Known Issues / Things to Watch Out For

1. Variable ordering issue in Dashboard.jsx — const now = new Date() is declared on line ~166 but referenced inside the filteredTasks filter (line ~86). This technically works (closure over mutable state doesn't apply here because it's a computed value), but is confusing. Refactor by moving now before filteredTasks.

3. todayTasks referenced inside filteredTasks — The UPCOMING deadline filter references todayTasks which is computed after filteredTasks. Both iterate over the original tasks state array so this is correct, but it's architecturally confusing.

4. Secrets in application.properties — Gemini API key and JWT secret are hardcoded. Must be moved to env vars before any cloud deployment.

5. gemini-3.6-flash model name — Verify this model name is supported by your Gemini API key and Spring AI version. Model names in Gemini can change.

6. Redis required at startup — If Redis is not running on localhost:6379, the backend will fail to start. For local dev without Redis: change spring.cache.type=simple in application.properties.

7. @Future validation on deadline — Backend rejects past dates with 400. Frontend datetime-local input has no min date enforcement. Add min attribute to input if you want frontend to enforce it too.

8. ManualTaskInput has no status field — Tasks created manually always start as TODO. This is intentional per the roadmap but can be added if needed.

9. No loading/error indicator in Dashboard for task list fetch — If getTasks() fails, tasks just stays empty with no user-visible error.

---

## 11. How to Run Locally

### Backend
```bash
cd backend
./mvnw spring-boot:run
# Prerequisites: PostgreSQL on localhost:5432 (DB: taskpulse), Redis on localhost:6379
# Config: src/main/resources/application.properties
```

### Frontend
```bash
cd taskpulse-frontend
npm install
npm run dev
# Runs on http://localhost:5173
# Calls backend at http://localhost:8080
```

### Docker (all services together)
```bash
docker-compose up
# See docker-compose.yml in project root
```

---

## 12. Instructions for the Next AI Assistant

When continuing work on this project, follow these rules:

1. READ THIS FILE FIRST before making any changes.
2. Read TaskPulse_AI_Project_Roadmap.txt for the full product vision.
3. Check Section 9 for completed phases and what to work on next.
4. Check Section 10 for known gotchas before touching Dashboard.jsx.
5. The NEXT task is Phase 2: Gmail Integration.
6. The codebase uses Lombok extensively — never manually write getters/setters/constructors.
7. Package base is: com.zubair.taskpulse
8. Frontend runs on port 5173. Backend runs on port 8080.
9. When adding new backend config, add it to application.properties.
10. When adding new API endpoints, update api.js in the frontend.
11. Always maintain the "user must confirm AI suggestions" principle — never auto-create tasks from AI.
12. The AI extraction endpoint and confirm endpoint are separate by design (two-step flow).
