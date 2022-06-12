package dev.adamko.vcsmvnpub.tasks


import dev.adamko.vcsmvnpub.GitService
import dev.adamko.vcsmvnpub.VcsMvnPublishPlugin
import java.io.File
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Transformer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GitRepoPublishTask : VcsMvnPublishTask() {

  @get:OutputDirectory
  abstract val localRepoDir: DirectoryProperty

  @get:Internal
  abstract val gitService: Property<GitService>

  @get:Input
  @get:Optional
  abstract val commitMessage: Property<Transformer<String, GitRepoPublishTask>>

  @get:Input
  abstract val publishTasks: NamedDomainObjectContainer<PublishToMavenRepository>


  init {
    description = "commit and push files to a Git repo after a Maven publication"
  }


  @TaskAction
  fun exec() {
    val gitService = gitService.get()

    val localRepoDir: File = localRepoDir.asFile.get()

    val commitMessage: String = commitMessage.map { it.transform(this) }.getOrElse(commitMessage())

    gitService.fetch(localRepoDir)

    gitService.commit(
      localRepoDir,
      commitMessage,
      addAll = true,
    )

    gitService.push(
      localRepoDir,
    )
  }


  private fun commitMessage(): String {
    val publications = publishTasks.joinToString("\n ---") { task ->
      val publicationName = "publication ${task.publication.name}"

      val artifacts = task.publication.artifacts.joinToString("\n  - ") { artifact ->
        runCatching {
          artifact.file.toRelativeString(localRepoDir.asFile.get())
        }.getOrElse { artifact.file.name }
      }

      """
        |$publicationName, published by ${task.name}
        |Artifacts:
        |$artifacts
      """.trimMargin()
    }

    return "${VcsMvnPublishPlugin.PROJECT_NAME} committed new artifacts\n$publications"
  }

  companion object {
    const val NAME = "gitRepoPublish"
  }
}
