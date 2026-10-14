# Dependencies

Every dependency must be justified here before being added. No network or analytics library is allowed in the data pipeline.

| Dependency | Version | Reason | Alternative |
| --- | --- | --- | --- |
| Compose BOM / `ui` / `foundation` | 2024.10.01 | UI toolkit. No Material library: components are built on `foundation` and Canvas. | Views/XML |
| `activity-compose` | 1.9.3 | `setContent` entry point and edge-to-edge support. | None practical |
| `core-ktx` | 1.13.1 | Kotlin extensions for the Android framework. | Plain framework APIs |
| Room (`runtime`, `ktx`, `compiler` via KSP) | 2.8.5 | Local SQLite storage with versioned migrations. | Raw SQLite |
| `datastore-preferences` | 1.1.1 | Settings storage. | SharedPreferences |
| `work-runtime-ktx` | 2.10.0 | Periodic background step reading without Google services. | AlarmManager |
| KSP plugin | 2.3.6 | Room annotation processing. | KAPT (deprecated) |
| JUnit | 4.13.2 | Unit tests. | JUnit 5 |

Build tooling: Android Gradle Plugin 9.4.1, Kotlin 2.2.10, Gradle 9.6.0 (see `gradle/libs.versions.toml`).
