cmake_minimum_required(VERSION 3.22.1)
project(oxide_lab_mobile)

# Set the path to the Rust project
set(RUST_PROJECT_DIR ${CMAKE_CURRENT_SOURCE_DIR})

# Find the cargo executable
find_program(CARGO_EXECUTABLE cargo)
if(NOT CARGO_EXECUTABLE)
    message(FATAL_ERROR "Cargo executable not found. Please install Rust.")
endif()

# Set the target directory for the built library
set(RUST_LIBRARY_TARGET ${CMAKE_CURRENT_BINARY_DIR}/liboxide_lab_mobile.so)

# Custom command to build the Rust library
add_custom_command(
    OUTPUT ${RUST_LIBRARY_TARGET}
    COMMAND ${CARGO_EXECUTABLE} ndk --target aarch64-linux-android --manifest-path ${RUST_PROJECT_DIR}/Cargo.toml -- build --release
    WORKING_DIRECTORY ${RUST_PROJECT_DIR}
    COMMENT "Building Rust library for Android"
)

# Custom target for the Rust library
add_custom_target(rust_library ALL
    DEPENDS ${RUST_LIBRARY_TARGET}
)

# Import the Rust library
add_library(oxide_lab_mobile SHARED IMPORTED)
set_target_properties(oxide_lab_mobile PROPERTIES
    IMPORTED_LOCATION ${RUST_LIBRARY_TARGET}
)

# Make sure the Rust library is built before the Android library
add_dependencies(oxide_lab_mobile rust_library)