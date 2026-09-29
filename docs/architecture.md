# Architecture

CodeDoc AI separates standard CRUD operations from its core intelligence layer. This ensures that while code logic and file management act as robust and scalable systems, the external LLM integration is abstracted securely.

## System Diagram

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

## CRUD vs AI Layers

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

## Endpoint List

### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login and receive JWT

### Projects
- `GET /api/projects` - List all projects
- `POST /api/projects` - Create a project
- `GET /api/projects/{id}` - Get project details
- `DELETE /api/projects/{id}` - Delete a project

### Files
- `POST /api/projects/{id}/files` - Upload source file
- `GET /api/files/{id}` - Get source file contents

### AI Operations
- `POST /api/ai/generate-doc` - Trigger documentation generation for a file
- `POST /api/ai/detect-complexity` - Trigger complexity detection for a file

### Documentation Management
- `GET /api/docs/{id}` - Fetch a generated document
- `GET /api/docs/{id}/export?format={md|pdf}` - Export document
- `GET /api/docs/{id}/sections` - List sections for a document
- `POST /api/docs/{id}/sections` - Add a new section manually
- `PATCH /api/sections/{id}` - Update a section's text or title
- `POST /api/docs/{id}/sections/reorder` - Reorder sections
- `POST /api/sections/{id}/comments` - Add a comment to a section

## Database Schema Summary

The database strictly enforces relationships and cascaded deletions.

1. **`users`**: Contains user info (`username`, `email`, `password` hash, `role`).
2. **`projects`**: Grouping entity. Belongs to a user.
3. **`source_files`**: A single source code file uploaded to a project. Contains raw content and `size_bytes`.
4. **`generated_docs`**: The root of an AI generation task. Linked to `source_files`. Tracks the LLM `model_used`, `status` (PENDING, SUCCESS, FAILED), and the `raw_response`.
5. **`doc_sections`**: The parsed, structured sections of documentation (e.g. classes, functions). Linked to a `generated_doc`. Tracks `order_index` for drag-and-drop.
6. **`comments`**: User comments threaded on a `doc_section`.
7. **`ai_logs`**: Audit trail of AI executions tracking token usage and latencies.
