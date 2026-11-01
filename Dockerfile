# our base build image
FROM maven:3.9-eclipse-temurin-17 AS maven
WORKDIR /shashin
# copy the Project Object Model file
COPY ./pom.xml ./pom.xml
# fetch all dependencies
RUN mvn dependency:go-offline -B
# copy your other files
COPY ./src ./src
# files the package step copies next to the jar
COPY ./CHANGELOG.md ./shashin_create_release_notes.sh ./shashin_reset_password.sh ./
# build for release. Tests and the JavaScript tooling (npm) run in CI, so they are skipped here:
# the image only needs the jar, and package.json is not part of the build context.
RUN mvn -B -Pprod package -DskipTests -Dskip.npm -Dskip.installnodenpm && cp target/shashin-*.jar shashin.jar

# smaller, final base image
FROM eclipse-temurin:17-jre
# OPTIONAL: copy dependencies so the thin jar won't need to re-download them
COPY --from=maven /root/.m2 /root/.m2
# set deployment directory
WORKDIR /shashin
# copy over the built artifact from the maven image
COPY --from=maven /shashin/shashin.jar ./shashin.jar
# set the startup command to run your binary
CMD ["java", "-jar", "/shashin/shashin.jar"]