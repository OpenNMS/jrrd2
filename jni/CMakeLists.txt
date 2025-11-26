cmake_minimum_required (VERSION 3.10.0)
project (jrrd2)

# JNI header name
set (JNI_HEADER "jrrd2_java_interface.h")

# Output folder
set (CMAKE_LIBRARY_OUTPUT_DIRECTORY "${PROJECT_BINARY_DIR}/../dist")

# Java JNI headers
find_package(JNI REQUIRED)
message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
include_directories(${JNI_INCLUDE_DIRS})

# librrd
INCLUDE(FindPkgConfig)
pkg_check_modules (LIBRRD REQUIRED librrd>=1.4.0)
message (STATUS "LIBRRD_LIBRARIES=${LIBRRD_LIBRARIES}")
message (STATUS "LIBRRD_LIBRARY_DIRS=${LIBRRD_LIBRARY_DIRS}")
if (NOT "${LIBRRD_LIBRARY_DIRS}" STREQUAL "")
  link_directories(${LIBRRD_LIBRARY_DIRS})
endif()
message (STATUS "LIBRRD_INCLUDE_DIRS=${LIBRRD_INCLUDE_DIRS}")
if (NOT "${LIBRRD_INCLUDE_DIRS}" STREQUAL "")
  include_directories(${LIBRRD_INCLUDE_DIRS})
endif()

# Generic includes
INCLUDE (CheckIncludeFiles)
CHECK_INCLUDE_FILES (unistd.h HAVE_UNISTD_H)
CHECK_INCLUDE_FILES (string.h HAVE_STRING_H)
CHECK_INCLUDE_FILES (stdlib.h HAVE_STDLIB_H)
CHECK_INCLUDE_FILES (malloc.h HAVE_MALLOC_H)
CHECK_INCLUDE_FILES (getopt.h HAVE_GETOPT_H)
CONFIGURE_FILE("${CMAKE_CURRENT_SOURCE_DIR}/include/config.h.in" "${CMAKE_CURRENT_SOURCE_DIR}/include/config.h")

# Turn all warnings into errors
IF(CMAKE_COMPILER_IS_GNUCC)
    SET(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror")
ENDIF(CMAKE_COMPILER_IS_GNUCC)

# Build the shared library
include_directories("${CMAKE_CURRENT_SOURCE_DIR}/include")
add_library(jrrd2 MODULE src/rrd_jinterface.c)

IF(WIN32)
	SET_TARGET_PROPERTIES(jrrd2 PROPERTIES PREFIX "" SUFFIX ".dll")
ENDIF(WIN32)

target_link_libraries(jrrd2 rrd)
