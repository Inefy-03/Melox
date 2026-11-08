#!/usr/bin/env python3
"""Rebuild Melox's Miuix Android patch from the pinned upstream source."""

import argparse
import hashlib
import re
import shutil
import subprocess
import tempfile
from pathlib import Path


UPSTREAM_COMMIT = "b7a5f08f"
PATCH_VERSION = "0.9.4.2-melox"
RAIL_SOURCE = "miuix-ui/src/commonMain/kotlin/top/yukonga/miuix/kmp/basic/NavigationRail.kt"
RAIL_SHA256 = "231daca7b4ba8352092a23cae6376eddbb73e97938dd9117c08c8e691d7f8e7f"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("upstream", type=Path, help="Local clone of compose-miuix-ui/miuix")
    args = parser.parse_args()
    directory = Path(__file__).resolve().parent
    project = directory.parent.parent
    archive = subprocess.check_output(
        ["git", "-C", str(args.upstream.resolve()), "archive", UPSTREAM_COMMIT],
    )
    with tempfile.TemporaryDirectory(prefix="melox-miuix-") as temporary:
        work = Path(temporary)
        subprocess.run(["tar", "-x", "-C", str(work)], input=archive, check=True)
        (work / "MEMORY.md").write_text(
            "# Patch Build Context\n\nIsolated pinned Miuix Glass source for Melox.\n"
            "Preserve original public APIs and native rail drawing.\n",
        )
        assert hashlib.sha256((work / RAIL_SOURCE).read_bytes()).hexdigest() == RAIL_SHA256
        subprocess.run(
            ["git", "apply", "--include=miuix-ui/*", str(directory / "navigation-rail-insets.patch")],
            cwd=work,
            check=True,
        )
        subprocess.run(
            ["git", "apply", str(directory / "overlay-dialog-binary-compat.patch")],
            cwd=work, check=True,
        )
        # Keep the shipped blur API and change only progressive downsampling.
        blur_archive = subprocess.check_output(
            ["git", "-C", str(args.upstream.resolve()), "archive", "39c40f99844227b853f0049a0933b1f3ae6c00ba", "miuix-blur/src"],
        )
        shutil.rmtree(work / "miuix-blur/src")
        subprocess.run(["tar", "-x", "-C", str(work)], input=blur_archive, check=True)
        subprocess.run(["git", "apply", str(directory / "progressive-blur-downsample.patch")], cwd=work, check=True)
        nav_archive = subprocess.check_output(["git", "-C", str(args.upstream.resolve()), "archive", "39c40f99844227b853f0049a0933b1f3ae6c00ba", "miuix-nav/src"])
        shutil.rmtree(work / "miuix-nav/src")
        subprocess.run(["tar", "-x", "-C", str(work)], input=nav_archive, check=True)
        subprocess.run(["git", "apply", str(directory / "single-step-root-pop.patch")], cwd=work, check=True)
        # Build only library targets; demo versioning expects a full Git checkout.
        settings = work / "settings.gradle.kts"
        settings.write_text(
            "\n".join(
                line for line in settings.read_text().splitlines()
                if not line.startswith((
                    'include(":example:', 'include(":docs:', 'include(":baselineprofile")',
                ))
            ) + "\n",
        )
        # Preserve Melox's existing palette API and color behavior.
        for theme_file in ("ThemeController.kt", "MiuixTheme.kt"):
            theme_path = "miuix-ui/src/commonMain/kotlin/top/yukonga/miuix/kmp/theme/" + theme_file
            (work / theme_path).write_bytes(subprocess.check_output(
                ["git", "-C", str(args.upstream.resolve()), "show",
                 "39c40f99844227b853f0049a0933b1f3ae6c00ba:" + theme_path],
            ))
        dynamic_colors = "miuix-ui/src/androidMain/kotlin/top/yukonga/miuix/kmp/theme/DynamicColors.android.kt"
        (work / dynamic_colors).write_bytes(subprocess.check_output(
            ["git", "-C", str(args.upstream.resolve()), "show",
             "39c40f99844227b853f0049a0933b1f3ae6c00ba:" + dynamic_colors],
        ))
        catalog = work / "gradle/libs.versions.toml"
        catalog.write_text(catalog.read_text().replace('"1.12.2"', '"1.12.0"')
                           .replace('kotlin = "2.4.21"', 'kotlin = "2.4.20"')
                           .replace('agp = "9.5.1"', 'agp = "9.4.1"')
                           .replace('materialKolor-utilities = "6.0.0"', 'materialKolor-utilities = "5.0.1"'))
        catalog.write_text(catalog.read_text().replace('kotlinx-serialization = "1.12.0"', 'kotlinx-serialization = "1.11.0"').replace('kotlinx-collections-immutable = "0.5.2"', 'kotlinx-collections-immutable = "0.5.1"'))
        build = work / "miuix-ui/build.gradle.kts"
        content = build.read_text()
        baseline_dependency = '    baselineProfile(projects.baselineprofile)\n'
        assert content.count(baseline_dependency) == 1
        build.write_text(content.replace(baseline_dependency, ""))
        local_properties = project / "local.properties"
        if local_properties.exists():
            sdk = next(
                (line for line in local_properties.read_text().splitlines()
                 if line.startswith("sdk.dir=")),
                None,
            )
            if sdk:
                (work / "local.properties").write_text(sdk + "\n")
        subprocess.run(
            [str(work / "gradlew"), "-p", str(work),
             ":miuix-ui:assembleAndroidMain", ":miuix-blur:assembleAndroidMain", ":miuix-nav:assembleAndroidMain", ":miuix-nav:generatePomFileForAndroidPublication", "-Prelease", "--no-configuration-cache"],
            cwd=project,
            check=True,
        )
        subprocess.run(
            [str(work / "gradlew"), "-p", str(work), ":miuix-nav:desktopTest",
             "--tests", "top.yukonga.miuix.kmp.nav.runtime.NavPresentationMathTest",
             "-Prelease", "--no-configuration-cache"], cwd=project, check=True,
        )
        artifacts = list((work / "miuix-ui/build/outputs/aar").glob("*.aar"))
        assert len(artifacts) == 1, artifacts
        output = directory / "maven/top/yukonga/miuix/kmp/miuix-ui-android" / PATCH_VERSION
        output.mkdir(parents=True, exist_ok=True)
        name = f"miuix-ui-android-{PATCH_VERSION}"
        target = output / f"{name}.aar"
        shutil.copy2(artifacts[0], target)
        pom = (directory / "upstream-ui-android.pom").read_text()
        pom = re.sub(r"<!--.*?-->", "", pom, flags=re.DOTALL)
        pom = "\n".join(line.rstrip() for line in pom.splitlines()) + "\n"
        pom = pom.replace("<version>0.9.4</version>", f"<version>{PATCH_VERSION}</version>", 1)
        pom = pom.replace("<name>miuix-ui</name>", "<name>Miuix UI - Melox local patch</name>")
        (output / f"{name}.pom").write_text(pom)
        print(f"AAR SHA-256: {hashlib.sha256(target.read_bytes()).hexdigest()}")
        print(target)
        blur_version = "0.9.4.1-melox"
        blur_output = directory / "maven/top/yukonga/miuix/kmp/miuix-blur-android" / blur_version
        blur_output.mkdir(parents=True, exist_ok=True)
        blur_name = f"miuix-blur-android-{blur_version}"
        blur_artifacts = list((work / "miuix-blur/build/outputs/aar").glob("*.aar"))
        assert len(blur_artifacts) == 1, blur_artifacts
        shutil.copy2(blur_artifacts[0], blur_output / f"{blur_name}.aar")
        (blur_output / f"{blur_name}.pom").write_text(
            (directory / "blur-android.pom").read_text()
        )
        print(f"Blur AAR SHA-256: {hashlib.sha256(blur_artifacts[0].read_bytes()).hexdigest()}")
        nav_version = "0.9.4.1-melox"
        nav_output = directory / "maven/top/yukonga/miuix/kmp/miuix-nav-android" / nav_version
        nav_output.mkdir(parents=True, exist_ok=True)
        nav_name = f"miuix-nav-android-{nav_version}"
        nav_artifacts = list((work / "miuix-nav/build/outputs/aar").glob("*.aar"))
        assert len(nav_artifacts) == 1, nav_artifacts
        shutil.copy2(nav_artifacts[0], nav_output / f"{nav_name}.aar")
        nav_pom = (work / "miuix-nav/build/publications/android/pom-default.xml").read_text()
        nav_pom = re.sub(r"<!--.*?-->", "", nav_pom, flags=re.DOTALL)
        # Keep retained dependency versions; only this module gets a local version.
        nav_pom = re.sub(r"(<artifactId>miuix-nav-android</artifactId>\s*<version>)[^<]+", r"\g<1>" + nav_version, nav_pom, count=1)
        nav_pom = re.sub(r"(<groupId>top.yukonga.miuix.kmp</groupId>\s*<artifactId>miuix-squircle[^<]*</artifactId>\s*<version>)[^<]+", r"\g<1>0.9.4", nav_pom)
        (nav_output / f"{nav_name}.pom").write_text(nav_pom)
        print(nav_output / f"{nav_name}.aar")


if __name__ == "__main__":
    main()
