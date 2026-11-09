# ContractIQ backend

Java 21 / Spring Boot 3.5 modular-monolith API. Local integrations use open-source services in Docker Compose, including RustFS for S3-compatible object storage; Brevo email is optional, and the API never fabricates AI responses.

## Local setup

1. Copy `.env.example` to `.env` in this directory. Replace `JWT_SECRET`, `MINIO_SECRET_KEY`, and `MYSQL_ROOT_PASSWORD` with development-only random values. For example, generate a JWT key with:

   ```text
   node -e "console.log(require('crypto').randomBytes(48).toString('base64url'))"
   ```

   Do not reuse these values outside a local development environment.
   If host ports `3306` or `6379` are already occupied, set `MYSQL_HOST_PORT` or `REDIS_HOST_PORT` to unused local ports.
2. Start the free local services:

   ```text
   docker compose --env-file .env up -d mysql kafka redis object-store qdrant ollama
   ```

3. Pull the explicitly configured models (first pull downloads several GB and needs network access):

   ```text
   docker compose --env-file .env exec ollama ollama pull llama3.2:3b
   docker compose --env-file .env exec ollama ollama pull nomic-embed-text
   ```

   Use the same names in `.env` if you choose different Ollama models. The embedding model dimension must match the Qdrant collection; create a new collection if changing dimensions. The local Ollama service defaults to a 2,048-token context and one parallel request to limit memory use; adjust `OLLAMA_CONTEXT_LENGTH`, `OLLAMA_NUM_PARALLEL`, or `OLLAMA_MAX_LOADED_MODELS` only when the host has enough memory.
4. Run the API:

   ```text
   docker compose --env-file .env --profile application up -d --build app
   ```

   Or run it on the host after starting dependencies:

   ```text
   mvn -B test
   ```

   Set `SPRING_PROFILES_ACTIVE=local` and `JWT_SECRET` (at least 32 bytes) before running `mvn spring-boot:run`. Flyway applies the schema on startup; Hibernate is set to `ddl-auto: validate`.
5. Check `http://localhost:8080/actuator/health` and browse the OpenAPI UI at `http://localhost:8080/swagger-ui.html`.

The Compose profile exposes ports only on loopback. `docker compose down` stops containers; `docker compose down -v` also destroys the local database, uploaded files, and model/vector data.

### Resource notes

All local infrastructure images and OCR are free/open-source for development. MySQL, Kafka, Redis, RustFS, Qdrant, Ollama, and Tesseract together consume significant memory and disk; plan for at least 8 GB RAM (16 GB recommended), plus several GB for each pulled model and persistent volumes. For smaller local environments, choose a smaller chat model (for example `llama3.2:1b`) and retain the low-context, single-request Ollama defaults. Inference latency depends on CPU/GPU and model selection. The Kafka processor handles one document event at a time and allows up to 15 minutes for local AI inference (`KAFKA_MAX_POLL_INTERVAL_MS`) before considering the consumer unresponsive. No AI cloud API is called by default. Brevo delivery requires a Brevo account/API key and may be subject to its account limits or charges.

The Docker application image installs Tesseract English data. For host execution, install Tesseract and ensure it is on `PATH`, or set `TESSERACT_BINARY`; set `TESSERACT_LANGUAGES` to available trained-data language codes. OCR is used per PDF page only when the text layer is empty. OCR timeouts and provider errors fail processing visibly rather than silently dropping page text.

To enable reminder email, set `BREVO_EMAIL_ENABLED=true`, `BREVO_API_KEY`, and `BREVO_SENDER_EMAIL` (optionally `BREVO_SENDER_NAME`) in the local `.env`. Reminder notifications remain available in-app when Brevo is disabled. Delivery is queued transactionally and retried up to five times; inspect `email_outbox.last_error` for exhausted deliveries. Keep API keys out of source control.

## API summary

All application APIs are under `/api/v1`; all except registration, login, and refresh require a signed bearer access token. Register creates an organization and OWNER membership. Passwords are BCrypt hashes, access JWTs expire quickly, refresh credentials are random opaque tokens whose SHA-256 hashes are persisted and rotated on use. Redis provides fixed-window limits for login attempts.

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | Create account, organization, and owner membership |
| POST | `/auth/login` | Login |
| POST | `/auth/refresh` | Rotate refresh token |
| POST | `/auth/logout` | Revoke the caller's refresh token |
| POST | `/auth/switch-organization` | Issue tokens for another organization membership |
| GET | `/auth/me` | Current principal |
| GET, POST | `/organizations/current/members` | List members / add an existing account |
| GET, POST | `/contracts` | Tenant-scoped contract list / create; GET supports `search`, `counterparty`, `contractType`, `status`, `createdFrom`, and `createdTo` filters |
| GET | `/contracts/{contractId}` | Tenant-scoped contract detail |
| GET | `/contracts/{contractId}/clauses` | List AI-extracted structured clauses, optionally filtered by `type` |
| POST | `/contracts/compare` | Compare two same-organization contracts with verified page citations |
| GET, POST | `/contracts/{contractId}/documents` | List documents / upload PDF, DOCX, or TXT |
| GET | `/contracts/{contractId}/documents/{documentId}/download` | Download a tenant-owned object |
| GET | `/processing/jobs/{jobId}` | Processing state |
| POST | `/processing/jobs/{jobId}/retry` | Requeue a failed job (bounded attempts) |
| GET | `/contracts/{contractId}/risks` | Deterministic potential-risk findings |
| PATCH | `/risks/{riskId}/review` | Confirm/dismiss a finding |
| GET | `/obligations` | Organization-scoped obligation list |
| POST | `/contracts/{contractId}/obligations` | Add a due obligation tied to an indexed evidence page |
| PATCH | `/obligations/{obligationId}` | Update obligation status |
| GET | `/notifications` | Current user's in-app reminder list |
| PATCH | `/notifications/{notificationId}/read` | Mark a tenant-owned notification read |
| POST | `/search/semantic` | Tenant-filtered semantic search |
| POST | `/chat` | Ollama response grounded in tenant-filtered search evidence |
| GET | `/analytics/overview` | Counts queried from organization data |
| GET | `/audit-events` | Organization-scoped audit page (OWNER/ADMIN) |

Errors use `{ "error": { "code", "message", "details", "correlationId", "timestamp" } }`. Cross-tenant document, contract, processing, risk, search, analytics, and audit reads are constrained by the authenticated organization ID. A missing resource and a resource belonging to a different organization both return not found.

## Processing and provider behavior

Uploads are size-limited, extension and server-side content-type checked, SHA-256 deduplicated per organization, and stored in RustFS locally through the MinIO-compatible S3 client. Metadata, queued processing state, and an outbox message are committed together in MySQL. The outbox publisher dispatches to Kafka; document text extraction (including Tesseract OCR for scanned PDF pages), normalization, page-aware chunking, Ollama embeddings, tenant-tagged Qdrant upserts, structured LLM clause/obligation extraction, and deterministic potential-risk rules run asynchronously. Extracted evidence must be an exact quote from its cited page; invalid model output fails the job. AI-extracted obligations are marked `AI`; manual obligations remain `MANUAL`. Extraction retries are idempotent and preserve obligation status and linked notifications. Kafka retries failures; the retry API can requeue failed jobs. Chunk/vector IDs are deterministic to make processing retries idempotent.

Qdrant access is behind `TenantVectorIndex`; Qdrant filters are mandatory on organization ID (and optionally contract IDs). Spring AI uses the configured Ollama chat and embedding models. Provider errors are not replaced with canned answers. Chat and comparison responses include only verifiable retrieved/page citations; absent or unverifiable evidence does not produce a successful fabricated answer. Uploaded text is treated as untrusted input. Deterministic findings require human review and are not legal conclusions.

The local profile is for development only. Non-local startup must explicitly configure all datasource, JWT, Redis, Kafka, S3-compatible object-storage, Qdrant, and Ollama settings. If Brevo is enabled, valid API key and sender settings are required. Never use the example local credentials in a shared or production environment.

## Audit and verification

Tenant-scoped audit events cover authentication/token lifecycle, membership changes, contract and document access, document processing/retries, clause analysis, risks, obligations, notifications, search/chat/comparison, analytics, and reminder-email attempts/results. Audit records omit document contents, prompts, tokens, and credentials. Only OWNER/ADMIN can read an organization's audit page.

Unit tests include organization-scoping checks, authentication filters, comparison/extraction evidence validation, scanned-page OCR fallback, email-outbox behavior, chunking idempotency/page metadata, and evidence-backed risk behavior. Run `mvn -B test` from this directory. The MySQL Testcontainers integration test is skipped if the Docker daemon is unavailable. The project compiles with `--release 21`; run the suite and local stack under Java 21/Docker before deployment.
