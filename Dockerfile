FROM gradle:7.5.1-jdk17-focal as cache
COPY build.gradle gradle.properties settings.gradle ./
RUN gradle --no-daemon resolveDependencies --stacktrace

FROM gradle:7.5.1-jdk17-focal AS builder
RUN apt-get update && apt-get install fakeroot -y
COPY --from=cache /root/.gradle /root/.gradle
COPY build.gradle gradle.properties settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/
RUN gradle --no-daemon jpackage --stacktrace

FROM scratch as release
COPY --from=builder /home/gradle/build/jpackage/linux/bbl_*.deb /build/jpackage/linux/
