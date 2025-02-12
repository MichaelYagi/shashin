[![CircleCI](https://circleci.com/gh/MichaelYagi/shashin/tree/main.svg?style=shield&circle-token=a7f7505f8d0357fbae3ca5be7a41ff8debed1d1d)](https://circleci.com/gh/MichaelYagi/shashin)

# Shashin
An Image Gallery inspired by Google Photos

<img src="https://michaelyagi.github.io/images/shashinss.png" alt="shashin"/>

# Versions
Must be updated in POM file and git tag with format v[Major][Minor][Patch].

# Maven Install
Run ```mvn clean install``` with unminified JS assets. Run ```mvn -Pprod clean install``` to run with minified JS assets. These also download and installs libraries needed for face recognition.

# Pipeline
Built on CircleCI with push to master at:

https://app.circleci.com/pipelines/github/MichaelYagi/shashin

To avoid triggering a CircleCI pipeline, ensure commit messages to master contain ```[ci skip]```.

Eg. ```git commit -m "[ci skip] Updated README"```

See this [help](https://circleci.com/docs/skip-build/) article.

# Docker
```mvn -Pprod clean install```

```docker build -t michaeltyagi/shashin .```

```docker run -d -p 6624:6624 michaeltyagi/shashin```

Published to https://hub.docker.com/repository/docker/michaeltyagi/shashin

# Development
To use unminified JS assets, set the following VM options:
```-Dspring.profiles.active=dev```

## Frontend Debugging
In the browser console, use ```shashin.enableDebug()``` to debug the frontend. You can pass different options:

| Option          | Value                                                                                                                                       | Default                          | Description           |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|-----------------------|
| ```filter```    | Array of ```shashin.consoleTypes.log```, ```shashin.consoleTypes.error```, ```shashin.consoleTypes.info```, ```shashin.consoleTypes.warn``` | ```[shashin.consoleTypes.log]``` | Include log types     |
| ```showTrace``` | ```boolean```                                                                                                                               | ```false```                      | Show stack trace      |
| ```writeLog```  | ```boolean```                                                                                                                               | ```false```                      | Write to backend logs |

eg. ```shashin.enableDebug({
            filter:[shashin.consoleTypes.log,shashin.consoleTypes.error],
            showTrace:true,
            writeLog:true}
    )```

Example above dumps the stack trace and writes log and error to the backend logs.

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
* [CompreFace](https://github.com/exadel-inc/CompreFace) - Self hosted REST API for face recognition
* [Deep Java Library](https://djl.ai/) - Face and object recognition library
* [Croppie](https://foliotek.github.io/Croppie/) - Image cropping Javascript plugin
