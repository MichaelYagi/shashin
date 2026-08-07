# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What Is This

Shashin is a self-hosted media gallery — a Spring Boot / Kotlin web app that scans local directories for photos and videos, extracts EXIF metadata, generates thumbnails, and serves a browsable gallery UI. It supports face recognition (via Argus), object detection, albums, maps, timelines, RSS/Atom feeds, and multi-user access control.

## Build Commands

```bash
# Development build (unminified JS, dev profile active by default)
./mvnw clean install

# Production build (minified JS assets)
./mvnw -Pprod clean install

# Run tests (Kotlin + JS)
./mvnw -Ptest test          # Kotlin/JVM tests (JUnit + Selenium)
npm test                    # JS unit tests (Mocha/Chai)

# Run a single Kotlin test class
./mvnw -Ptest test -Dtest=FileUtilsTests

# Run a single JS test file
npx mocha 'src/test/js/site/util.tests.js' --timeout 60000 --exit

# Run the app locally (dev mode, unminified JS)
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev"
```

The app runs on port **6624** by default. The dev profile loads `shashin_dev.db` (SQLite); the test profile loads `shashin_test.db`.

## Architecture

### Backend (Kotlin / Spring Boot 3.4)

**Layers:**
- `controller/` — Spring MVC `@Controller` classes handling HTTP endpoints and Thymeleaf model binding. `BaseController` is a shared base. `SettingsController` is the heaviest controller; it also owns the media scan logic (`scanMediaDirectories`).
- `service/` — Business logic not tied to HTTP: `MetadataProcessing` (EXIF extraction via metadata-extractor), `ImageProcessing` (thumbnails, filters, face/object recognition), `VideoProcessing`, `DuplicateImageDetection` (perceptual hash + BK-tree).
- `repository/` — Spring Data JPA repositories backed by SQLite.
- `model/` — JPA entities.
- `component/` — Spring-managed beans for cross-cutting concerns: `DataLoader` (seeds `Settings` on first run), `ScheduledTasks` (cron-based face/object scanning), `ShashinFileChangeListener` (triggers re-scan when media files change on disk), WebSocket message POJOs.
- `configuration/` — Security (`WebSecurityConfig` / `ApiSecurityConfig`), WebSocket, MVC, and file watcher config.
- `util/` — Stateless helpers: `FileUtils`, `TextUtils`, `SearchQueryBuilder`, `BKTree`, `MetricsUtil`.

**Database:** SQLite via Hibernate/JPA. Schema is initialised from `src/main/resources/schema.sql` on startup. The `Settings` table stores all runtime config (media directories, recognition thresholds, scheduled scan time, etc.).

**Security:** Three roles — `ROLE_USER`, `ROLE_ADMIN`, `ROLE_SUPER`. Two separate filter chains: a stateless API chain (`/api/v1/**`) using API-key authentication, and a session-based web chain for Thymeleaf pages. Public routes are declared in `MultiSecurityConfig.publicList`.

**Sidecar files:** Thumbnails and YAML metadata sidecars are stored in `sidecar_dev/` (dev) or `sidecar_test/` (test) relative to the project root. The sidecar path is set by `app.sidecar.path` in the profile-specific properties file.

**AI / recognition:**
- [Argus](https://github.com/MichaelYagi/argus) (external service) for face detection, clustering, recognition, and object detection; synced via webhook.
- [Ollama](https://ollama.com/) for vision AI: photo captions, keyword tagging, semantic search embeddings, conversational Q&A, and Memories generation. When a vision model is available, Ollama is used for object detection in place of Argus.

### Frontend

Thymeleaf templates in `src/main/resources/templates/`. JavaScript lives in `src/main/resources/static/js/`. The `app.js.useMinified` property (false in dev/test) switches between minified and unminified JS bundles.

The gallery lightbox is [Shoji](https://github.com/MichaelYagi/shoji) (`shoji.min.js` / `shoji.min.css`). Custom Shoji plugins live at `static/js/lg-*.js`: `lg-download.js`, `lg-cast-media.js`, `lg-metadata-detail.js`, `lg-video-thumbnail.js`, `lg-shashin-editor.js`. The Shashin gallery wrapper (`site/fragments/app/lightgallery.js`) wraps Shoji construction and provides `shashin.initLightGallery`, `shashin.setLightGallery`, and related helpers used by every gallery page.

JS tests use Mocha + Chai + Sinon + jsdom and live in `src/test/js/`.

### Test Structure

```
src/test/
  com/miyagi/shashin/
    unit/          # Pure unit tests (no Spring context)
    integration/   # @SpringBootTest with @ActiveProfiles("test")
    e2e/           # Selenium-based end-to-end tests
  js/              # Mocha JS tests
```

Integration and e2e tests use `@ActiveProfiles("test")` which activates `application-test.properties` and points to `shashin_test.db`.

## Key Configuration

| Property | Default | Notes |
|---|---|---|
| `server.port` | 6624 | |
| `app.sidecar.path` | `/sidecar_dev/` (dev) | Relative to working dir |
| `app.endpoint.url.argus` | `http://127.0.0.1:8100/` | Argus REST API |
| `app.endpoint.url.geocode` | Nominatim URL | Reverse geocoding |
| `app.role.super` / `.admin` / `.user` | `ROLE_SUPER` etc. | Role constants |

## Release Process

1. Bump `<version>` in `pom.xml`
2. Update `CHANGELOG.md` (keep-a-changelog format — required for auto-generated release notes)
3. Commit with `[skip ci]` to avoid triggering CI on the commit itself
4. `git tag v<version> && git push origin v<version>` — this triggers the CircleCI pipeline, uploads artifacts to RepoFlow, and creates a GitHub release

## Docker

```bash
./mvnw -Pprod clean install
docker build -t michaeltyagi/shashin .
docker run -d -p 6624:6624 michaeltyagi/shashin
```
