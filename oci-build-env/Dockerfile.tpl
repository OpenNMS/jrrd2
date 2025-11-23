###
# Do not edit the generated Dockerfile
###

# hadolint ignore=DL3006
FROM "${BASE_IMAGE}"

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

# hadolint ignore=DL3008
RUN apt-get update && \
    apt-get -y install --no-install-recommends \
        build-essential \
        ca-certificates \
        cmake \
        curl \
        git \
        gnupg \
        librrd-dev \
        openjdk-${JDK_MAJOR_VERSION}-jdk \
        pkg-config \
        python3-pip \
        rpm \
        ruby && \
    curl "https://dlcdn.apache.org/maven/maven-${MAVEN_MAIN_VERSION}/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -o /tmp/maven.tar.gz && \
    gem install "fpm:${FPM_VERSION}" && \
    # I got the failure "error: externally-managed-environment" pip install cloudsmith-cli
    # The only way I got it working without digging into the details of venv with Docker running with other user contexts was using
    # --break-system-packages ¯\_(ツ)_/¯
    pip install --break-system-packages --upgrade --no-cache-dir cloudsmith-cli=="${CLOUDSMITH_CLI_VERSION}" && \
    ln -s /usr/lib/jvm/java-1.${JDK_MAJOR_VERSION}* /usr/lib/jvm/java-${JDK_MAJOR_VERSION}-openjdk && \
    mkdir /opt/maven && \
    tar xzf /tmp/maven.tar.gz --strip-components=1 -C /opt/maven && \
    apt-get autoremove && \
    apt-get autoclean && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/*

### Runtime information and not relevant at build time
ENV PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/maven/bin
ENV M2_HOME=/opt/maven
ENV JAVA_HOME=/usr/lib/jvm/java-${JDK_MAJOR_VERSION}-openjdk

ENTRYPOINT ["/bin/bash"]

CMD ["-i"]

LABEL org.opencontainers.image.source="${VCS_SOURCE}" \
      org.opencontainers.image.revision="${VCS_REVISION}" \
      org.opencontainers.image.vendor="OpenNMS Community" \
      org.opencontainers.image.authors="maintainer@opennms.org" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.java.version="${JDK_MAJOR_VERSION}"
