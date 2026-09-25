# SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
# SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

cmake_minimum_required(VERSION 3.25)

project(ntm-next-native C CXX)

set(CMAKE_CXX_STANDARD 26)
set(CMAKE_CXX_STANDARD_REQUIRED YES)
set(CMAKE_C_STANDARD 11)

set(NTM_MATH_SOURCES
        src/math/exports.cpp
        src/math/paths/avx.cpp
        src/math/paths/avx512.cpp
        src/math/paths/neon.cpp
)

set(NTM_RADSIM_SOURCES
        src/radsim/abi.cpp
        src/radsim/runtime.cpp
        src/radsim/topology.cpp
        src/radsim/decay.cpp
        src/radsim/edits.cpp
        src/radsim/sweeps.cpp
        src/radsim/paths/sweeps_avx.cpp
        src/radsim/paths/sweeps_avx2.cpp
        src/radsim/paths/sweeps_avx512.cpp
        src/radsim/paths/sweeps_fma.cpp
        src/radsim/cpu_profile.cpp
)

add_library(ntm_next_native SHARED ${NTM_MATH_SOURCES} ${NTM_RADSIM_SOURCES})
set_target_properties(ntm_next_native PROPERTIES PREFIX "" OUTPUT_NAME "ntm_next_native")

target_include_directories(ntm_next_native PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/src)

if (CMAKE_CXX_COMPILER_ID MATCHES "Clang")
    include(CheckCXXCompilerFlag)
    foreach (flag -mno-gather -mno-scatter)
        string(MAKE_C_IDENTIFIER "NTM_HAS${flag}" supported)
        check_cxx_compiler_flag(${flag} ${supported})
        if (${supported})
            target_compile_options(ntm_next_native PRIVATE ${flag})
        endif ()
    endforeach ()
endif ()
target_compile_options(ntm_next_native PRIVATE -Wall -Wextra -Wundef -ffp-contract=off
        -fno-math-errno -fno-trapping-math)

get_filename_component(NTM_SOURCE_PREFIX ${CMAKE_CURRENT_SOURCE_DIR} DIRECTORY)
target_compile_options(ntm_next_native PRIVATE -ffile-prefix-map=${NTM_SOURCE_PREFIX}=.)
find_package(Threads REQUIRED)
target_link_libraries(ntm_next_native PRIVATE Threads::Threads)
if (MINGW)
    target_link_options(ntm_next_native PRIVATE -static-libgcc -static-libstdc++ -static
            -Wl,--no-insert-timestamp)
elseif (APPLE)
    set_target_properties(ntm_next_native PROPERTIES NO_SONAME ON)
    if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
        target_compile_options(ntm_next_native PRIVATE -mmacosx-version-min=${CMAKE_OSX_DEPLOYMENT_TARGET})
        target_link_options(ntm_next_native PRIVATE -static-libgcc -static-libstdc++ -nodefaultrpaths
                -mmacosx-version-min=${CMAKE_OSX_DEPLOYMENT_TARGET})
    endif ()
    target_link_options(ntm_next_native PRIVATE
            -Wl,-S -Wl,-x -Wl,-install_name,libntm_next_native.dylib)
else ()
    target_link_options(ntm_next_native PRIVATE -static-libgcc -static-libstdc++)
endif ()
