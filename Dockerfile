FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system --gid 10001 easenest \
    && useradd --system --uid 10001 --gid easenest --home-dir /app --shell /usr/sbin/nologin easenest \
    && mkdir -p /app/uploads \
    && chown -R easenest:easenest /app

COPY --chown=easenest:easenest target/zhuxiang-service-0.0.1-SNAPSHOT.jar /app/app.jar

USER easenest:easenest

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
