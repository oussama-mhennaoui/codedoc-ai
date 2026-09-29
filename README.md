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

## 🖼️ Screenshots

![Dashboard Placeholder](placeholder-dashboard.png)
*Dashboard showing uploaded files and projects.*

![Doc Editor Placeholder](placeholder-editor.png)
*Document editor with drag-and-drop sections and complexity tracking.*
