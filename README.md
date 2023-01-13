# vcs-mvn-publish

Publish libraries and applications to a Git repository. 

Simpler than figuring out publishing to Maven Central!


### Set up

```kotlin
plugins {
  id("dev.adamko.vcs-mvn-publish")
  java
  `maven-publish`
}

vcsMvnPublish {
  gitPushToRemoteEnabled.set(false)
  gitRepo("artifacts")
}
```
