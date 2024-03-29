package buildsrc.convention

import buildsrc.ext.createKotkaStreamsPom
import buildsrc.ext.credentialsAction
import buildsrc.ext.publishing
import buildsrc.ext.signing


plugins {
  id("buildsrc.convention.base")
  `maven-publish`
//  signing
}

val sonatypeRepositoryCredentials: Provider<Action<PasswordCredentials>> =
  providers.credentialsAction("sonatypeRepository")

val gitHubPackagesCredentials: Provider<Action<PasswordCredentials>> =
  providers.credentialsAction("GitHubPackages")


val sonatypeRepositoryReleaseUrl: Provider<String> = provider {
  if (version.toString().endsWith("SNAPSHOT")) {
    "https://s01.oss.sonatype.org/content/repositories/snapshots/"
  } else {
    "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
  }
}


val signingKeyId: Provider<String> =
  providers.gradleProperty("signingKeyId")
val signingKey: Provider<String> =
  providers.gradleProperty("signingKey")
val signingPassword: Provider<String> =
  providers.gradleProperty("signingPassword")
val signingSecretKeyRingFile: Provider<String> =
  providers.gradleProperty("signingSecretKeyRingFile")


tasks.withType<AbstractPublishToMaven>().configureEach {
  // Gradle warns about some signing tasks using publishing task outputs without explicit dependencies.
  // Here's a quick fix.
  dependsOn(tasks.withType<Sign>())
  mustRunAfter(tasks.withType<Sign>())

  // use a val for the GAV to avoid Gradle Configuration Cache issues
  val publicationGAV = publication?.run { "$group:$artifactId:$version" }

  doLast {
    if (publicationGAV != null) {
      logger.lifecycle("[task: ${path}] $publicationGAV")
    }
  }
}


//signing {
//  if (sonatypeRepositoryCredentials.isPresent) {
//    if (signingKeyId.isPresent && signingKey.isPresent && signingPassword.isPresent) {
//      useInMemoryPgpKeys(signingKeyId.get(), signingKey.get(), signingPassword.get())
//    } else {
//      useGpgCmd()
//    }
//
//    // sign all publications
////    sign(publishing.publications)
//  }
//}


publishing {
  repositories {
    // publish to local dir, for testing
    maven(rootProject.layout.buildDirectory.dir("maven-project-local")) {
      name = "ProjectLocal"
    }

//    if (sonatypeRepositoryCredentials.isPresent) {
//      maven(sonatypeRepositoryReleaseUrl) {
//        name = "sonatype"
//        credentials(sonatypeRepositoryCredentials.get())
//      }
//    }
//
//    if (gitHubPackagesCredentials.isPresent) {
//      maven("https://maven.pkg.github.com/adamko-dev/kotka-streams") {
//        name = "GitHubPackages"
//        credentials(gitHubPackagesCredentials.get())
//      }
//    }
  }

  publications.withType<MavenPublication>().configureEach {
    createKotkaStreamsPom()
  }
}


//plugins.withType<JavaPlugin>().configureEach {
//  publishing.publications.create<MavenPublication>("mavenJava") {
//    from(components["java"])
////    artifact(tasks["sourcesJar"])
//  }
//}


plugins.withType<JavaPlatformPlugin>().configureEach {

  val javadocJarStub by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Stub javadoc.jar artifact (required by Maven Central)"
    archiveClassifier.set("javadoc")
  }

  tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(javadocJarStub)
  }

//  signing.sign(javadocJarStub.get())

  publishing.publications.create<MavenPublication>("mavenJavaPlatform") {
    from(components["javaPlatform"])
    artifact(javadocJarStub)
  }
}
