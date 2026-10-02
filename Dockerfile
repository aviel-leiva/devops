FROM amazoncorretto:17
COPY ./target/seMethods-jar-with-dependencies.jar /tmp
WORKDIR /tmp
ENTRYPOINT ["java", "-jar", "seMethods-jar-with-dependencies.jar", "db:3306", "30000"]
