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

target_include_directories(
    core-engine
    PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
)

