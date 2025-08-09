# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/), and follows [keep a change log](https://keepachangelog.com/en/1.1.0/) v1.1.0 format.

* Added for new features
* Changed for changes in existing functionality 
* Deprecated for soon-to-be removed features 
* Removed for now removed features 
* Fixed for any bug fixes 
* Security in case of vulnerabilities

## [Unreleased]
### Added
- Progress bar option for slideshow
- Orientation option on slideshow

### Changed
### Deprecated
### Removed
### Fixed
- Pause for slideshow

### Security

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
