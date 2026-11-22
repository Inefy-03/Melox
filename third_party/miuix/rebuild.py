#!/usr/bin/env python3
"""Rebuild the patched Miuix UI Android artifact used by Melox."""

import argparse
import hashlib
import re
import shutil
import subprocess
import tempfile
from pathlib import Path


UPSTREAM_COMMIT = "3c560868758f80cd97bbdab2f8e53612ad1c56be"
UPSTREAM_ARCHIVE_SHA256 = "e4fcca8e6c27abfbd6d5c5ea4f251af37462bdea01db72b61381e4e2b3dccff4"
UPSTREAM_VERSION = "0.9.5-beta04"
ARTIFACT_ID = "miuix-ui-android"
MAVEN_GROUP = Path("maven/top/yukonga/miuix/kmp")
RAIL_SOURCE = "miuix-ui/src/commonMain/kotlin/top/yukonga/miuix/kmp/basic/NavigationRail.kt"
RAIL_SHA256 = "9c9b84f84548db581e418488a874cfbbfa8054afca5eae45dd083f23b1f99842"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("upstream", type=Path, help="Upstream Git clone or pinned GitHub .tar.gz archive")
    args = parser.parse_args()
    directory = Path(__file__).resolve().parent
    project = directory.parent.parent
    catalog = (project / "gradle/libs.versions.toml").read_text()
    configured_version = re.search(r'^miuix\s*=\s*"([^"]+)"', catalog, re.MULTILINE)
    if configured_version is None or configured_version[1] != UPSTREAM_VERSION:
        parser.error(f"The Gradle Miuix version must match the pinned upstream version {UPSTREAM_VERSION}")
    upstream = args.upstream.resolve()
    with tempfile.TemporaryDirectory(prefix="melox-miuix-") as temporary:
        work = Path(temporary)
        if upstream.is_file():
            if hashlib.sha256(upstream.read_bytes()).hexdigest() != UPSTREAM_ARCHIVE_SHA256:
                parser.error(f"Expected the GitHub archive of {UPSTREAM_COMMIT}")
            subprocess.run(["tar", "-xzf", str(upstream), "-C", str(work), "--strip-components=1"], check=True)
        else:
            archive = subprocess.check_output(["git", "-C", str(upstream), "archive", UPSTREAM_COMMIT])
            subprocess.run(["tar", "-x", "-C", str(work)], input=archive, check=True)
        assert hashlib.sha256((work / RAIL_SOURCE).read_bytes()).hexdigest() == RAIL_SHA256
        subprocess.run(
            ["git", "apply", "--include=miuix-ui/*", str(directory / "navigation-rail-insets.patch")],
            cwd=work, check=True,
        )
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
             ":miuix-ui:assembleAndroidMain",
             "-Prelease", "-Pkotlin.compiler.execution.strategy=in-process",
             "--no-configuration-cache", "--no-watch-fs", "--max-workers=4"],
            cwd=project, check=True,
        )
        install_artifact(work, directory)


def install_artifact(work, directory):
    artifacts = list((work / "miuix-ui/build/outputs/aar").glob("*.aar"))
    assert len(artifacts) == 1, artifacts
    repository = directory / MAVEN_GROUP / ARTIFACT_ID
    output = repository / UPSTREAM_VERSION
    output.mkdir(parents=True, exist_ok=True)
    name = f"{ARTIFACT_ID}-{UPSTREAM_VERSION}"
    target = output / f"{name}.aar"
    pom = (directory / f"upstream-ui-android-{UPSTREAM_VERSION}.pom").read_text()
    pom = re.sub(r"<!--.*?-->", "", pom, flags=re.DOTALL)
    pom = "\n".join(line.rstrip() for line in pom.splitlines()) + "\n"
    with tempfile.TemporaryDirectory(dir=output) as temporary:
        staged = Path(temporary)
        shutil.copy2(artifacts[0], staged / target.name)
        (staged / f"{name}.pom").write_text(pom)
        (staged / target.name).replace(target)
        (staged / f"{name}.pom").replace(output / f"{name}.pom")
    print(f"{target}: {hashlib.sha256(target.read_bytes()).hexdigest()}")

    for version_dir in repository.glob("[0-9]*"):
        version = version_dir.name
        if version == UPSTREAM_VERSION or not version_dir.is_dir() or version_dir.is_symlink():
            continue
        for extension in ("aar", "pom"):
            (version_dir / f"{ARTIFACT_ID}-{version}.{extension}").unlink(missing_ok=True)
        if not any(version_dir.iterdir()):
            version_dir.rmdir()


if __name__ == "__main__":
    main()
