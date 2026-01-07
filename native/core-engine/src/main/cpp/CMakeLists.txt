cmake_minimum_required(VERSION 3.22.1)

project("core-engine")

# C++17 standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Source files
add_library(
    core-engine
    SHARED
    engine.cpp
    engine.h
    playback_state.cpp
    playback_state.h
)

# Include directories
target_include_directories(
    core-engine
    PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
)

