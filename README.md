# SwEnt Bootcamp: Android To-Do App

An Android app written in Kotlin and Jetpack Compose for the EPFL Software Enterprise (CS-311) bootcamp, autumn 2026.

- Google Sign-In via Firebase Authentication
- Per-user to-do lists stored in Cloud Firestore, with access-control rules
- Location-based to-dos, using Nominatim geocoding and Google Maps
- MVVM architecture with repository abstractions (Firestore and local)
- Instrumented and unit tests against the Firebase emulator, with JaCoCo coverage and ktfmt formatting, all run in GitHub Actions CI

Instructions come from [swent-epfl/public](https://github.com/swent-epfl/public/tree/main/bootcamp). To build, supply your own `google-services.json` and a Maps API key in `local.properties`; neither is included here.
