FROM gradle:6.6 as build

WORKDIR /build

COPY "gradle" "gradle"
COPY "build.gradle" "settings.gradle" "gradle.properties" "/build"/

RUN gradle build

COPY src src

RUN gradle build

FROM openjdk:14

WORKDIR /bot

COPY --from=build /build/build/libs/bot-kt-*.jar /bot/

RUN string1="$(ls bot-kt-*.jar)" && string2=${string1#"bot-kt-"} && echo ${string2%".jar"} > currentVersion

ENTRYPOINT java -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch -XX:+UseAdaptiveGCBoundary -XX:+UseGCOverheadLimit -XX:MaxHeapFreeRatio=80 -XX:MinHeapFreeRatio=40 -XX:-UseG1GC -XX:+UseZGC -XX:+DisableExplicitGC -XX:-UseParallelGC -jar bot-kt-$(cat currentVersion).jar
