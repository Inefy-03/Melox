#!/usr/bin/env python3
"""Rebuild the pinned Miuix Android artifacts used by Melox."""

import argparse
import hashlib
import re
import shutil
import subprocess
import tempfile
from pathlib import Path


UPSTREAM_COMMIT = "08d52d455bf1d62c45e979ec75b47f6b5dc377f3"
UPSTREAM_VERSION = "0.9.5-beta01"
MODULES = ("ui", "blur", "preference")
MAVEN_GROUP = Path("maven/top/yukonga/miuix/kmp")
RAIL_SOURCE = "miuix-ui/src/commonMain/kotlin/top/yukonga/miuix/kmp/basic/NavigationRail.kt"
RAIL_SHA256 = "231daca7b4ba8352092a23cae6376eddbb73e97938dd9117c08c8e691d7f8e7f"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("upstream", type=Path, help="Local clone of compose-miuix-ui/miuix")
    args = parser.parse_args()
    directory = Path(__file__).resolve().parent
    project = directory.parent.parent
    catalog = (project / "gradle/libs.versions.toml").read_text()
    configured_version = re.search(r'^miuix\s*=\s*"([^"]+)"', catalog, re.MULTILINE)
    if configured_version is None or configured_version[1] != UPSTREAM_VERSION:
        parser.error(f"The Gradle Miuix version must match the pinned upstream version {UPSTREAM_VERSION}")
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
        prune_inactive_artifacts(directory)


def install_artifacts(work, directory):
    for module in MODULES:
        artifacts = list((work / f"miuix-{module}/build/outputs/aar").glob("*.aar"))
        assert len(artifacts) == 1, artifacts
        artifact_id = f"miuix-{module}-android"
        output = directory / MAVEN_GROUP / artifact_id / UPSTREAM_VERSION
        output.mkdir(parents=True, exist_ok=True)
        name = f"{artifact_id}-{UPSTREAM_VERSION}"
        target = output / f"{name}.aar"
        pom = (directory / f"upstream-{module}-android-{UPSTREAM_VERSION}.pom").read_text()
        pom = re.sub(r"<!--.*?-->", "", pom, flags=re.DOTALL)
        pom = "\n".join(line.rstrip() for line in pom.splitlines()) + "\n"
        with tempfile.TemporaryDirectory(dir=output) as temporary:
            staged = Path(temporary)
            shutil.copy2(artifacts[0], staged / target.name)
            (staged / f"{name}.pom").write_text(pom)
            (staged / target.name).replace(target)
            (staged / f"{name}.pom").replace(output / f"{name}.pom")
        print(f"{target}: {hashlib.sha256(target.read_bytes()).hexdigest()}")


def prune_inactive_artifacts(directory):
    repository = directory / MAVEN_GROUP
    for module in MODULES:
        artifact_id = f"miuix-{module}-android"
        active = repository / artifact_id / UPSTREAM_VERSION
        for extension in ("aar", "pom"):
            if not (active / f"{artifact_id}-{UPSTREAM_VERSION}.{extension}").is_file():
                raise FileNotFoundError(f"Missing active artifacts in {active}")

    for module in (*MODULES, "nav"):
        artifact_id = f"miuix-{module}-android"
        active_version = UPSTREAM_VERSION if module in MODULES else None
        for version_dir in (repository / artifact_id).glob("[0-9]*"):
            version = version_dir.name
            if version == active_version or not version_dir.is_dir() or version_dir.is_symlink():
                continue
            for extension in ("aar", "pom"):
                (version_dir / f"{artifact_id}-{version}.{extension}").unlink(missing_ok=True)
            if not any(version_dir.iterdir()):
                version_dir.rmdir()


if __name__ == "__main__":
    main()
