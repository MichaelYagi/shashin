# Release notes
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [1.0.0] - 2022-01-14
### Added

- Initial release
- Added search auto-complete with search history for configurable number of unique terms
- Scroll to top button for infinite scroll pages
- Date headings for share link pages

### Changed
- Improved API response for unauthorized requests
- Added keyword table and removed keyword field from metadata
- Updated site CSS to make JQuery UI align with bootstrap UX

### Fixed

- Fixed editing lat/lng on map photo view
- Fixed processing lat/lng
- Fixed map icon flickering
- Fixed timezone offset list and selection
- Fixed thumbnail removal for non-timeline views
- Fixed API data structure of values returned
- Prevent keyword blank string entry