cmake_minimum_required(VERSION 3.22.1)

project("native-bridge")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Resolve path to core-engine C++ sources (5 levels up from this file)
set(CORE_ENGINE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../native/core-engine/src/main/cpp)

add_library(
    native-bridge
    SHARED
    jni_bridge.cpp
    ${CORE_ENGINE_DIR}/image_filter.cpp
    ${CORE_ENGINE_DIR}/audio_processor.cpp
)

target_include_directories(
    native-bridge
    PRIVATE
    ${CORE_ENGINE_DIR}
)

find_library(log-lib log)
target_link_libraries(native-bridge ${log-lib})

