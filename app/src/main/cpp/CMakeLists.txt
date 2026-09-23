cmake_minimum_required(VERSION 3.10)
project(chords_app_native C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 11)
set(PROJECT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../..")
set(WHISPER_ROOT "${PROJECT_ROOT}/third_party/whisper.cpp")

if(NOT EXISTS "${WHISPER_ROOT}/src/whisper.cpp")
    message(FATAL_ERROR "Missing third_party/whisper.cpp. Run tools/fetch-whisper.sh first.")
endif()

set(GGML_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_BLAS OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_ACCELERATE OFF CACHE BOOL "" FORCE)
set(GGML_CCACHE OFF CACHE BOOL "" FORCE)

add_subdirectory("${WHISPER_ROOT}/ggml" "${CMAKE_BINARY_DIR}/ggml")

add_library(whisper SHARED
    "${WHISPER_ROOT}/src/whisper.cpp"
    whisper_jni.c
    chord/chord.c
    chord/chord_hmm.c
    chord/chord_jni.c
)

target_compile_definitions(whisper PRIVATE GGML_USE_CPU WHISPER_VERSION="1.9.4")
target_compile_options(whisper PRIVATE -O3 -fvisibility=hidden -ffunction-sections -fdata-sections)
target_link_options(whisper PRIVATE -Wl,--gc-sections)

if(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_options(whisper PRIVATE -mfpu=neon-vfpv4)
endif()

target_include_directories(whisper PRIVATE
    "${WHISPER_ROOT}"
    "${WHISPER_ROOT}/include"
    "${WHISPER_ROOT}/src"
    "${WHISPER_ROOT}/ggml/include"
    "${WHISPER_ROOT}/ggml/src"
    "${WHISPER_ROOT}/ggml/src/ggml-cpu"
    "${CMAKE_CURRENT_SOURCE_DIR}/chord"
)

find_library(log-lib log)
find_library(android-lib android)
target_link_libraries(whisper ${log-lib} ${android-lib} ggml)
