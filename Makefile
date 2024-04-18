.DEFAULT_GOAL := shared-lib

SHELL               := /bin/bash -o nounset -o pipefail -o errexit
MAVEN_SETTINGS_XML  ?= ./.cicd-assets/settings.xml
BUILD_DIR           := ./build
DEB_PKG_BUILD_DIR   := $(BUILD_DIR)/deb
RPM_PKG_BUILD_DIR   := $(BUILD_DIR)/rpm
CMAKE_ARGS          ?=
VERSION             := $(shell cd java && mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
BUILD_NUMBER        ?= 0

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
	cd java && mvn --settings "../$(MAVEN_SETTINGS_XML)" clean compile
	cd build && cmake "$(CMAKE_ARGS)" ../jni/ && make

.PHONY: shared-lib
shared-lib: jni-header
	cd java && mvn --settings "../$(MAVEN_SETTINGS_XML)" package && \
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
      --vendor "Bluebird" \
      --license "GPLv2" \
      --maintainer "maintainer@bluebirdlabs.tech" \
      --url "https://github.com/Bluebird-Community/jrrd2" \
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
      --vendor "Bluebird" \
      --license "GPLv2" \
      --maintainer "maintainer@bluebirdlabs.tech" \
      --url "https://github.com/Bluebird-Community/jrrd2" \
      --version $(VERSION) \
      -t rpm \
      -C $(RPM_PKG_BUILD_DIR) \
      -d "rrdtool > 1.5.0" \
      -p ./dist --iteration $(BUILD_NUMBER)

.PHONY: clean
clean:
	cd java && mvn --settings "../$(MAVEN_SETTINGS_XML)" clean
	rm -rf build
	rm -rf dist
	rm -f jni/include/config.h
	rm -f jni/include/jrrd2_java_interface.h
