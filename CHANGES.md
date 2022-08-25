# Release notes
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [1.0.0] - 2022-01-14
### Added

- Initial release
- Added search auto-complete with search history for configurable number of unique terms
- Scroll to top button for infinite scroll pages
- Date headings for share link pages
- Dashboard stats for keywords
- Added lightgallery rotate plugin

### Changed
- Improved API response for unauthorized requests
- Added keyword table and removed keyword field from metadata
- Updated site CSS to make JQuery UI align with bootstrap UX
- Delete all orphaned keywords if no longer used in any photos

### Fixed

- Fixed editing lat/lng on map photo view
- Fixed processing lat/lng
- Fixed map icon flickering
- Fixed timezone offset list and selection
- Fixed thumbnail removal for non-timeline views
- Fixed API data structure of values returned
- Prevent keyword blank string entry
- Open only one search link when clicking on dashboard bar for camera and keywords

## [2.0.0] - 2022-01-29
### Added

- Input to change or add camera model
- Added description field in metadata

### Changed

- Simplified timeline API and view logic
- Optimized modals on views
- Re-hauled date scrolling
- Changed params for UUID generation for metadata

### Fixed

## [2.0.1] - 2022-02-21
### Added

- Import/Export metadata functionality

### Changed

### Fixed

- UX fixes and improvements for mobile
- Fixed favorite counts for media

## [2.0.2] - 2022-03-15
### Added

- Added nonce to inline Javascript

### Changed

### Fixed

## [2.1.0] - 2022-03-18
### Added

- Added recently modified view and API

### Changed

### Fixed

- Firefox scroll jumping

## [2.2.0] - 2022-06-19
### Added

- Added ability to download selected files in various views

### Changed

### Fixed

## [2.2.1] - 2022-06-20
### Added

- Spinner on download icon while making AJAX call for timeline media downloads
- UX improvements for backup exports

### Changed

- Minor file naming change for downloaded files

### Fixed

- Prevent multiple downloads while downloading