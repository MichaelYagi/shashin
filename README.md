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
Framework
* [Spring Boot](https://spring.io/)
* [Thymeleaf](https://www.thymeleaf.org/)

Tech Stack
* [Kotlin](https://kotlinlang.org/)
* Javascript

3rd Party Libraries used:
* Modified [LightGallery](https://www.lightgalleryjs.com/) - Image and video lightbox
* [Metadata Extractor](https://github.com/drewnoakes/metadata-extractor) - Extracts Exif, IPTC, XMP, ICC and other metadata from image, video and audio files
* [TensorFlow](https://www.tensorflow.org/) - Build machine learning applications
* [JQuery](https://jquery.com/) - JavaScript library
* [OpenLayers](https://openlayers.org/) with [OpenStreetMap](https://www.openstreetmap.org/) - Display map tiles, vector data and markers loaded from any source, like Open Street Maps
* [Bootstrap](https://getbootstrap.com/) - Frontend toolkit

See [folders.js](https://github.com/MichaelYagi/shashin/blob/main/src/main/resources/static/js/site/folders.js) for an example of JS templating and pagination.
