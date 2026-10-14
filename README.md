# Steply

A local step counter with no account and exportable data, for de-Googled Android phones.

> Work in progress: only the project skeleton exists so far.

## Principles

- Local only: no account, no cloud, no `INTERNET` permission (checked by a unit test on the manifest).
- Step data stays on the device and can be exported and re-imported as CSV.
- Not a medical device.

## Stack

Kotlin, Jetpack Compose (no Material library), Room, DataStore, WorkManager. See [DEPENDENCIES.md](DEPENDENCIES.md).

## Structure

```
app/src/main/java/io/github/enterenn/steply/
├── core/   # dependency container, time, background
├── data/   # database, sensor, repositories, settings
└── ui/     # theme, components, screens
```

## Build

Requires JDK 17+ and the Android SDK (`minSdk` 26, `compileSdk` 35).

```
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

The version lives in `version.properties` (SemVer).

## License

Apache License 2.0, see [LICENSE](LICENSE).
