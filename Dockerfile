# our base build image
FROM maven:3.6.0 as maven
WORKDIR /app
# copy the Project Object Model file
COPY ./pom.xml ./pom.xml
# fetch all dependencies
RUN mvn dependency:go-offline -B
# copy your other files
COPY ./src ./src
# build for release
RUN mvn package && cp target/shashin-*.jar app.jar

# smaller, final base image
FROM openjdk:11
# OPTIONAL: copy dependencies so the thin jar won't need to re-download them
COPY --from=maven /root/.m2 /root/.m2
# set deployment directory
WORKDIR /app
# copy over the built artifact from the maven image
COPY --from=maven /app/app.jar ./app.jar
# set the startup command to run your binary
CMD ["java", "-jar", "/app/app.jar"]