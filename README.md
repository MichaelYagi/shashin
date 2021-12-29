[![CircleCI](https://circleci.com/gh/MichaelYagi/shashin/tree/main.svg?style=shield&circle-token=6dc6f05ef637006f89e253fe7e3eb3d58047d173)](https://circleci.com/gh/MichaelYagi/shashin/tree/main)

# Shashin
An Image Gallery

# Version
Must be updated in POM file and git tag with format v<Major><Minor><patch>.

# Maven Install
Run ```mvn clean install``` with unminified JS assets. Run ```mvn -Pprod clean install``` to run with minified JS assets.

# Pipeline
Built on CircleCI with every push to master at:

```https://app.circleci.com/pipelines/github/MichaelYagi/shashin?filter=all```

Artifacts with new tags will be published to artifactory at:

```https://shashin.jfrog.io/ui/native/shashin/```

JAR, EXE and test output published.

# Docker
```mvn -Pprod clean install```

```docker build -t michaeltyagi/shashin .```

```docker run -d -p 6624:6624 michaeltyagi/shashin```

Published to https://hub.docker.com/repository/docker/michaeltyagi/shashin