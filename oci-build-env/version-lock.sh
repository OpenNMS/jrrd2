#!/usr/bin/env bash
set -u -o pipefail

VCS_SOURCE="$(git remote get-url --push origin)"
VCS_REVISION="$(git describe --always)"
DATE="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
export VCS_SOURCE
export VCS_REVISION
export DATE
export BASE_IMAGE="ubuntu:noble-20251013"
export MAVEN_MAIN_VERSION="3"
export MAVEN_MINOR_VERSION="9.11"
export MAVEN_VERSION="${MAVEN_MAIN_VERSION}.${MAVEN_MINOR_VERSION}"
export JDK_MAJOR_VERSION="17"
export FPM_VERSION="1.15.1"
export CLOUDSMITH_CLI_VERSION="1.9.4"
