\# CodeDoc AI — Copilot Instructions



\## Project

CodeDoc AI — a web app where developers upload source code and get

AI-generated technical documentation. Real CRUD + AI module (prompts only,

no model training).



\## Root

C:\\Users\\LENOVO\\Desktop\\spring boot\\Code Docs



\## Stack (do not change)

\- Backend: Spring Boot 3.2, Java 17, Maven, Spring Data JPA, Spring Security 6,

&#x20; JJWT 0.12.5, Flyway, PostgreSQL 15, networknt json-schema-validator 1.3.3, Lombok

\- Frontend: Angular 17 standalone components, Angular Material, Signals,

&#x20; HttpClient + functional interceptors

\- LLM: OpenAI-compatible API via Spring RestClient

\- Docker + docker-compose



\## Folder structure

\- backend\\src\\main\\java\\com\\codedoc\\   (Java sources)

\- backend\\src\\main\\resources\\db\\migration\\   (Flyway V\*.sql)

\- backend\\src\\main\\resources\\prompts\\   (LLM prompt .txt files)

\- backend\\src\\main\\resources\\schemas\\   (JSON schemas)

\- frontend\\src\\app\\core\\   (services, models, guards, interceptors)

\- frontend\\src\\app\\features\\   (pages)

\- frontend\\src\\app\\shared\\components\\   (reusable UI)



\## Java conventions

\- Package base: com.codedoc

\- Use jakarta.\* (not javax.\*)

\- Constructor injection via Lombok @RequiredArgsConstructor

\- No @Data on JPA entities (only @Getter @Setter @NoArgsConstructor)

\- DTOs are Java records

\- Controllers return DTOs, never entities

\- @Transactional on service write methods

\- Never log secrets, prompts, or full LLM responses

\- @AuthenticationPrincipal User currentUser in every secured controller



\## Angular conventions

\- Standalone components only (no NgModules)

\- inject() for DI (no constructor injection)

\- Signals for local state

\- Angular 17 control flow (@if, @for) — never \*ngIf / \*ngFor

\- OnPush change detection on every component

\- environment.apiUrl — never hardcode URLs

\- Services: providedIn: 'root'

\- File naming: feature.component.ts, feature.service.ts, feature.model.ts



\## AI module rules (CRITICAL)

\- Prompts live in resources/prompts/\*.txt — never hardcode prompts in Java

\- Schemas live in resources/schemas/\*.json (JSON Schema draft 2020-12)

\- ALWAYS validate LLM output against the schema before returning/saving

\- ALWAYS anonymize code (email, phone, api keys) before sending to LLM

\- ALWAYS log AI calls to ai\_logs table (user, endpoint, status, latency, tokens)

\- Error codes to handle: missing\_api\_key, quota, timeout, invalid\_schema, llm\_error

\- Never expose the API key to the frontend



\## Endpoint conventions

\- Base paths: /api/auth, /api/projects, /api/files, /api/docs, /api/ai

\- Plural nouns, kebab-case for multi-word

\- 201 on create, 204 on delete, 200 on read/update

\- Errors return: { timestamp, status, error, message, path, fieldErrors? }



\## Rules for you, Copilot

\- If a file path is ambiguous, ask before creating.

\- Never modify files outside the scope of the request.

\- Never add dependencies that aren't already in pom.xml / package.json.

\- Always produce compilable code.

\- Always include tests when creating a service or controller.

\- Prefer explicit types over `var` in Java when it helps readability.

\- For Angular, prefer typed forms (FormControl<T>).

