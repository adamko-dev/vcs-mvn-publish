package dev.adamko.vcsmvnpub

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.kotlin.dsl.named

internal class VcsMvnPublishConfigurations(
  project: Project
) {

  val vcsMvnPublicationsElements = project.configurations.register("vcsMvnPublicationsProvider") {
    description = "provide vcs-mvn-publish publications to other subprojects"
    asProvider()
    attributes {
      attribute(USAGE_ATTRIBUTE, project.objects.named("dev.adamko.vcs-mvn-publish"))
      attribute(CATEGORY_ATTRIBUTE, project.objects.named("publication"))
      attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named("zip"))
    }
  }

  val vcsMvnPublication = project.configurations.register("vcsMvnPublications") {
    description = "consume vcs-mvn-publish publications from other subprojects"
    asConsumer()
    attributes {
      attribute(USAGE_ATTRIBUTE, project.objects.named("dev.adamko.vcs-mvn-publish"))
      attribute(CATEGORY_ATTRIBUTE, project.objects.named("publication"))
      attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named("zip"))
    }
    defaultDependencies {
      addAllLater(
        project.provider {
          project.subprojects.map { subproject ->
            project.dependencies.create(subproject)
          }
        }
      )
    }
  }

  /** Mark this [Configuration] as one that will be consumed by other subprojects. */
  private fun Configuration.asProvider() {
    isVisible = false
    isCanBeResolved = false
    isCanBeConsumed = true
  }

  /** Mark this [Configuration] as one that will consume (also known as 'resolving') artifacts from other subprojects */
  private fun Configuration.asConsumer() {
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
  }
}
