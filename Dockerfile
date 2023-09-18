# Docker multistage layer with a fatty jar stripped of unused rocskdb-instances
FROM eclipse-temurin:20.0.2_9-jre-alpine
ENV LANG="nb_NO.UTF-8"
ENV LC_ALL="nb_NO.UTF-8"
ENV TZ="Europe/Oslo"
RUN apk --update --no-cache add libstdc++
COPY /app/build/libs/app-all.jar app.jar
CMD ["java", "-XX:ActiveProcessorCount=2", "-jar", "app.jar"]

# use -XX:+UseParallelGC when 2 CPUs and 4G RAM.
# use G1GC when using more than 4G RAM and/or more than 2 CPUs
# use -XX:ActiveProcessorCount=2 if less than 1G RAM.