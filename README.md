[![CircleCI](https://circleci.com/gh/MichaelYagi/shashin/tree/main.svg?style=shield&circle-token=a7f7505f8d0357fbae3ca5be7a41ff8debed1d1d)](https://circleci.com/gh/MichaelYagi/shashin/tree/main)

# Shashin
An Image Gallery inspired by Google Photos

# Version
Must be updated in POM file and git tag with format v[Major][Minor][Patch].

# Maven Install
Run ```mvn clean install``` with unminified JS assets. Run ```mvn -Pprod clean install``` to run with minified JS assets.

# Pipeline
Built on CircleCI with every push to master at:

https://app.circleci.com/pipelines/github/MichaelYagi/shashin?filter=all

Artifacts with new tags will be published to artifactory at:

https://shashin.jfrog.io/ui/native/shashin/

JAR, EXE and test output published.

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
* Heavily modified [LightGallery](https://www.lightgalleryjs.com/)
* [Metadata Extractor](https://github.com/drewnoakes/metadata-extractor)
* [TensorFlow](https://www.tensorflow.org/)
* [JQuery](https://jquery.com/)

See src\main\resources\static\js\site\folders.js for an example of JS templating and pagination.
