cmake_minimum_required(VERSION 3.22.1)

project("native-bridge")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Core engine source directory
set(CORE_ENGINE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../native/core-engine/src/main/cpp)

# Include core engine source files directly
# In a production setup, you might link to a pre-built library instead
add_library(
    native-bridge
    SHARED
    jni_bridge.cpp
    jni_bridge.h
    ${CORE_ENGINE_DIR}/engine.cpp
    ${CORE_ENGINE_DIR}/engine.h
    ${CORE_ENGINE_DIR}/playback_state.cpp
    ${CORE_ENGINE_DIR}/playback_state.h
)

target_include_directories(
    native-bridge
    PRIVATE
    ${CORE_ENGINE_DIR}
)

# Link Android log library
find_library(log-lib log)
target_link_libraries(
    native-bridge
    ${log-lib}
)

