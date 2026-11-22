# Detached-owner cleanup patch for Compose UI

Melox uses `androidx.compose.ui:ui-android:1.13.0-rc01-melox`, a local build of
AndroidX Compose UI `1.13.0-rc01` with the detached-owner cleanup fix.

## Reason and scope

The supplied release crash retraces to lazy-list prefetch cancellation disposing
a precomposed slot after its old layout root has lost its owner. The private
`LayoutNodeSubcompositionsState.outOfFrameExecutor` getter calls `requireOwner()`
even though its result is nullable and the caller already supports synchronous
cleanup when no executor is available.

The sole behavior change is equivalent to this source change in
`commonMain/androidx/compose/ui/layout/SubcomposeLayout.kt`:

```diff
-        get() = root.requireOwner().outOfFrameExecutor
+        get() = root.owner?.outOfFrameExecutor
```

An attached root still forwards the same owner's executor. A detached root
returns null, selecting the existing synchronous composition-deactivation path.
Normal prefetch remains enabled. No production reflection, global exception
handler, list-state replacement, or other framework changes are introduced.

## Provenance and license

- [Official upstream AAR](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.13.0-rc01/ui-android-1.13.0-rc01.aar)
- [Official upstream POM](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.13.0-rc01/ui-android-1.13.0-rc01.pom)
- [Official upstream sources](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.13.0-rc01/ui-android-1.13.0-rc01-sources.jar)

AndroidX is authored by the Android Open Source Project and licensed under
Apache-2.0; see `LICENSE`. Original AAR/JAR entries, including metadata and
notices, are preserved. `upstream.json` pins the original AAR, POM and target
class hashes. `subcompose-detached-owner.patch` records the source equivalent;
`PatchSubcomposeOwner.java` performs the narrowly scoped bytecode transformation.

## Rebuild and validation

Use JDK 21, Python 3, and ASM/ASM Tree 9.9. Supply the original, unmodified AAR
and POM, plus the two ASM JARs:

```sh
python3 third_party/compose-ui/rebuild.py \
  --aar /path/to/ui-android-1.13.0-rc01.aar \
  --pom third_party/compose-ui/upstream-ui-android.pom \
  --asm /path/to/asm-9.9.jar \
  --asm-tree /path/to/asm-tree-9.9.jar \
  --java-home /path/to/jdk-21
```

The script rejects unexpected upstream hashes, requires exactly one original
`requireOwner` call in the target getter, and verifies that every other JAR
entry is byte-identical. It also compares disassembled method bodies within
the changed class: only the getter may differ. The original POM's dependency
versions remain intact; only the local artifact version and unavailable Gradle
metadata marker change.

The generated AAR SHA-256 is
`158e5812af521d333e9736edce950fbc0693e1e63610c66ab8fb6ab9c1d933ec`.
`verification.json` records the checks. The local Maven artifact is bundled
as an input to ordinary app builds; ASM is not an app dependency.

`SubcomposeLayoutOwnerTest` exercises the actual framework getter for detached,
attached and subsequently detached owners. Test-only constructor bypass avoids
Android drawing stubs. The original AAR fails the two detached cases with the
reported exception and passes the attached case. This isolates the reported
failure without claiming device-level navigation or rendering coverage.
The patched artifact passes all three cases, including the unchanged
attached-owner control.
