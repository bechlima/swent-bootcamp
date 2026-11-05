import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  jacoco
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.kotlinCompose)
  alias(libs.plugins.gms)
}

// ---------------------------------------------------------------------------
// Test selection flags
//
// Test classes are labelled by milestone (OverviewScreenB1Test, NavigationB2StaffTest,
// ToDosRepositoryLocalTestB1, ...), and staff tests additionally carry "Staff" in the class name.
// These flags narrow both the JVM unit tests and the instrumented tests, and they combine:
//
//   ./gradlew connectedCheck -B1            only the B1 instrumented tests
//   ./gradlew connectedCheck -staff         only the staff tests
//   ./gradlew connectedCheck -B2 -public    B2 without the staff tests
//   ./gradlew check -B1                     only the B1 unit tests
//
// Gradle's own CLI has no -B1 / -staff / -public options, so gradlew and gradlew.bat translate
// them into project properties (-PB1, -Pstaff, -Ppublic) before Gradle sees them. Outside the
// wrapper (Android Studio's Gradle panel, a system `gradle`) pass those properties directly;
// -Pmilestone=B1 is the long form of -PB1.
//
// With no flag the whole suite runs, so CI is unaffected.
// ---------------------------------------------------------------------------
val milestone: String? = run {
  val requested =
      if (project.hasProperty("milestone")) project.property("milestone")?.toString().orEmpty()
      else listOf("B1", "B2", "B3").firstOrNull { project.hasProperty(it) }
  requested?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
}
val staffOnly = project.hasProperty("staff")
val publicOnly = project.hasProperty("public")
val filtersActive = milestone != null || staffOnly || publicOnly

if (milestone != null) {
  require(Regex("^B[1-3]$").matches(milestone)) {
    "Unknown milestone '$milestone'. Use -B1, -B2 or -B3 (or -PB1 / -Pmilestone=B1)."
  }
}

require(!(staffOnly && publicOnly)) {
  "-staff and -public are opposites and cannot be combined; pass at most one of them."
}

/** True when the active flags keep the test class [fqcn]. */
fun selectedByFlags(fqcn: String): Boolean {
  val name = fqcn.substringAfterLast('.')
  if (milestone != null && !name.contains(milestone)) return false
  if (staffOnly && !name.contains("Staff")) return false
  if (publicOnly && name.contains("Staff")) return false
  return true
}

/**
 * Fully-qualified names of the top-level test classes declared under [sourceDir]. Only files that
 * actually hold a @Test are considered, which leaves out the shared helpers in utils/. The class
 * name is read from the source rather than inferred from the file name, because the two do not
 * always agree (AddToDoScreenB2StaffTest.kt declares AddToDoScreenB2StaffTestTest).
 */
fun testClassesIn(sourceDir: String): List<String> {
  val packageDecl = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
  val classDecl = Regex("""^(?:open\s+|abstract\s+)?class\s+(\w+)""", RegexOption.MULTILINE)
  val root = file(sourceDir)
  if (!root.isDirectory) return emptyList()
  return root
      .walkTopDown()
      .filter { it.isFile && it.extension == "kt" }
      .flatMap { source ->
        val text = source.readText()
        val packageName = packageDecl.find(text)?.groupValues?.get(1)
        if (packageName == null || !text.contains("@Test")) emptySequence()
        else
            classDecl
                .findAll(text)
                .map { it.groupValues[1] }
                .filter { it.contains("Test") }
                .map { "$packageName.$it" }
      }
      .sorted()
      .toList()
}

val instrumentedSelection =
    if (filtersActive) testClassesIn("src/androidTest/java").filter { selectedByFlags(it) }
    else emptyList()
val unitSelection =
    if (filtersActive) testClassesIn("src/test/java").filter { selectedByFlags(it) }
    else emptyList()

if (filtersActive) {
  val label =
      listOfNotNull(milestone, "staff".takeIf { staffOnly }, "public".takeIf { publicOnly })
          .joinToString(" ")
  logger.lifecycle(
      "Test filter [$label]: ${instrumentedSelection.size} instrumented and " +
          "${unitSelection.size} unit test class(es) selected."
  )
  instrumentedSelection.plus(unitSelection).forEach {
    logger.info("  selected: ${it.substringAfterLast('.')}")
  }
}

android {
  namespace = "com.github.se.bootcamp"
  compileSdk = 37

  // Load the API key from local.properties
  val localProperties = Properties()
  val localPropertiesFile = rootProject.file("local.properties")
  if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
  }

  val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY") ?: ""

  defaultConfig {
    applicationId = "com.github.se.bootcamp"
    minSdk = 29
    // Target Android 16 (API 36). Android 17's Local Network Protection is gated by targetSdk
    // and blocks the app from reaching the Firebase emulators on 10.0.2.2 when it targets 37;
    // pinning to 36 keeps the emulators reachable on any device image, including the recommended
    // API 37 emulator.
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    // ddmlib builds the on-device command as `am instrument ... -e <name> <value>` without
    // quoting the value, so this argument has to stay free of shell metacharacters. An explicit
    // class list is the safe form (letters, dots and commas only) and it also expresses
    // "everything except the staff tests", which no plain regex can do.
    if (filtersActive) {
      testInstrumentationRunnerArguments["class"] =
          if (instrumentedSelection.isEmpty()) "$namespace.NoTestMatchedTheSelectionFlags"
          else instrumentedSelection.joinToString(",")
    }
    vectorDrawables { useSupportLibrary = true }
    manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard-rules.pro",
      )
    }
    debug {
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      merges += "META-INF/LICENSE.md"
      merges += "META-INF/LICENSE-notice.md"
      excludes += "META-INF/LICENSE-notice.md"
      excludes += "META-INF/LICENSE.md"
      excludes += "META-INF/LICENSE"
      excludes += "META-INF/LICENSE.txt"
      excludes += "META-INF/NOTICE"
      excludes += "META-INF/NOTICE.txt"
      pickFirsts += "dispatcher.jar"
    }
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true

      isReturnDefaultValues = true
    }
    packaging { jniLibs { useLegacyPackaging = true } }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  lint {
    // B1 students construct a ViewModel in a composable, before DI is taught.
    // Keep the hint visible without failing their build.
    warning += "ViewModelConstructorInComposable"
  }

  // Robolectric needs to be run only in debug. But its tests are placed in the shared source set
  // (test)
  // The next lines transfers the src/test/* from shared to the testDebug one
  //
  // This prevents errors from occurring during unit tests
  sourceSets {
    getByName("test") {
      java.directories.add("src/test/java")
      resources.directories.add("src/test/resources")
    }

    getByName("testDebug") {
      java.directories.add("src/testDebug/java")
      resources.directories.add("src/testDebug/resources")
    }
  }
}

// With AGP 9+ we have to set the JVM target on a kotlin block outside the Android block.
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
  // Version alignment. Every androidx.compose.* and com.google.firebase artifact below
  // takes its version from one of these, so none of them declare one.
  implementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  androidTestImplementation(platform(libs.firebase.bom))

  // Core
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.kotlinx.serialization.json)

  // Jetpack Compose UI
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.material)
  implementation(libs.androidx.material3)
  testImplementation(libs.test.core.ktx)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
  implementation(libs.material)
  implementation(libs.androidx.material.icons.extended)

  // Navigation
  implementation(libs.androidx.navigation.compose)

  // Google Service and Maps
  implementation(libs.play.services.maps)
  implementation(libs.maps.compose)
  implementation(libs.maps.compose.utils)
  implementation(libs.play.services.auth)

  // Firebase
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)

  // Credential Manager (for Google Sign-In)
  implementation(libs.credentials)
  implementation(libs.credentials.play.services.auth)
  implementation(libs.googleid)

  // Networking with OkHttp
  implementation(libs.okhttp)

  // Testing Unit
  testImplementation(libs.junit)
  androidTestImplementation(libs.mockk.android)
  androidTestImplementation(libs.mockk.agent)
  testImplementation(libs.mockk)
  testImplementation(libs.json)

  // Test UI
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.espresso.intents)
  androidTestImplementation(libs.androidx.ui.test.junit4)
  testImplementation(libs.mockito.core)
  androidTestImplementation(libs.mockito.android)
  androidTestImplementation(libs.mockito.kotlin)
  testImplementation(libs.robolectric)
  androidTestImplementation(libs.kaspresso)
  androidTestImplementation(libs.kaspresso.allure.support)
  androidTestImplementation(libs.kaspresso.compose.support)

  testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
  // Configure Jacoco for each tests
  configure<JacocoTaskExtension> {
    isIncludeNoLocationClasses = true
    excludes = listOf("jdk.internal.*")
  }

  // Same selection, for the JVM/Robolectric tests. Naming a class includes all of its methods,
  // and a combination that matches no unit test at all is not an error, it just runs nothing.
  if (filtersActive) {
    filter {
      isFailOnNoMatchingTests = false
      if (unitSelection.isEmpty()) includeTestsMatching("NoTestMatchedTheSelectionFlags")
      else unitSelection.forEach { includeTestsMatching(it) }
    }
  }
}

tasks.register("jacocoTestReport", JacocoReport::class) {
  mustRunAfter("testDebugUnitTest", "connectedDebugAndroidTest")

  reports {
    xml.required = true
    html.required = true
  }

  val fileFilter =
      listOf(
          "**/R.class",
          "**/R$*.class",
          "**/BuildConfig.*",
          "**/Manifest*.*",
          "**/*Test*.*",
          "android/**/*.*",
          "**/sigchecks/**",
      )
  val debugTree =
      fileTree(project.layout.buildDirectory) {
        include(
            // AGP 9 compiles Kotlin with its built-in compiler, which writes here
            "intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes/**",
            // Fallback for the AGP 8 layout
            "tmp/kotlin-classes/debug/**",
        )
        exclude(fileFilter)
      }
  val mainSrc = "${project.projectDir}/src/main/java"

  sourceDirectories.setFrom(files(mainSrc))
  classDirectories.setFrom(files(debugTree))
  executionData.setFrom(
      fileTree(project.layout.buildDirectory) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        include("outputs/code_coverage/debugAndroidTest/connected/*/coverage.ec")
      }
  )
}

configurations.forEach { configuration ->
  // Exclude protobuf-lite from all configurations
  // This fixes a fatal exception for tests interacting with Cloud Firestore
  configuration.exclude("com.google.protobuf", "protobuf-lite")
}
