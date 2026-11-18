#!/usr/bin/env python3
"""Rebuild Melox's Miuix UI patch from the v0.9.5-beta01 source."""

import argparse
import hashlib
import re
import shutil
import subprocess
import tempfile
from pathlib import Path


UPSTREAM_COMMIT = "6d2554a850b8960e2616c968263ff64beb5ffb03"
UPSTREAM_VERSION = "0.9.5-beta01"
PATCH_VERSION = f"{UPSTREAM_VERSION}-melox"
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
            [str(work / "gradlew"), "-p", str(work), ":miuix-ui:assembleAndroidMain",
             "-Prelease", "--no-configuration-cache", "--max-workers=4"],
            cwd=project, check=True,
        )
        artifacts = list((work / "miuix-ui/build/outputs/aar").glob("*.aar"))
        assert len(artifacts) == 1, artifacts
        output = directory / "maven/top/yukonga/miuix/kmp/miuix-ui-android" / PATCH_VERSION
        output.mkdir(parents=True, exist_ok=True)
        name = f"miuix-ui-android-{PATCH_VERSION}"
        target = output / f"{name}.aar"
        shutil.copy2(artifacts[0], target)
        pom = (directory / f"upstream-ui-android-{UPSTREAM_VERSION}.pom").read_text()
        pom = re.sub(r"<!--.*?-->", "", pom, flags=re.DOTALL)
        pom = "\n".join(line.rstrip() for line in pom.splitlines()) + "\n"
        pom = pom.replace(f"<version>{UPSTREAM_VERSION}</version>", f"<version>{PATCH_VERSION}</version>", 1)
        (output / f"{name}.pom").write_text(pom)
        print(f"AAR SHA-256: {hashlib.sha256(target.read_bytes()).hexdigest()}")
        print(target)


if __name__ == "__main__":
    main()
