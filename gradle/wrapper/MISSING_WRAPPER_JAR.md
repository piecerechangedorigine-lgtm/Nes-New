This directory is missing `gradle-wrapper.jar`.

`gradlew` and `gradlew.bat` (both now present, one directory up) are
the standard Gradle wrapper scripts — they're plain text and were
written out directly. `gradle-wrapper.jar` is a compiled binary
Gradle distributes, and it could not be generated in the environment
that produced this repository: that environment has no network access
(confirmed: outbound requests are rejected with `host_not_allowed`)
and no local Gradle installation to run `gradle wrapper` with.

To finish this, from a machine with either Gradle or network access,
in the project root:

```
gradle wrapper --gradle-version 8.10
```

or, if Gradle itself isn't installed locally, run any of the
`gradlew` commands in `README`/CI once this jar exists elsewhere and
is copied in — a fresh `git clone` of an Android Studio "New Project"
scaffold on any supported Gradle 8.10 version also has a byte-identical
copy of this file, since the wrapper jar doesn't vary by project.

Once `gradle-wrapper.jar` is in place, this file should be deleted —
its only purpose is to explain why the jar is missing, not to be a
permanent part of the repository.

See `FOUNDATION_COMPLETION_REPORT.md` and `ROADMAP_NEXT.md` for where
this was originally flagged as a real, tracked gap rather than an
oversight discovered only now.
