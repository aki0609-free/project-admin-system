FROM eclipse-temurin:21-jdk-noble AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew \
    && ./gradlew --no-daemon dependencies

COPY src src

RUN ./gradlew --no-daemon clean bootJar \
    && java -Djarmode=tools -jar build/libs/application.jar \
        extract --layers --destination build/extracted

FROM eclipse-temurin:21-jre-noble AS runtime

ARG APP_UID=10001
ARG APP_GID=10001

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        python3 \
        python3-venv \
        curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid "${APP_GID}" projectadmin \
    && useradd --uid "${APP_UID}" --gid projectadmin \
        --home-dir /app --create-home --shell /usr/sbin/nologin projectadmin

COPY requirements-container.txt /tmp/requirements-container.txt

RUN python3 -m venv /opt/projectadmin-python \
    && /opt/projectadmin-python/bin/pip install --no-cache-dir \
        --requirement /tmp/requirements-container.txt \
    && /opt/projectadmin-python/bin/python -c \
        "import pandas, pdfplumber, requests" \
    && /opt/projectadmin-python/bin/python -m pip uninstall --yes pip \
    && apt-get purge -y --auto-remove \
        python3-venv \
        python3-pip-whl \
        python3-setuptools-whl \
        wget \
    && rm /tmp/requirements-container.txt

WORKDIR /app

RUN mkdir -p \
        /app/storage/imports/csv \
        /app/storage/imports/scripts \
        /app/storage/reports \
        /app/storage/reports-output \
        /tmp/projectadmin \
    && chown -R projectadmin:projectadmin \
        /app /app/storage /tmp/projectadmin

COPY --from=builder --chown=projectadmin:projectadmin \
    /workspace/build/extracted/dependencies/ ./
COPY --from=builder --chown=projectadmin:projectadmin \
    /workspace/build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=projectadmin:projectadmin \
    /workspace/build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=projectadmin:projectadmin \
    /workspace/build/extracted/application/ ./

COPY --from=builder --chown=projectadmin:projectadmin \
    /workspace/src/main/resources/imports/scripts/ /app/storage/imports/scripts/
COPY --from=builder --chown=projectadmin:projectadmin \
    /workspace/src/main/resources/reports/ /app/storage/reports/

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.io.tmpdir=/tmp/projectadmin" \
    PATH="/opt/projectadmin-python/bin:${PATH}" \
    PROJECT_IMPORTS_SCRIPT_PYTHON_COMMAND="/opt/projectadmin-python/bin/python" \
    PROJECT_STORAGE_LOCAL_BASE_PATH="/app/storage" \
    TZ="Asia/Tokyo"

USER projectadmin

EXPOSE 8080

STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "-jar", "application.jar"]
