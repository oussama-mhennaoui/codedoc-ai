# CodeDoc AI

CodeDoc AI is an AI-powered technical documentation generator for software projects. Developers can upload source code, and the platform utilizes Large Language Models (LLMs) to automatically detect complexity and generate robust documentation split by sections (e.g., classes, functions, summaries). 

Users can manually edit, reorganize, and manage comments on the generated documentation, as well as export it to Markdown, PDF, or HTML.

## 🚀 Tech Stack

**Backend:**
- Spring Boot 3.2 (Java 17)
- Spring Data JPA, Spring Security 6 (JJWT 0.12.5)
- PostgreSQL 15, Flyway for database migrations
- Maven for dependency management
- NetworkNT JSON Schema Validator 1.3.3 for LLM response validation
- OpenPDF 1.3.39 for PDF generation

**Frontend:**
- Angular 17 (Standalone Components)
- Angular Material, Signals for state management
- ngx-highlightjs for syntax highlighting
- CDK Drag and Drop for section reordering

## 📦 How to Run

### Option 1: Docker Compose (Recommended)
You can easily spin up the PostgreSQL database, backend, and frontend using Docker.
```bash
docker-compose up --build
```
*Note: The frontend will be available at `http://localhost:4200` and the backend at `http://localhost:8081`.*

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

The backend relies on the following key environment variables (often configured in `application.yml` or Docker environment):

- `SPRING_DATASOURCE_URL`: The PostgreSQL connection URL (e.g., `jdbc:postgresql://db:5432/codedoc`)
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `JWT_SECRET`: A secure, 256-bit secret key used for signing JSON Web Tokens.
- `OPENAI_API_KEY`: Your OpenAI (or compatible) API key used for generating the LLM documents.
- `AI_API_URL`: The base URL for the LLM requests (e.g., `https://openrouter.ai/api/v1`).

## 🏗️ Architecture

CodeDoc AI separates standard CRUD operations from its core intelligence layer. This ensures that while code logic and file management act as robust and scalable systems, the external LLM integration is abstracted securely.

### System Overview

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
- Authentication state is persisted via JWT and restored on the frontend using `APP_INITIALIZER`.

**AI Layer:**
- Composed of the `AiService` in the backend. 
- Operates on HTTP RestClient calls to OpenAI-compatible APIs.
- When a user triggers "Generate Documentation":
  1. The code is pulled from the DB.
  2. A specific prompt template (e.g., `generate_doc_v1.txt`) is loaded from resources.
  3. The code is anonymized (emails, phone numbers removed) and sent to the LLM.
  4. The response is rigorously validated against JSON Schemas (`generate_doc_schema.json`) before being accepted.
  5. The structured data is persisted into `generated_docs` and `doc_sections`.
  6. **Note:** Only one generated document is allowed per source file to optimize costs.

### API Endpoints

#### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login and receive JWT
- `GET /api/auth/me` - Get current user profile

#### Projects
- `GET /api/projects` - List all projects
- `POST /api/projects` - Create a project
- `GET /api/projects/{id}` - Get project details
- `PATCH /api/projects/{id}` - Update a project
- `DELETE /api/projects/{id}` - Delete a project

#### Files
- `POST /api/projects/{id}/files` - Upload source file
- `GET /api/files/{id}` - Get source file contents
- `DELETE /api/files/{id}` - Delete a source file

#### AI Operations
- `POST /api/ai/generate-doc` - Trigger documentation generation for a file
- `POST /api/ai/detect-complexity` - Trigger complexity detection for a file

#### Quality & Code Smells Analysis (Module 1)
- `POST /api/quality/analyze` - Trigger code smells, SOLID & quality analysis for a file
- `GET /api/quality/files/{fileId}` - Get quality analysis report for a file
- `GET /api/quality/projects/{projectId}/summary` - Get aggregated project quality & technical debt summary
- `GET /api/quality/reports/{id}/export?format={pdf|md}` - Export quality report to PDF or Markdown

#### Documentation Management
- `GET /api/docs/{id}` - Fetch a generated document
- `PATCH /api/docs/{id}` - Update document title or status
- `DELETE /api/docs/{id}` - Delete a document
- `GET /api/docs/{id}/export?format={md|pdf|html}` - Export document
- `GET /api/docs/{id}/sections` - List sections for a document
- `POST /api/docs/{id}/sections` - Add a new section manually
- `PATCH /api/sections/{id}` - Update a section's text, title, or order
- `DELETE /api/sections/{id}` - Delete a section
- `POST /api/docs/{id}/sections/reorder` - Reorder sections (bulk update)
- `GET /api/sections/{id}/comments` - List comments for a section
- `POST /api/sections/{id}/comments` - Add a comment to a section

### Database Schema

The database strictly enforces relationships and cascaded deletions.

1. **`users`**: Contains user info (`username`, `email`, `password` hash, `role`).
2. **`projects`**: Grouping entity. Belongs to a user.
3. **`source_files`**: A single source code file uploaded to a project. Contains raw content, `size_bytes`, and `checksum`.
4. **`generated_docs`**: The root of an AI generation task. Linked to `source_files` with a unique constraint (one doc per file).
5. **`doc_sections`**: The parsed, structured sections of documentation. Linked to a `generated_doc`.
6. **`comments`**: User comments threaded on a `doc_section`.
7. **`ai_logs`**: Audit trail of AI executions tracking token usage and latencies.
8. **`quality_reports`**: Quality analysis reports (score global 0-100, code smells, SOLID principles, technical debt minutes and rating, JSONB raw analysis) linked 1-to-1 with `source_files`.

## 🖼️ Screenshots

![Dashboard Placeholder](placeholder-dashboard.png)
*Dashboard showing projects and file management.*

![Doc Editor Placeholder](placeholder-editor.png)
*Document editor with two-column layout, drag-and-drop sections, and comment threads.*
