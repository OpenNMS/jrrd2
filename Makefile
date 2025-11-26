.DEFAULT_GOAL := shared-lib

SHELL               := /bin/bash -o nounset -o pipefail -o errexit
BUILD_DIR           := ./build
DEB_PKG_BUILD_DIR   := $(BUILD_DIR)/deb
RPM_PKG_BUILD_DIR   := $(BUILD_DIR)/rpm
CMAKE_ARGS          ?=
VERSION             := $(shell cd java && mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
BUILD_NUMBER        ?= 0

GIT_BRANCH          := $(shell git branch --show-current)
RELEASE_VERSION     := UNSET.0.0
RELEASE_BRANCH      := main
MAJOR_VERSION       := $(shell echo $(RELEASE_VERSION) | cut -d. -f1)
MINOR_VERSION       := $(shell echo $(RELEASE_VERSION) | cut -d. -f2)
PATCH_VERSION       := $(shell echo $(RELEASE_VERSION) | cut -d. -f3)
SNAPSHOT_VERSION    := $(MAJOR_VERSION).$(MINOR_VERSION).$(shell expr $(PATCH_VERSION) + 1)-SNAPSHOT
RELEASE_LOG         := $(BUILD_DIR)/release.log
OK                  := "[ 👍 ]"

.PHONY: help
help:
	@echo ""
	@echo "Jrrd2 provides a native interface for Java to rrdtool."
	@echo "Artifacts are generated in the dist directory."
	@echo ""
	@echo "Goals:"
	@echo "  help:          Show this help for build goals"
	@echo "  deps-build:    Verify build tools to compile from source"
	@echo "  deps-packages: Verify build tools to build RPM and DEB packages"
	@echo "  jni-header:    Generate JNI header"
	@echo "  shared-lib:    Compile shared lib for jrrd2 using the JNI header, (Default Goal)"
	@echo "  deb-pkg:       Generate Debian package using fpm"
	@echo "  rpm-pkg:       Generate RPM package using frpm"
	@echo "  clean:         Delete all build artifacts"
	@echo ""

.PHONY: deps-build
deps-build:
	@echo "Check build dependencies: Java JDK, Maven and cmake"
	command -v $(MAVEN_BIN)
	command -v java
	command -v javac
	command -v cmake
	mkdir -p $(BUILD_DIR)
	mkdir -p $(DEB_PKG_BUILD_DIR)
	mkdir -p $(RPM_PKG_BUILD_DIR)

.PHONY: deps-debian-pkg
deps-packages:
	@echo "Check dependencies to build Debian packages"
	command -v fpm

.PHONY: jni-header
jni-header: deps-build
	cd java && mvn clean compile
	cd build && cmake "$(CMAKE_ARGS)" ../jni/ && make

.PHONY: shared-lib
shared-lib: jni-header
	cd java && mvn package && \
	cp target/jrrd2-api-*.jar ../dist/

.PHONY: deb-pkg
deb-pkg: deps-packages shared-lib
	mkdir -p $(DEB_PKG_BUILD_DIR)/usr/lib/jni
	mkdir -p $(DEB_PKG_BUILD_DIR)/usr/share/java
	cp dist/libjrrd2.so $(DEB_PKG_BUILD_DIR)/usr/lib/jni/libjrrd2.so
	chmod 755 $(DEB_PKG_BUILD_DIR)/usr/lib/jni/libjrrd2.so
	cp dist/jrrd2-api-$(VERSION).jar $(DEB_PKG_BUILD_DIR)/usr/share/java/jrrd2.jar
	chmod 644 $(DEB_PKG_BUILD_DIR)/usr/share/java/jrrd2.jar
	fpm -s dir \
      --name jrrd2 \
      --description "A native interface to rrdtool for Java" \
      --vendor "OpenNMS Community" \
      --license "GPLv2" \
      --maintainer "maintainer@opennms.org" \
      --url "https://github.com/OpenNMS/jrrd2" \
      --version $(VERSION) \
      -t deb \
      -C $(DEB_PKG_BUILD_DIR) \
      -d "rrdtool > 1.5.0" \
      -p ./dist --iteration $(BUILD_NUMBER)

.PHONY: rpm-pkg
rpm-pkg: deps-packages shared-lib
	mkdir -p $(RPM_PKG_BUILD_DIR)/usr/lib64
	mkdir -p $(RPM_PKG_BUILD_DIR)/usr/share/java
	cp dist/libjrrd2.so $(RPM_PKG_BUILD_DIR)/usr/lib64/libjrrd2.so
	chmod 755 $(RPM_PKG_BUILD_DIR)/usr/lib64/libjrrd2.so
	cp dist/jrrd2-api-$(VERSION).jar $(RPM_PKG_BUILD_DIR)/usr/share/java/jrrd2.jar
	chmod 644 $(RPM_PKG_BUILD_DIR)/usr/share/java/jrrd2.jar
	fpm -s dir \
      --name jrrd2 \
      --description "A native interface to rrdtool for Java" \
      --vendor "OpenNMS Community" \
      --license "GPLv2" \
      --maintainer "maintainer@opennms.org" \
      --url "https://github.com/OpenNMS/jrrd2" \
      --version $(VERSION) \
      -t rpm \
      -C $(RPM_PKG_BUILD_DIR) \
      -d "rrdtool > 1.5.0" \
      -p ./dist --iteration $(BUILD_NUMBER)

.PHONY: clean
clean:
	cd java && mvn clean
	rm -rf build
	rm -rf dist
	rm -f jni/include/config.h
	rm -f jni/include/jrrd2_java_interface.h

.PHONY: release
release: deps-build
	@mkdir -p target
	@echo ""
	@echo "Release version:                $(RELEASE_VERSION)"
	@echo "New snapshot version:           $(SNAPSHOT_VERSION)"
	@echo "Git version tag:                v$(RELEASE_VERSION)"
	@echo "Current branch:                 $(GIT_BRANCH)"
	@echo "Release branch:                 $(RELEASE_BRANCH)"
	@echo "Release log file:               $(RELEASE_LOG)"
	@echo ""
	@echo -n "👮‍♀️ Check release branch:        "
	@if [ "$(GIT_BRANCH)" != "$(RELEASE_BRANCH)" ]; then echo "Releases are made from the $(RELEASE_BRANCH) branch, your branch is $(GIT_BRANCH)."; exit 1; fi
	@echo "$(OK)"
	@echo -n "👮‍♀️ Check uncommited changes     "
	@if git status --porcelain | grep -q .; then echo "There are uncommited changes in your repository."; exit 1; fi
	@echo "$(OK)"
	@echo -n "👮‍♀️ Check branch in sync         "
	@if [ "$(git rev-parse HEAD)" != "$(git rev-parse @{u})" ]; then echo "$(RELEASE_BRANCH) branch not in sync with remote origin."; exit 1; fi
	@echo "$(OK)"
	@echo -n "👮‍♀️ Check release version:       "
	@if [ "$(RELEASE_VERSION)" = "UNSET.0.0" ]; then echo "Set a release version, e.g. make release RELEASE_VERSION=1.0.0"; exit 1; fi
	@echo "$(OK)"
	@echo -n "👮‍♀️ Check version tag available: "
	@if git rev-parse v$(RELEASE_VERSION) >$(RELEASE_LOG) 2>&1; then echo "Tag v$(RELEASE_VERSION) already exists"; exit 1; fi
	@echo "$(OK)"
	@echo -n "💅 Set Maven release version:   "
	@cd java; mvn versions:set -DnewVersion=$(RELEASE_VERSION) >>../$(RELEASE_LOG) 2>&1
	@echo "$(OK)"
	@echo -n "👮‍♀️ Validate build:              "
	@$(MAKE) shared-lib >>$(RELEASE_LOG) 2>&1
	@echo "$(OK)"
	@echo -n "🎁 Git commit new release       "
	@git commit --signoff -am "release: JRRD2 version $(RELEASE_VERSION)" >>$(RELEASE_LOG) 2>&1
	@echo "$(OK)"
	@echo -n "🦄 Set Git version tag:         "
	@git tag -a "v$(RELEASE_VERSION)" -m "Release JRRD2 version $(RELEASE_VERSION)" >>$(RELEASE_LOG) 2>&1
	@echo "$(OK)"
	@echo -n "⬆️ Set Maven snapshot version:  "
	@cd java; mvn versions:set -DnewVersion=$(SNAPSHOT_VERSION) >>../$(RELEASE_LOG) 2>&1
	@echo "$(OK)"
	@echo -n "🎁 Git commit snapshot release: "
	@git commit --signoff -am "release: JRRD2 version $(SNAPSHOT_VERSION)" >>$(RELEASE_LOG) 2>&1
	@echo "$(OK)"
	@echo ""
	@echo "🦄 Congratulations! ✨"
	@echo "You made a release in your local repository."
	@echo "Publish the release by pushing the version tag"
	@echo "and the new snapshot version to the remote repo"
	@echo "with the following commands:"
	@echo ""
	@echo "  git push"
	@echo "  git push origin v$(RELEASE_VERSION)"
	@echo ""
	@echo "Thank you for computing with us."
	@echo ""