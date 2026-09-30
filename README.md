# CodeDoc AI

CodeDoc AI is an AI-powered technical documentation generator for software projects. Developers can upload source code, and the platform utilizes Large Language Models (LLMs) to automatically detect complexity and generate robust documentation split by sections (e.g., classes, functions, summaries). 

Users can manually edit, reorganize, and manage comments on the generated documentation, as well as export it to Markdown or PDF.

## 🚀 Tech Stack

**Backend:**
- Spring Boot 3.2 (Java 17)
- Spring Data JPA, Spring Security 6 (JJWT)
- PostgreSQL 15, Flyway for database migrations
- Maven for dependency management
- NetworkNT JSON Schema Validator for LLM response validation

**Frontend:**
- Angular 17 (Standalone Components)
- Angular Material
- Signal-based state management
- CDK Drag and Drop

## 📦 How to Run

### Option 1: Docker Compose (Recommended)
You can easily spin up the PostgreSQL database and backend using Docker.
```bash
docker-compose up -d
```
*Note: Make sure to verify the services defined in `docker-compose.yml`.*

### Option 2: Local Development Setup

1. **Database:** Start a PostgreSQL 15 instance. Create a database `codedoc` with user `codedoc` and password `codedoc`.
2. **Backend:** 
   Navigate to the `backend` directory and run:
   ```bash
   ./mvnw spring-boot:run
   ```
   The backend will automatically apply Flyway migrations and run on `http://localhost:8081`.

3. **Frontend:**
   Navigate to the `frontend` directory, install dependencies, and start the Angular dev server:
   ```bash
   npm install
   npm run start
   ```
   The frontend runs on `http://localhost:4200`.

## ⚙️ Environment Variables

The backend relies on the following key environment variables (often configured in `application.yml` or `.env`):

- `SPRING_DATASOURCE_URL`: The PostgreSQL connection URL (e.g., `jdbc:postgresql://localhost:15432/codedoc`)
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `JWT_SECRET`: A secure, 256-bit secret key used for signing JSON Web Tokens.
- `AI_API_KEY`: Your OpenAI (or compatible) API key used for generating the LLM documents.
- `AI_API_URL`: The base URL for the LLM requests (e.g., `https://api.openai.com/v1/chat/completions`).
- `CORS_ALLOWED_ORIGIN`: Frontend URL allowed for CORS (e.g., `http://localhost:4200` for dev, production domain for prod).

## 🏗️ Architecture

### System Overview

CodeDoc AI separates standard CRUD operations from its core intelligence layer. This ensures that while code logic and file management act as robust and scalable systems, the external LLM integration is abstracted securely.

```text
+-------------------+       +-----------------------+       +------------------------+
|                   |       |                       |       |                        |
|   Angular UI      | <---> |   Spring Boot API     | <---> |  PostgreSQL Database   |
|   (Frontend)      |  REST |   (Backend)           | JDBC  |  (Storage & State)     |
|                   |       |                       |       |                        |
+-------------------+       +-----------+-----------+       +------------------------+
                                        |
                                        | HTTP/REST
                                        v
                            +-----------------------+
                            |                       |
                            |   External LLM API    |
                            |   (OpenAI or Mock)    |
                            |                       |
                            +-----------------------+
```

### CRUD vs AI Layers

**CRUD Layer:**
- Implements conventional Spring Data JPA functionality.
- Stores users, projects, uploaded source code, and manually edited doc sections and comments.
- Performs all security and validation strictly, ensuring a user only accesses their own projects.

**AI Layer:**
- Composed of the `AiService` in the backend. 
- Operates asynchronously (or blocks on HTTP RestClient calls).
- When a user triggers "Generate Documentation" or "Detect Complexity":
  1. The code is pulled from the DB.
  2. A specific prompt template (e.g., `generate_doc_v1.txt`) is loaded from resources and populated with context.
  3. The prompt is dispatched to the LLM.
  4. The response is rigorously validated against JSON Schemas (using NetworkNT JSON Schema Validator) before being accepted.
  5. The structured data is persisted into `generated_docs` and `doc_sections`.

### API Endpoints

#### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login and receive JWT

#### Projects
- `GET /api/projects` - List all projects
- `POST /api/projects` - Create a project
- `GET /api/projects/{id}` - Get project details
- `DELETE /api/projects/{id}` - Delete a project

#### Files
- `POST /api/projects/{id}/files` - Upload source file
- `GET /api/files/{id}` - Get source file contents

#### AI Operations
- `POST /api/ai/generate-doc` - Trigger documentation generation for a file
- `POST /api/ai/detect-complexity` - Trigger complexity detection for a file

#### Documentation Management
- `GET /api/docs/{id}` - Fetch a generated document
- `GET /api/docs/{id}/export?format={md|pdf}` - Export document
- `GET /api/docs/{id}/sections` - List sections for a document
- `POST /api/docs/{id}/sections` - Add a new section manually
- `PATCH /api/sections/{id}` - Update a section's text or title
- `POST /api/docs/{id}/sections/reorder` - Reorder sections
- `POST /api/sections/{id}/comments` - Add a comment to a section

### Database Schema

The database strictly enforces relationships and cascaded deletions.

1. **`users`**: Contains user info (`username`, `email`, `password` hash, `role`).
2. **`projects`**: Grouping entity. Belongs to a user.
3. **`source_files`**: A single source code file uploaded to a project. Contains raw content and `size_bytes`.
4. **`generated_docs`**: The root of an AI generation task. Linked to `source_files`. Tracks the LLM `model_used`, `status` (PENDING, SUCCESS, FAILED), and the `raw_response`.
5. **`doc_sections`**: The parsed, structured sections of documentation (e.g. classes, functions). Linked to a `generated_doc`. Tracks `order_index` for drag-and-drop.
6. **`comments`**: User comments threaded on a `doc_section`.
7. **`ai_logs`**: Audit trail of AI executions tracking token usage and latencies.

## 🖼️ Screenshots

![Dashboard Placeholder](placeholder-dashboard.png)
*Dashboard showing uploaded files and projects.*

![Doc Editor Placeholder](placeholder-editor.png)
*Document editor with drag-and-drop sections and complexity tracking.*
