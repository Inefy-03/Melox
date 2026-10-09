#!/usr/bin/env python3
"""Rebuild the pinned, blur-only arm64 Toolkit AAR (Python 3.12+, JDK 21)."""

import argparse
import hashlib
import io
import os
from pathlib import Path
import re
import shutil
import subprocess
import tarfile
import tempfile
import urllib.request


COMMIT = "344be3f6bf03fb6b63a80b36f08f8dccac59d784"
SOURCE_SHA256 = "1243a1cb118cbeb3de596f8c03aacba52b98758bd9c7ed57c87f215bc8ef8044"
ROOT = Path(__file__).resolve().parents[1]


def rebuild():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-archive", type=Path, help="Use a cached upstream tar.gz")
    parser.add_argument("--sdk", type=Path, default=os.environ.get("ANDROID_HOME"))
    args = parser.parse_args()
    sdk = args.sdk
    if sdk is None:
        for line in (ROOT / "local.properties").read_text().splitlines():
            if line.startswith("sdk.dir="):
                sdk = Path(line.partition("=")[2].replace("\\:", ":").replace("\\\\", "\\"))
    if sdk is None or not sdk.is_dir():
        parser.error("Provide --sdk or configure sdk.dir/ANDROID_HOME")

    archive = args.source_archive.read_bytes() if args.source_archive else urllib.request.urlopen(
        f"https://codeload.github.com/android/renderscript-intrinsics-replacement-toolkit/tar.gz/{COMMIT}"
    ).read()
    if hashlib.sha256(archive).hexdigest() != SOURCE_SHA256:
        raise ValueError("Upstream source archive checksum mismatch")

    with tempfile.TemporaryDirectory(prefix="melox-blur-") as directory:
        project = Path(directory)
        with tarfile.open(fileobj=io.BytesIO(archive), mode="r:gz") as bundle:
            bundle.extractall(project, filter="data")
        upstream = project / f"renderscript-intrinsics-replacement-toolkit-{COMMIT}"
        source = upstream / "renderscript-toolkit/src/main"
        cpp = project / "src/main/cpp"
        cpp.mkdir(parents=True)
        # Preserve the upstream blur kernel, NEON assembly and worker pool verbatim.
        for name in ["Blur.cpp", "Blur_advsimd.S", "TaskProcessor.cpp", "TaskProcessor.h",
                     "Utils.cpp", "Utils.h", "RenderScriptToolkit.cpp", "RenderScriptToolkit.h"]:
            shutil.copy2(source / "cpp" / name, cpp / name)
        jni = (source / "cpp/JniEntryPoints.cpp").read_text()
        jni = jni[:jni.index('extern "C" JNIEXPORT void JNICALL Java_com_google_android_renderscript_Toolkit_nativeColorMatrix(')]
        jni = re.sub(
            r'extern "C" JNIEXPORT void JNICALL Java_com_google_android_renderscript_Toolkit_nativeBlend(?:Bitmap)?\([\s\S]*?\n}\n',
            "", jni,
        )
        (cpp / "JniEntryPoints.cpp").write_text(jni)
        (cpp / "CMakeLists.txt").write_text('''cmake_minimum_required(VERSION 3.22.1)
project(renderscript-toolkit LANGUAGES C CXX ASM)
add_library(renderscript-toolkit SHARED Blur.cpp Blur_advsimd.S JniEntryPoints.cpp
    RenderScriptToolkit.cpp TaskProcessor.cpp Utils.cpp)
target_compile_features(renderscript-toolkit PRIVATE cxx_std_17)
target_compile_definitions(renderscript-toolkit PRIVATE ANDROID OC_ARM_ASM
    ARCH_ARM_USE_INTRINSICS ARCH_ARM64_USE_INTRINSICS ARCH_ARM64_HAVE_NEON)
target_link_libraries(renderscript-toolkit cpufeatures jnigraphics log)
include(AndroidNdkModules)
android_ndk_import_module_cpufeatures()
''')
        lines = (source / "java/com/google/android/renderscript/Toolkit.kt").read_text().splitlines(True)

        def section(first, last):
            return "".join(lines[first - 1:last])

        # These ranges refer to the checksum-pinned source, not an evolving branch.
        kotlin = section(1, 23) + "\n/** Blur-only subset of the AOSP Toolkit. */\nobject Toolkit {\n"
        kotlin += section(175, 202) + section(228, 239) + section(1090, 1113)
        kotlin += section(1132, 1150) + "}\n\n" + section(1439, 1446)
        kotlin += section(1474, 1501) + section(1525, 1564)
        kotlin = kotlin.replace("inputBitmap.height, inputBitmap.config)",
                                "inputBitmap.height, requireNotNull(inputBitmap.config))")
        java = project / "src/main/java/com/google/android/renderscript"
        java.mkdir(parents=True)
        (java / "Toolkit.kt").write_text(kotlin)
        (project / "src/main/AndroidManifest.xml").write_text('<manifest />\n')
        (project / "consumer-rules.pro").write_text('''-keep class com.google.android.renderscript.Range2d {
    int startX;
    int endX;
    int startY;
    int endY;
}
''')
        (project / "settings.gradle.kts").write_text('''pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "toolkit-blur"
''')
        (project / "build.gradle.kts").write_text('''plugins { id("com.android.library") version "9.4.1" }
android {
    namespace = "com.google.android.renderscript"
    compileSdk = 37
    ndkVersion = "29.0.14206865"
    defaultConfig {
        minSdk = 28
        ndk { abiFilters += "arm64-v8a" }
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild { cmake { arguments += "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON" } }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
}
''')
        (project / "local.properties").write_text(f"sdk.dir={sdk}\n")
        (project / "gradle.properties").write_text("org.gradle.jvmargs=-Xmx2048m\n")
        subprocess.run([str(ROOT / "gradlew"), "-p", str(project), "assembleRelease", "--console=plain"], check=True)
        output = ROOT / "app/libs/renderscript-toolkit-blur-344be3f-arm64-16k.aar"
        shutil.copy2(project / "build/outputs/aar/toolkit-blur-release.aar", output)
        print(f"AAR: {output}\nSHA-256: {hashlib.sha256(output.read_bytes()).hexdigest()}")


if __name__ == "__main__":
    rebuild()
