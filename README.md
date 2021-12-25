[![CircleCI](https://circleci.com/gh/MichaelYagi/shashin/tree/main.svg?style=svg&circle-token=6dc6f05ef637006f89e253fe7e3eb3d58047d173)](https://circleci.com/gh/MichaelYagi/shashin/tree/main)

# Shashin
An Image Gallery

# Version
Must be updated in POM file

# Install
Run ```mvn -Pprod clean install``` to run with minified JS assets.

# Docker
```mvn -Pprod clean install```

```docker build -t michaeltyagi/shashin .```

```docker run -d -p 6624:6624 michaeltyagi/shashin```

Published to https://hub.docker.com/repository/docker/michaeltyagi/shashin