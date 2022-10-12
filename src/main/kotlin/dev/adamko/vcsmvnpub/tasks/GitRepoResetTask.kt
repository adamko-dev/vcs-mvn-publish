package dev.adamko.vcsmvnpub.tasks


import dev.adamko.vcsmvnpub.GitService
import java.io.File
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction


abstract class GitRepoResetTask : VcsMvnPublishTask() {

  /** The branch into which artifacts will be committed */
  @get:Input
  abstract val branch: Property<String>

  @get:OutputDirectory
  abstract val localRepoDir: DirectoryProperty

  @get:Internal
  abstract val gitService: Property<GitService>


  init {
    outputs.upToDateWhen(GitRepoInitTaskUpToDateCheck)
  }


  @TaskAction
  fun exec() {
    logger.lifecycle("GitRepoInitTask executing")

    val gitService = gitService.get()
    val localRepoFile: File = localRepoDir.asFile.get()
    val branch: String = branch.get()

    gitService.fetch(
      repoDir = localRepoFile,
      depth = 1,
    )

    logger.lifecycle("checking out existing $branch in $localRepoFile")
    gitService.checkout(
      repoDir = localRepoFile,
      branch = branch,
    )

    logger.lifecycle("cleaning $branch in $localRepoFile")
    gitService.clean(
      repoDir = localRepoFile,
      force = true,
      directories = true,
    )

//    gitService.commit(
//      repoDir = localRepoFile,
//      message = "${VcsMvnPublishPlugin.PROJECT_NAME} initialized repo",
//    )
  }


  companion object {
    const val NAME = "gitRepoReset"
  }
}
