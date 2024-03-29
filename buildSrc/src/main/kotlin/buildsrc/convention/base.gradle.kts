package buildsrc.convention

import java.time.Duration
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  base
}

if (project != rootProject) {
  project.group = rootProject.group
  project.version = rootProject.version
}

tasks.withType<AbstractArchiveTask>().configureEach {
  // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
}

tasks.withType<Test>().configureEach {
  timeout.set(Duration.ofMinutes(15))

  testLogging {
    showCauses = true
    showExceptions = true
    showStackTraces = true
    showStandardStreams = true
    events(
      TestLogEvent.PASSED,
      TestLogEvent.FAILED,
      TestLogEvent.SKIPPED,
      // TestLogEvent.STARTED,
      TestLogEvent.STANDARD_ERROR,
      TestLogEvent.STANDARD_OUT,
    )
  }
}
