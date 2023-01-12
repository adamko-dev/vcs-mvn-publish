package buildsrc.convention

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
