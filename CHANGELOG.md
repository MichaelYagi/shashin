# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/), and follows [keep a change log](https://keepachangelog.com/en/1.1.0/) v1.1.0 format.

* Added for new features
* Changed for changes in existing functionality 
* Deprecated for soon-to-be removed features 
* Removed for now removed features 
* Fixed for any bug fixes 
* Security in case of vulnerabilities

## [2.28.1] - 2026-09-01
### Fixed
- Memories: viewing a second memory slideshow always played the first one — Shoji's slide cache matched slots by index rather than item identity, so slots from the previous memory were reused; fixed by reinitialising the gallery for each memory open
- Albums: batch DB queries in buildAlbums replace per-album N+1 queries (224+ queries for 32 albums reduced to ~5)

## [2.28.0] - 2026-09-01
### Added
- Profile picture: "Pick from gallery" button on the account page — opens a modal to pick a photo directly from Shashin; admins/supers see an infinite-scroll grid of all photos via `/api/v1/taken`; users see their shared albums and can drill into an album to browse its photos; clicking a thumbnail loads it into the Kiri cropper
- Dashboard: combined user-role query, `@Cacheable` on location/camera/keyword/people stats, two new indexes (`idx_metadata_camera`, `idx_rlp_recognition_label_id`), removed redundant distinct-count queries

### Changed
- Dashboard: removed Page Metrics panel

## [2.27.0] - 2026-08-31
### Added
- Slideshow: replaced custom slideshow with a real Shoji lightbox instance — autoplay with configurable interval and progress bar, keyboard/touch shortcuts (Space to pause, arrow keys to navigate, `f` fill-screen, `d` caption, `i` instructions, `[`/`]` orientation, `-`/`=` interval, `a` album), Wake Lock API to keep screen on, cast and download in toolbar, infinite random-feed with preloading, fullscreen on open
### Changed
- Profile picture cropper: replaced Croppie with [Kiri](https://michaelyagi.github.io/kiri) — same circle-crop UX, file passed directly without FileReader, actively maintained first-party library

## [2.26.2] - 2026-08-28
### Fixed
- Batch date select: restores server-side ID lookup for all page types (timeline, taken, recent, modified, accessed, album) — the DOM-only approach introduced earlier missed tiles not yet rendered (off-screen evicted sections on timeline; unpaginated items on browse pages); results are cached per date to avoid repeat requests

## [2.26.1] - 2026-08-25
### Fixed
- Timeline: added concurrency guard to `renderThumbnails` (same pattern as `renderThumbnailsAlt`) — fast scrolling on wireless was spawning multiple concurrent instances, each making overlapping network requests and flooding the connection; now only one runs at a time and catches up with the latest scroll position when done
- Timeline: `renderViewport` (called on scroll-stop) now resets `prevElements` and forces a full render pass on desktop, fixing sections that refuse to render or only half-render when scroll stops on a slow connection
- Timeline: deduplicated in-flight per-thumbnail metadata requests in `renderMetadata` — the three `rescanElements` calls on scroll-stop could flood the server with duplicate requests for the same thumbnail

## [2.26.0] - 2026-08-25
### Fixed
- Tests: `recentImageAPITest` no longer fails with `ConcurrentModificationException` — scan completion is now detected by polling the thread file instead of a fixed-duration sleep, ensuring the background scan thread has fully finished before API tests run

## [2.25.0] - 2026-08-25
### Changed
- Optimized timeline queries: type filters now use prefix LIKE (`image/%`, `video/%`) enabling index use; added covering index on `(hidden, year, month, day, type)`
- Timeline full-view endpoints no longer cache favorites (fixes per-user favorites being served to all users)
### Fixed
- Timeline: sections now correctly removed from viewport when scrolling down, not only when scrolling up
- Timeline scroll handler crash (`Cannot read properties of undefined (reading 'replace')`) when a section is removed before its deferred `setDateSection` callback fires

## [2.24.0] - 2026-08-24
### Added
- Ask tab in media details modal now renders markdown and LaTeX in AI responses
### Changed
- Updated Shoji
### Fixed
- Timeline: infinite scroll placeholder (Stage 1) now appears correctly on desktop Chrome/Brave — `isScrolling` state snapshotted at scroll start instead of re-checked after async thumbnail rendering completes

## [2.23.2] - 2026-08-15
### Changed
- Updated Shoji
### Fixed
- Lightbox: zoom buttons (magnifying glass and actual-size) now hidden on video slides — they already did nothing on video, this removes the dead UI

## [2.23.1] - 2026-08-13
### Fixed
- Timeline: lightbox zoom-from-thumbnail animation now opens and closes correctly — `display: block` applied to `.mediaLink` so `getBoundingClientRect()` returns the thumbnail's real dimensions instead of the inline height
- Timeline: day heading batch select is now instant — IDs read directly from the DOM instead of an AJAX round-trip, and visual updates run synchronously in a single paint
- Timeline: infinite scroll section-load performance restored — CSS simplified to `display: block` only, removing `height: 100%` which forced height resolution on every link element in each newly-loaded section

## [2.23.0] - 2026-08-11
### Added
- ActiveThumbnail plugin enabled on all pages except timeline — thumbnail grid stays in sync with whichever slide is open in the lightbox
### Changed
- Updated Shoji
- Lightbox zoom-from-thumbnail animation enabled on open and close
### Fixed
- Lightbox: image opened from the details modal now renders above the modal instead of behind it (z-index)
- Lightbox: blank gap before the low-res placeholder eliminated — cache key mismatch between `item.thumb` and the grid `<img>` src removed
- Explore pages: date header text no longer runs together (e.g. "TakenThu, Jul 23") — flex formatting removed from `.dateSection .dateHeader`
- Explore pages: date heading batch select circle now responds to clicks — display check replaced with class check, which is unaffected by flex container blockification
- Explore pages: date heading batch select is now instant — IDs read directly from the DOM instead of an AJAX round-trip
- All pages: date heading batch select visual updates are now chunked via `requestAnimationFrame`, keeping the page responsive during large selections
- All pages: date heading batch select uses a single bulk storage write instead of one write per item

## [2.22.1] - 2026-08-08
### Changed
- Updated Shoji: caption block no longer overlaps native HTML5 video controls
### Fixed
- Explore gallery: date sections now display side-by-side when horizontal space permits (layout was inadvertently broken in a prior commit)
- Explore gallery: date header text no longer wraps within the header line
- Explore gallery: justified layout now sizes each section to its own content width rather than the full container, preventing overflow when sections sit side-by-side
- Info sidebar close-guard now targets `#metadataDetail` instead of the removed `.bi-info-circle.lg-icon` element
### Removed
- Dead `.lg-components` CSS rule (leftover from lightGallery migration)
- Dead `Util.subHtmlObserver()` function (leftover from lightGallery migration)

## [2.22.0] - 2026-08-02
### Added
- Memories module: AI-curated slideshows grouping photos by occasion and by person/pair over time; generated nightly when Ollama is configured, or on demand via the Regenerate button
- Memories: video items play in the slideshow with poster thumbnail and play button (via lgVideo plugin)
- Memories: place names incorporated into cluster hints for more contextually accurate titles
- Memories: photo count shown on each memory card
- Memories: 1:1 interleaving of longitudinal (person/pair histories) and occasion clusters so person memories appear throughout, not only at the end
### Changed
- Memories: centroid-based coherence filter removes outlier photos from mixed clusters, keeping each memory thematically consistent
- Memories: Ollama titles are generated without captions; dates used sparingly (month and year only, never the day)
- Memories: generic person labels (Baby, Boy, Girl, Man, Woman, etc.) trigger an automatic retry prompt to Ollama, forcing a place- or activity-based title instead
- Memories: duplicate titles are removed before display
- Memories: memories sorted with longitudinal first, then occasion
- Memories: default memory count increased to 16
- Memories: cluster photo exclusion samples ~12 photos per cluster (random) rather than marking the entire cluster as used
### Fixed
- Memories slideshow: media no longer shifts down to accommodate the toolbar (`allowMediaOverlap: true`)
- Memories slideshow: preload spinner eliminated — all images are loaded into browser cache before the gallery opens

## [2.21.4] - 2026-07-28
### Added
- 'Set as album cover' overlay is hidden on the person photo page when Argus is connected and managing that person's cover
- Argus person cover image on the People page now falls back to the Shashin thumbnail, then `fnf.png`, when the Argus connection is unavailable
### Changed
- Training Images badge now uses `detection_count` from `GET /api/identities/{id}` (all confirmed + reassigned detections) instead of `embedding_count` (enrolled only), matching the Argus gallery total
- Training Images tab now shows all confirmed detections, not just enrolled ones — `enrolled=true` filter removed from gallery fetch
- ArgusReconcile gallery sync now promotes detections with `review_status = confirmed` or `reassigned` to `auto_tagged = false`, not only those with `enrolled = true` — fixes Person tab undercounting photos that were confirmed in Argus but not yet enrolled in the model
### Fixed
- Person page slow to load: removed `syncArgusConfirmedToShashin` call from page load, and replaced the paginated review-queue walk (used for the Matches badge) with `pending_review_count` from `GET /api/identities/{id}` (requires Argus to expose this field; falls back to 0 until deployed)
- Training Images count showed 0 for large galleries: replaced 9999-item gallery fetch (which silently failed beyond the 256 KB WebClient buffer) with a single lightweight identity fetch
- WebClient `maxInMemorySize` increased to 16 MB in the gallery fetch path, preventing silent failures on large gallery responses
- N+1 DB queries in the Training Images tab eliminated: detection records and metadata are now batch-loaded per page instead of one query per item
- `findByArgusDetectionId` renamed to `findFirstByArgusDetectionId` everywhere to tolerate pre-`replace=true` duplicate rows without throwing `IncorrectResultSizeDataAccessException`

## [2.21.3] - 2026-07-22
### Added
### Changed
- Argus endpoint updated: `DELETE /api/face_embeddings/{id}` → `DELETE /api/face-embeddings/{id}` to match Argus API rename
- Dependency update: metadata-extractor version bump
### Deprecated
### Removed
### Fixed
- Argus webhook now correctly links manually-drawn bounding box detections to the person: `detection.created` events carrying an `identity_id` are linked immediately, and `identity.updated / detection_added` events update the record
- Argus reconcile sync now creates missing Shashin records for gallery detections that have a `source_external_ref` (detections confirmed in Argus that were never received via webhook or were received before Shashin was running)
- Media scan deletion-check phase now loads only `id` and `path` per row instead of full entities, making routine scans significantly faster on large libraries
- Fixed `ConcurrentModificationException` in overlapping scan cycles: `metadataIdArray` is now a `CopyOnWriteArrayList` so the recognition thread's iteration is not corrupted when a second scan starts concurrently

## [2.21.2] - 2026-07-20
### Added
### Changed
### Deprecated
### Removed
### Fixed
- Match indexing now passes `replace=true` to Argus, preventing duplicate detection rows when the same photo is submitted more than once in a single or concurrent indexing run
- People page cache is now evicted after Argus sync, so deletions from a wipe are reflected immediately without requiring a server restart

## [2.21.1] - 2026-07-19
### Added
### Changed
### Deprecated
### Removed
### Fixed
- Argus detect calls now pass `replace=true` on every submission, preventing duplicate detection rows from accumulating when the same photo is re-indexed across multiple scan runs
- Enrolling a person in a photo where Argus detects no face now returns an error and rolls back the newly created person record, preventing orphaned empty person entries
- ArgusReconcile cleanup no longer deletes people with a null `argus_identity_id` (regression introduced in a prior commit); only people whose linked Argus identity has been removed are cleaned up

## [2.21.0] - 2026-07-13
### Added
- Semantic search powered by Ollama (`nomic-embed-text`): photo descriptions are embedded on ingest and searchable via natural language queries
- Blended search combines keyword and semantic results on page 0, ranked by relevance; keyword-only results returned on subsequent pages
- Embeddings generated automatically during media scan via Ollama vision pipeline; backfill batch job available in Settings for existing libraries
- Embedding generation progress indicator in Settings (polls live progress during batch run)
- Search history now persisted and shown as autocomplete suggestions in the search box
- Ollama embed model status check in Settings (shows whether `nomic-embed-text` is available)
- Search spinner on profile icon in topnav while search is loading
- `GET /search/embeddings/progress` and `POST /search/embeddings/generate` API endpoints
- `GET /api/v1/search/semantic` API endpoint for direct semantic search

### Changed
- Semantic blended search skipped when keyword results already fill a page, keeping keyword searches snappy
- SQLite connection pool configured with `auto-commit=false` and `provider_disables_autocommit=true` to prevent double-commit errors with sqlite-jdbc
- Removed `transaction_mode=IMMEDIATE` from JDBC URL to eliminate write-lock contention under concurrent load
- Background embedding writes use `JdbcTemplate` directly (avoids JPA transaction interference from non-Spring-managed threads)

### Fixed
- SQLite cannot commit / cannot rollback errors during concurrent page loads while embedding generation runs in background

## [2.20.0] - 2026-07-10
### Added
- Argus webhook receiver (`POST /api/argus/webhook`) for real-time sync: handles `identity.created`, `identity.updated` (rename, thumbnail, enroll/unenroll), `identity.merged`, `identity.deleted`, and `detection.labeled`
- `external_ref` (Shashin metadata ID) now passed on all Argus detect calls, enabling direct photo lookup from webhook payloads

### Fixed
- Match Indexing tab now shown when facial or object detection is enabled, not when Argus is merely configured

## [2.19.2] - 2026-07-04
### Added
- Ollama vision model integration for generating photo descriptions and keyword tags
- Ollama falls back to Argus object detection when unavailable
- Ollama URL and vision model configurable in Settings

### Changed
- Face tagging now uses detect-then-label flow: POST detect (no label) → PUT label on specific face, preventing all faces in a group photo from being enrolled as the same person
- People page photo count now uses `COUNT(DISTINCT)` to match the count shown on the person detail page
- People page now shows all tagged photos without confidence threshold filtering, consistent with the person detail page
- Settings page layout: Pagination, Notification, and Search History limits moved to a single row (col-4 each); Match Scan and Training Data limits on the next row (col-6 each)

### Removed
- Facial Recognition Confidence Threshold setting (now managed in Argus)
- Object Recognition Confidence Threshold setting (now managed in Argus)

### Fixed
- Argus gallery sync no longer deletes pending detections that are not yet enrolled, preventing valid face tags from being removed after a match scan
- People page match badge no longer disappears after clicking into a person whose review queue was fetched at a different time
- Duplicate `recognitionlabelphoto` rows prevented by UNIQUE constraint on `(recognition_label_id, metadata_id)`
- `schema.sql` corrected to reflect actual Hibernate-generated column names for the `recognitionlabelphoto` table

## [2.19.1] - 2026-07-01
### Added
- Argus reconcile runs automatically after Match Indexing, Media Indexing, and the scheduled face scan
- Reconcile now creates new Shashin identities for people added directly in Argus
- Reconcile button moved to the People page (visible to admins and supers when Argus is available)
- Automatic Media Indexing skips the scan if any configured media directory is inaccessible, preventing false triggers when a NAS connection drops

### Changed
- Reconcile button hidden from the Match Indexing page

### Fixed
- Training Images tab no longer shows the batch-delete toolbar on initial page load before any images are selected
- People page cover photo now falls back to the first matched photo when no explicit cover is set, fixing blank covers for identities imported from Argus
- Argus-paginated training images no longer double up on first load

## [2.19.0] - 2026-06-23
### Added
- Argus integration for face and object recognition (self-hosted, replaces CompreFace)
- Object detection results surfaced as searchable keywords
- Matches workflow backed by the Argus review queue (confirm/reject suggested matches)
- Face and object detection settings are gated by the models active in Argus

### Changed
- Manual labeling now enrolls the person as an identity in Argus
- Argus connection check validates the API key against an authenticated endpoint instead of only checking server reachability

### Removed
- Native CompreFace and Deep Java Library (DJL) recognition code paths

### Fixed
- Orphaned-subject cleanup no longer deletes Argus identities, preventing loss of enrolled faces

## [2.18.2] - 2026-06-13
### Changed
- Repo now public

### Fixed
- Cast module on all pages
- Reset password for super admins fixed
- Page indexing
- Multi select

## [2.18.1] - 2026-05-11
### Changed
- Updated drewnoakes/metadata-extractor to 2.20.0

### Fixed
- RSS/ATOM formatting
- Fixed casting URL issues

### Security
- Bump org.codehaus.plexus:plexus-utils
- Bump the npm_and_yarn group across 1 directory with 1 update
- Bump @tootallnate/once in the npm_and_yarn group across 1 directory

## [2.18.0] - 2026-03-25
### Added
- Search API endpoints
- Description and keywords metadata update endpoints

### Security
- Bump the npm_and_yarn group across 1 directory with 1 update

## [2.17.4] - 2025-12-06
### Changed
- License to CC

### Security
- Updates `org.apache.tika:tika-core` from 3.0.0 to 3.2.2

## [2.17.3] - 2025-11-21
### Changed
- Rescan duplicates

### Fixed
- Query for duplicates

## [2.17.2] - 2025-11-19
### Fixed
- Date display bug in explore module

### Security
- Bump js-yaml from 4.1.0 to 4.1.1

## [2.17.1] - 2025-11-16
### Added
- Multi-select in duplicates page

### Changed
- Optimized multi-select

## [2.17.0] - 2025-11-11
### Added
- Duplicate image module

## [2.16.0] - 2025-11-09
### Changed
- Code refactoring to adhere to architectural coding standards
- UX tweaks to editor

### Fixed
- Share page paged video thumbnails
- Video thumbnails in mobile view
- Tests updated

## [2.15.3] - 2025-11-01
### Added
- Pause on video screenshot

### Fixed
- Fixed poster after taking video screenshot

## [2.15.2] - 2025-10-31
### Changed
- Centralized lightgallery events to a single location

### Fixed
- Viewer and player
- LightGallery integration
- Metadata look up permission

## [2.15.1] - 2025-10-29
### Changed
- Rebuilding editor UI using bootstrap
- Improved mobile UI/UX when in editor

### Fixed
- Backend range check for brightness, contrast, and saturation

## [2.15.0] - 2025-10-26
### Added
- Added Sharpness to editor

### Changed
- Batch selection improvements
- Removed play button from thumbnails
- Updated UI so that photos have a justified row layout

### Fixed
- Shift selecting same image

## [2.14.1] - 2025-10-08
### Added
- Added Saturation to editor

### Fixed
- Improvements to image editor

## [2.14.0] - 2025-10-05
### Added
- Simple editing module to rotate, flip, adjust contrast and brightness in photos

## [2.13.4] - 2025-09-30
### Added
- Location stats in Dashboard

### Fixed
- Fixed add to existing people checkbox for batch edits
- Multi-select bugs

### Security
- Bump org.springframework.security:spring-security-core from 6.4.4 to 6.4.10

## [2.13.3] - 2025-09-15
### Changed
- Simplified multi-select logic

### Fixed
- Notification page from crashing due to length limit

## [2.13.2] - 2025-09-04
### Added
- UI tweaks

### Fixed
- Double tap select images for mobile

### Security

## [2.13.1] - 2025-08-28
### Changed
- UI for date slider
- Use date slider in timeline for mobile view

### Fixed
- Scrolling in timeline

## [2.13.0] - 2025-08-16
### Added
- Increased support for Safari and Firefox

### Security
- Update brace-expansion from 2.0.1 to 2.0.2

## [2.12.0] - 2025-08-15
### Added
- Show progress bar option for slideshow
- Fill to screen option on slideshow
- Orientation option on slideshow
- Query params to define orientation to RSS/ATOM feeds

### Fixed
- Pause for slideshow
- Loading spinner in timeline when specifying date hash
- Timeline jumping

### Fixed
- ATOM feed date format

## [2.11.6] - 2025-08-07
### Added
- Date selection to missing coordinate, description, and comment filters
- Translations for tooltips

### Fixed
- ATOM feed date format

## [2.11.5] - 2025-08-05
### Fixed
- Consistency in date formats between JS and Kotlin
- Multi-select UI bugs and album multi select
- Filter type count
- Double tap on mobile

## [2.11.4] - 2025-08-01
### Fixed
- Password verification fixes
- i18n bugfixes

## [2.11.3] - 2025-07-31
### Added
- i18n setup with English fully templated

### Fixed
- Taken at view shift select and date select

## [2.11.2] - 2025-07-14
### Added
- Select photos by date in Added, Modified and Accessed views

## [2.11.1] - 2025-07-13
### Added
- Character count for description field

### Changed
- Refactored date batch selection
- Moved share link generation to back end

### Fixed
- Bug when selecting image in share view
- Share view dark mode
- Cancel when selecting in paged mode
- Batch select if same date

## [2.11.0] - 2025-07-10
### Added
- Select photos by date in timeline, taken, and albums

### Fixed
- Modal bug when pressing the 'i' key

## [2.10.4] - 2025-06-10
### Fixed
- People recognition query
- Update timeline maps after metadata edits
- People view queries
- convertMSToRelativeTime JS function

## [2.10.3] - 2025-05-22
### Added
- Dexie.js for timeline

### Removed
- Automatic browser opening after server start

### Fixed
- Don't show hidden photos in albums
- Health and status endpoint performance

### Security
- Updated bonigarcia web driver manager version

## [2.10.2] - 2025-05-09
### Added
- Map context to save or set coordinates from the map

### Changed
- Dashboard stats API keys
- Improved file stats performance

## [2.10.0] - 2025-05-01
### Added
- Filter for slideshow
- Keyboard shortcuts for slideshow

### Fixed
- Counts for pictures and videos in albums view
- RSS/ATOM feeds to show image preview and use slideshow filters
- Album filter logic

### Security
- Bump org.apache.httpcomponents.client5:httpclient5 from 5.4.1 to 5.4.3

## [2.9.1] - 2025-04-11
### Added
- Pagination for albums, shared albums, folders, and favourites pages
- Pagination pages to valid redirect pages
- Tests for Pagy pagination
- Kotlin function to detect mobile from user agent string
- Automated release notes after test & build
- Comments filter in album view
- UX improvement for snapshot feedback
- Notifications for album name changes
- Description filter for timeline, albums, shared albums, folders, and favourites pages

### Changed
- Smaller centered images in mobile view in gallery
- Util.isMobile kept to a single source
- Smaller centered images in mobile view in albums, folders and people view
- Refactored Shashin version checking and added tests

### Removed
- Duplicate lg listeners

### Fixed
- Total paged result calculation
- UI for adding and deleting comments
- UI margin adjustments for mobile views
- Timeline view more responsive
- EXIF format
- Mobile view media at start
- Mobile view in timeline

## [2.9.0] - 2025-04-03
### Added
- Taskbar icon for JFrame
- Badges for matched people
- Logout button on index pages
- Added pagination for recent, modified, taken and accessed pages (/page/mediatype)

### Fixed
- Access control for articles

## [2.8.3] - 2025-03-30
### Added
- Interval setting for slideshow
- Usage graph for sidecar storage
- Missing latitude/longitude filter
- Simple GUI component

### Changed
- Dynamically update albums and people in metadata modal
- Updated spring security version
- Access to dev articles
- Dashboard to count number of people tagged

### Fixed
- Panorama detection
- Prevent saving in modal when selecting auto complete item
- Fixed articles modal access
- Don't close metadata modal on rescan confirmation
- Exit gallery when browser back button pressed
- Redirect to taken page in Safari after login/register
- Truncating place name

### Removed
- Sidebar info
- Change port field

## [2.8.2] - 2025-03-23
### Added
- CpuMetrics class for dashboard
- Public endpoint for tags and release notes
- Detailed login/failed login information

### Changed
- Labels in dashboard circle charts
- Upgraded charts.js to 4.x.x

### Fixed
- Cookie check for login persistence
- Getting tag name

## [2.8.1] - 2025-03-15
### Added
- Tests for rescan feature
- Choose profile picture from URL
- Tests for CORS

### Changed
- Improved version checking
- Reset everything after rescan and close modal
- CORS for APIs
- Dashboard UI updates
- Improved server start time capture in dashboard

### Fixed
- Search history queue mechanism
- Links in dashboard bar graph

### Removed

## [2.8.0] - 2025-03-09
### Added
- Color select for maps
- Upload artifacts to RepoFlow in CircleCI script
- Put place name beside latitude/longitude in metadata modal

### Changed
- Album and people select now modal select
- Further optimizing e2e tests
- UX improvements for deleting content and account
- Sizes for modal windows appropriate for content
- Timeline scrollbar improvements

### Fixed
- Media directory parsing
- Small bug fixes
- Flaky tests
- Album batch modal select
- CI script to be more streamlined
- Sub html in LightGallery for descriptions changed to toast messages to avoid collisions with video controls
- Hanging element in timeline

### Removed
- Unneeded person modal to edit metadata

## [2.7.2] - 2025-02-23
### Added
- New release notices in about modal

### Fixed
- Mobile views

## [2.7.1] - 2025-02-19
### Added
- Password reset script for admins

### Fixed
- Timeline initial rendering
- Slideshow UI improvements

## [2.7.0] - 2025-02-03
### Added
- Multi select images

## [2.6.1] - 2025-01-18
### Added
- Drag and drop files to a preconfigured directory setting

## [2.6.0] - 2024-08-24
### Changed

- Updated to Spring Boot 3.x.x
- Updated from Java 11 to 17
- Updated Hibernate JPA to 6.x.x

## [2.5.1] - 2024-06-02
### Added
- Accessed in browse view

### Changed
- Added more search input titles

### Fixed
- Timeline scrolling rendering
- Non-blocking Dashboard loading
- CircleCI script

## [2.5.0] - 2024-05-03
### Added
- Super admin privileges

## [2.4.0] - 2023-10-08
### Added
- Slideshow view

### Fixed
- Full screen fixes and enhancements

## [2.3.0] - 2023-10-04
### Added
- Facial recognition using CompreFace
- Object recognition
- Interface to manage training images from CompreFace server
- API management endpoint and interface
- Profile photos 
- Custom context menu in map views
- Date taken in browse view
- Rescan metadata
- Capture video thumbnails

### Changed
- Updated API calls to reflect appropriate request types

### Fixed
- Bug fixes for modal edits
- Timeline view bug fixes

## [2.2.3] - 2023-04-26
### Added
- Editable Lens field
- SQLite backup added during metadata export
- Double click, Drag and Zoom added to maps
- Health endpoint
- Icon indicator for gifs

### Changed
- Use placeholder divs during timeline loading
- Rendering preview thumbnail images in viewport for better performance
- Updated to OpenLayers 7.4.0

### Fixed
- Bulk edit Albums and People dropdown list population
- Work around for duplicate video html on map view
- Map fullscreen button bug

## [2.2.2] - 2023-04-09
### Added
- Ability to download album photos

### Changed
- Paginated "Folders" and "Albums" view

## [2.2.1] - 2022-06-20
### Added
- Spinner on download icon while making AJAX call for timeline media downloads
- UX improvements for backup exports
- File browser for media directories
- Date filter for map

### Changed
- Minor file naming change for downloaded files
- Updated OpenLayers to 7.0.0

### Fixed
- Prevent multiple downloads while downloading
- Redirect to referer after login

## [2.2.0] - 2022-06-19
### Added
- Ability to download selected files in various views

## [2.1.0] - 2022-03-18
### Added
- Recently modified view and API

### Fixed
- Firefox scroll jumping

## [2.0.2] - 2022-03-15
### Added
- Nonce to inline Javascript

## [2.0.1] - 2022-02-21
### Added
- Import/Export metadata functionality

### Fixed
- UX fixes and improvements for mobile
- Fixed favorite counts for media

## [2.0.0] - 2022-01-29
### Added
- Input to change or add camera model
- Added description field in metadata

### Changed
- Simplified timeline API and view logic
- Optimized modals on views
- Re-hauled date scrolling
- Changed params for UUID generation for metadata

## [1.0.0] - 2022-01-14
### Added
- Initial release
- Added search auto-complete with search history for configurable number of unique terms
- Scroll to top button for infinite scroll pages
- Date headings for share link pages
- Dashboard stats for keywords
- Lightgallery rotate plugin

### Changed
- Improved API response for unauthorized requests
- Added keyword table and removed keyword field from metadata
- Updated site CSS to make JQuery UI align with bootstrap UX
- Delete all orphaned keywords if no longer used in any photos

### Fixed
- Editing lat/lng on map photo view
- Processing lat/lng
- Map icon flickering
- Timezone offset list and selection
- Thumbnail removal for non-timeline views
- API data structure of values returned
- Prevent keyword blank string entry
- Open only one search link when clicking on dashboard bar for camera and keywords

## [0.1.0] - 2021-08-09
### Added
- Initial commit
- Framework setup
- Base and timeline controller and views
