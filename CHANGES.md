# Change Log
All notable additions, changes, fixes and removals to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
- Put place name beside latitude/longitude in metadata modal
- - Account deletion for non-super admins

### Changed
- Further optimizing e2e tests
- UX improvements for deleting content and account

### Fixed
- Album batch modal select
- CI script to be more streamlined
- Sub html in LightGallery for descriptions changed to toast messages to avoid collisions with video controls

## [2.7.4] - 2025-03-01
### Added
- Color select for maps
- Upload artifacts to RepoFlow in CircleCI script

### Changed
- Album and people select now modal select

### Fixed
- Media directory parsing
- Small bug fixes
- Flaky tests

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
