FROM gcr.io/distroless/java21-debian13

ADD target/pmanagement-service.jar pmanagement-service.jar

EXPOSE 9081

ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "/pmanagement-service.jar"]