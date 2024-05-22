cmake_minimum_required(VERSION 3.22.1)

project("myapplication")

# 定义 libgpiod 目标并设置为 IMPORTED 类型
add_library(libgpiod SHARED IMPORTED)

# 设置 libgpiod 的 IMPORTED_LOCATION 属性
set_target_properties(libgpiod
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jni/${CMAKE_ANDROID_ARCH_ABI}/libgpiod.so)

# 添加你的项目的目标
add_library(${CMAKE_PROJECT_NAME}
        SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        native-lib.cpp)

# 将 libgpiod 链接到你的项目中
target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        ${log-lib}
        libgpiod
        log)
