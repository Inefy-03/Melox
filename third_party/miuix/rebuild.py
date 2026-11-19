#!/usr/bin/env python3
"""Rebuild the pinned Miuix Android artifacts used by Melox."""

import argparse
import hashlib
import re
import shutil
import subprocess
import tempfile
from pathlib import Path


UPSTREAM_COMMIT = "58b0a3dd06f8ee83c1099a7ebee1183adce3f83c"
UPSTREAM_VERSION = "0.9.5-beta01"
PATCH_VERSION = f"{UPSTREAM_VERSION}-melox.2"
MODULES = ("ui", "blur", "preference")
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
        assert hashlib.sha256((work / RAIL_SOURCE).read_bytes()).hexdigest() == RAIL_SHA256
        subprocess.run(
            ["git", "apply", "--include=miuix-ui/*", str(directory / "navigation-rail-insets.patch")],
            cwd=work, check=True,
        )
        top_bar = work / RAIL_SOURCE.replace("NavigationRail.kt", "TopAppBar.kt")
        source = top_bar.read_text()
        old_limit = "var heightOffsetLimit = initialHeightOffsetLimit"
        assert source.count(old_limit) == 1
        top_bar.write_text(source.replace(old_limit, "var heightOffsetLimit by mutableFloatStateOf(initialHeightOffsetLimit)"))

        settings = work / "settings.gradle.kts"
        settings.write_text(
            "\n".join(
                line for line in settings.read_text().splitlines()
                if not line.startswith(('include(":example:', 'include(":docs:', 'include(":baselineprofile")'))
            ) + "\n",
        )
        build = work / "miuix-ui/build.gradle.kts"
        build.write_text(build.read_text().replace("    baselineProfile(projects.baselineprofile)\n", ""))
        local_properties = project / "local.properties"
        if local_properties.exists():
            sdk = next((line for line in local_properties.read_text().splitlines() if line.startswith("sdk.dir=")), None)
            if sdk:
                (work / "local.properties").write_text(sdk + "\n")
        subprocess.run(
            [str(work / "gradlew"), "-p", str(work),
             *(f":miuix-{module}:assembleAndroidMain" for module in MODULES),
             "-Prelease", "-Pkotlin.compiler.execution.strategy=in-process",
             "--no-configuration-cache", "--no-watch-fs", "--max-workers=4"],
            cwd=project, check=True,
        )
        install_artifacts(work, directory)


def install_artifacts(work, directory):
    for module in MODULES:
        artifacts = list((work / f"miuix-{module}/build/outputs/aar").glob("*.aar"))
        assert len(artifacts) == 1, artifacts
        artifact_id = f"miuix-{module}-android"
        output = directory / "maven/top/yukonga/miuix/kmp" / artifact_id / PATCH_VERSION
        output.mkdir(parents=True, exist_ok=True)
        name = f"{artifact_id}-{PATCH_VERSION}"
        target = output / f"{name}.aar"
        shutil.copy2(artifacts[0], target)
        pom = (directory / f"upstream-{module}-android-{UPSTREAM_VERSION}.pom").read_text()
        pom = re.sub(r"<!--.*?-->", "", pom, flags=re.DOTALL)
        pom = "\n".join(line.rstrip() for line in pom.splitlines()) + "\n"
        pom = pom.replace(f"<version>{UPSTREAM_VERSION}</version>", f"<version>{PATCH_VERSION}</version>", 1)
        (output / f"{name}.pom").write_text(pom)
        print(f"{target}: {hashlib.sha256(target.read_bytes()).hexdigest()}")


if __name__ == "__main__":
    main()
