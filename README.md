[![CircleCI](https://circleci.com/gh/MichaelYagi/shashin/tree/main.svg?style=shield&circle-token=a7f7505f8d0357fbae3ca5be7a41ff8debed1d1d)](https://circleci.com/gh/MichaelYagi/shashin/tree/main)

# Shashin
An Image Gallery inspired by Google Photos

# Version
Must be updated in POM file and git tag with format v[Major][Minor][Patch].

# Maven Install
Run ```mvn clean install``` with unminified JS assets. Run ```mvn -Pprod clean install``` to run with minified JS assets.

# Pipeline
Built on CircleCI with every push to master at:

https://app.circleci.com/pipelines/github/MichaelYagi/shashin

# Docker
```mvn -Pprod clean install```

```docker build -t michaeltyagi/shashin .```

```docker run -d -p 6624:6624 michaeltyagi/shashin```

Published to https://hub.docker.com/repository/docker/michaeltyagi/shashin

# Development
To use unminified JS assets, set the following VM options:
```-Dspring.profiles.active=dev```

Frameworks
* [Spring Boot](https://spring.io/) - Java framework to create micro services and web apps
* [Thymeleaf](https://www.thymeleaf.org/) - Server-side Java template engine for both web and standalone environments

Tech Stack
* [Kotlin](https://kotlinlang.org/)
* Javascript

3rd Party Libraries used:
* [lightGallery](https://www.lightgalleryjs.com/) and [plugins](https://cdnjs.com/libraries/lightgallery) - Image and video lightbox
* [Metadata Extractor](https://github.com/drewnoakes/metadata-extractor) - Extracts Exif, IPTC, XMP, ICC and other metadata from image, video and audio files
* [JQuery](https://jquery.com/) - JavaScript library
* [OpenLayers](https://openlayers.org/) with [OpenStreetMap](https://www.openstreetmap.org/) - Display map tiles, vector data and markers loaded from any source, like Open Street Maps
* [Bootstrap](https://getbootstrap.com/) - Frontend toolkit
* [CompreFace](https://github.com/exadel-inc/CompreFace) - Self hosted REST API for face recognition

See [folders.js](https://github.com/MichaelYagi/shashin/blob/main/src/main/resources/static/js/site/folders.js) for an example of JS templating and pagination.
