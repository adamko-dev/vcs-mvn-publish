# `vcs-mvn-publish`

Publish Maven artifacts to a Git repository. 

Artifacts are published to a separate branch, so development branches remain clean.

Simpler than figuring out publishing to Maven Central!

### Status

`vcs-mvn-publish` is a proof-of-concept. There are no tests. Use at your own risk.

### Quick start

First, set up a regular Gradle project, and configure the 
[Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html).

Next, add the `vcs-mvn-publish` plugin, and register a Git repo.

```kotlin
buildscript {
  plugins {
    repositories {
      maven("https://raw.githubusercontent.com/adamko-dev/vcs-mvn-publish/artifacts/m2")
    }
  }
}

plugins {
  id("dev.adamko.vcs-mvn-publish") version "main-SNAPSHOT"
  java
  `maven-publish`
}

vcsMvnPublish {
  gitPushToRemoteEnabled.set(false) // disable auto-push, so we can check it works first
  gitRepo("artifacts") // 'artifacts' will be the remote branch
}
```

Then, run the publishing task

```shell
./gradlew gitRepoPublish
```

Check that there's a Maven repo in `$projectDir/.gradle/vcs-mvn-publish/artifacts/`.
Are all the published artifacts there?

If you're happy, then enable auto-push, and re-run `./gradlew gitRepoPublish`.


### Multiple subprojects

To publish multiple subprojects to the Git repository, just add `vcs-mvn-publish` to all relevant
subprojects, and to the root project.
