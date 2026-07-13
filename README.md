[![CircleCI](https://circleci.com/gh/MichaelYagi/shashin/tree/main.svg?style=shield&circle-token=a7f7505f8d0357fbae3ca5be7a41ff8debed1d1d)](https://circleci.com/gh/MichaelYagi/shashin)

# Shashin
A self-hosted media gallery with AI-powered photo understanding.

Scan local directories, browse your library, and let AI describe, tag, and answer questions about your photos — all on your own hardware.

- **AI captions & keywords** — connect [Ollama](https://ollama.com/) to auto-generate descriptions and tags for every photo
- **Ask your photos** — conversational Q&A on any photo; ask follow-up questions and pick up where you left off; conversation history saved per photo
- **Face recognition** — detect, enrol, and label faces via [Argus](https://github.com/MichaelYagi/argus), with real-time webhook sync
- **Object detection** — automatically tag photos with what's in them
- **Interactive map** — browse by location with OpenLayers and reverse geocoding
- **Rich browsing** — timeline, folders, albums, favourites, keywords, date taken/modified/accessed
- **Full-text search** — across titles, descriptions, keywords, people, and places
- **EXIF metadata** — extracted automatically; fully editable
- **Duplicate detection** — perceptual hashing with BK-tree similarity search
- **Multi-user** — three role levels, session auth for the UI, API key auth for the REST API
- **RSS / Atom feeds**

<img src="https://michaelyagi.github.io/images/shashinss.png" alt="shashin"/>

# Get Started
Start from [here](https://michaelyagi.github.io/shashin/) to get setup.

# Versions
Must be updated in POM file and git tag with format v[Major][Minor][Patch].

# Maven Install
Run `mvn clean install` with unminified JS assets. Run `mvn -Pprod clean install` to run with minified JS assets. These also download and installs libraries needed for face recognition.

# Pipeline
Built on CircleCI with push to master at:

https://app.circleci.com/pipelines/github/MichaelYagi/shashin

To avoid triggering a CircleCI pipeline, ensure commit messages to master contain `[skip ci]`.

Eg. `git commit -m "[skip ci] Updated README"`

Creating and pushing tags will upload jar and exe artifacts to [RepoFlow](https://api.repoflow.io/browse/universal/cd373a98-3f60-419f-9c40-d10a1180ccda/shashin/shashin).

## Release Process

* Update version in `pom.xml` under `<project><version>{version}</version></project>`
* Update `CHANGELOG.md` with the new version
    * Adhere to [keep a change log](https://keepachangelog.com/) as capturing dev notes depends on this format
* Commit and push - use `[skip ci]` in commit message
* Create tag:
    * `git tag v<version>`
    * `git push origin v<version>`
* Finished! This will kick off a build in circleci with artifacts uploaded to RepoFlow and generate a Github release with notes based on `CHANGELOG.md`

# Docker
`mvn -Pprod clean install`

`docker build -t michaeltyagi/shashin .`

`docker run -d -p 6624:6624 michaeltyagi/shashin`

Published to https://hub.docker.com/repository/docker/michaeltyagi/shashin

# Development
To use unminified JS assets, set the following VM options:
`-Dspring.profiles.active=dev`

You can view dev notes on Shashin at `/articles/quickstart`.

## Frameworks Used
* [Spring Boot](https://spring.io/) - Java framework to create micro services and web apps
* [Thymeleaf](https://www.thymeleaf.org/) - Server-side Java template engine for both web and standalone environments

Tech Stack
* [Kotlin](https://kotlinlang.org/)
* Javascript

3rd Party Libraries used:
* [lightGallery](https://www.lightgalleryjs.com/) and [plugins](https://cdnjs.com/libraries/lightgallery) - Image and video lightbox
* [Metadata Extractor](https://github.com/drewnoakes/metadata-extractor) - Extracts Exif, IPTC, XMP, ICC and other metadata from image, video and audio files
* [JQuery](https://jquery.com/) - JavaScript library
* [OpenLayers](https://openlayers.org/) - Display map tiles, vector data and markers loaded from multiple map sources, like [OpenStreetMap](https://www.openstreetmap.org/), [ArcGIS](https://www.arcgis.com/index.html) and [MapTiler](https://www.maptiler.com/)
* [Nominatim](https://nominatim.org/) - Open-source geocoding with OpenStreetMap data
* [Bootstrap](https://getbootstrap.com/) - Frontend toolkit
* [Argus](https://github.com/MichaelYagi/argus) - Self hosted REST API for face and object recognition
* [Deep Java Library](https://djl.ai/) - Bundled face/object recognition library (legacy; recognition is now handled by Argus)
* [Croppie](https://foliotek.github.io/Croppie/) - Image cropping Javascript plugin
* [Chart.js](https://www.chartjs.org/) - Javascript charting library
