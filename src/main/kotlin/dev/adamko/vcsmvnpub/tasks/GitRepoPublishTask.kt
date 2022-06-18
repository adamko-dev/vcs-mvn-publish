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
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction


abstract class GitRepoPublishTask : VcsMvnPublishTask() {

  @get:InputDirectory
//  @get:SkipWhenEmpty
  abstract val localRepoDir: DirectoryProperty

  @get:Internal
  abstract val gitService: Property<GitService>

  @get:Input
  @get:Optional
  abstract val commitMessage: Property<Transformer<String, GitRepoPublishTask>>

  @get:Input
  @get:Optional
  abstract val gitPushToRemoteEnabled: Property<Boolean>

  @get:Input
//  @get:Optional
  abstract val publishTasks: NamedDomainObjectContainer<PublishToMavenRepository>


  init {
    description = "commit and push files to a Git repo after a Maven publication"

    outputs.upToDateWhen { task ->
      when (task) {
        !is GitRepoPublishTask -> false
        else                   -> with(task) {
          val gitService = gitService.get()
          val localRepoDir: File = localRepoDir.asFile.get()

          gitService.status(localRepoDir).isBlank()
        }
      }
    }
  }


  @TaskAction
  fun exec() {
    val gitService = gitService.get()
    val localRepoDir: File = localRepoDir.asFile.get()
    val commitMessage: String = commitMessage.map { it.transform(this) }.getOrElse(commitMessage())

    fun currentBranch() = "current-branch:" + gitService.getCurrentBranch(localRepoDir).get()

    logger.lifecycle("[GitRepoPublishTask] committing localRepo $localRepoDir commit message $commitMessage ${currentBranch()}")

    gitService.fetch(localRepoDir)

    logger.lifecycle("[GitRepoPublishTask] adding all files ${currentBranch()}")
    gitService.addAll(
      localRepoDir,
    )

    logger.lifecycle("[GitRepoPublishTask] committing ${currentBranch()}")
    gitService.commit(
      localRepoDir,
      commitMessage,
    )

    if (gitPushToRemoteEnabled.orNull == true) {
      logger.lifecycle("[GitRepoPublishTask] pushing to remote")
      gitService.push(localRepoDir)
    } else {
      logger.lifecycle("[GitRepoPublishTask] skipping push to remote")
    }
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
