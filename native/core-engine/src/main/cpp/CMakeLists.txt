cmake_minimum_required(VERSION 3.22.1)

project("core-engine")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
    core-engine
    STATIC
    image_filter.cpp
    audio_processor.cpp
)

# Always optimize the compute kernels, even in debug builds. Without this the
# NDK compiles at -O0, which makes the per-pixel convolution several times
# slower than necessary.
target_compile_options(
    core-engine
    PRIVATE
    -O3
    -ffast-math
)

target_include_directories(
    core-engine
    PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
)

