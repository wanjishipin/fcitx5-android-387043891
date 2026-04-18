# fcitx5-android Termux 专用修复版
# 优先级：Gradle 传递的 CMake 变量 → 环境变量 → Termux 实际路径 → 原有 fallback

if(DEFINED ECM_DIR)
    set(ECM_DIR "${ECM_DIR}")
elseif(DEFINED ENV{ECM_DIR})
    set(ECM_DIR "$ENV{ECM_DIR}")
elseif(CMAKE_HOST_WIN32)
    set(ECM_DIR "C:/msys64/ucrt64/share/ECM/cmake")
elseif(CMAKE_HOST_APPLE)
    if(CMAKE_HOST_SYSTEM_PROCESSOR STREQUAL "arm64")
        set(ECM_DIR /opt/homebrew/share/ECM/cmake)
    else()
        set(ECM_DIR /usr/local/share/ECM/cmake)
    endif()
else()
    # Termux 实际路径（关键修复）
    set(ECM_DIR "/data/data/com.termux/files/usr/share/ECM/cmake")
endif()

find_package(ECM REQUIRED CONFIG)
