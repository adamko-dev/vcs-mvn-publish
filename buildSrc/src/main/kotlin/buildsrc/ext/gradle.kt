package buildsrc.ext

import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.plugin.use.PluginDependencySpec
import org.gradle.plugins.ide.idea.model.IdeaModule


// https://github.com/gradle/gradle/issues/20925
fun ProviderFactory.credentialsAction(
  repositoryName: String
): Provider<Action<PasswordCredentials>> = zip(
  gradleProperty("${repositoryName}Username"),
  gradleProperty("${repositoryName}Password"),
) { user, pass ->
  Action<PasswordCredentials> {
    username = user
    password = pass
  }
}


/** exclude generated Gradle code, so it doesn't clog up search results */
fun IdeaModule.excludeGeneratedGradleDsl(layout: ProjectLayout) {

  val generatedSrcDirs = listOf(
    "kotlin-dsl-accessors",
    "kotlin-dsl-external-plugin-spec-builders",
    "kotlin-dsl-plugins",
  )

  excludeDirs.addAll(
    layout.projectDirectory.asFile.walk()
      .filter { it.isDirectory }
      .filter { it.parentFile.name in generatedSrcDirs }
      .flatMap { file ->
        file.walk().maxDepth(1).filter { it.isDirectory }.toList()
      }
  )
}
