package dev.adamko.vcsmvnpub.tasks


import dev.adamko.vcsmvnpub.GitService
import dev.adamko.vcsmvnpub.VcsMvnGitRepo
import java.io.File
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction


abstract class GitRepoInitTask : VcsMvnPublishTask() {

  @get:Input
  abstract val remoteUri: Property<String>

  /** The branch into which artifacts will be committed */
  @get:Input
  abstract val artifactBranch: Property<String>

  @get:Input
  abstract val artifactBranchCreateMode: Property<VcsMvnGitRepo.BranchCreateMode>

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

    val localRepoTree: FileTree = localRepoDir.asFileTree
    val localRepoFile: File = localRepoDir.asFile.get()

    val remoteUri: String = remoteUri.get()
    val branch: String = artifactBranch.get()

    require(
      localRepoTree.matching { exclude(".git") }.isEmpty
    ) {
      val topFiles = localRepoFile.walk().maxDepth(1).map { it.name }.joinToString(", ")
      val gitStatus = gitService.status(localRepoFile)

      """
        |localRepoDir must be an up-to-date git directory, or empty.
        |
        |Contained files: $topFiles
        |git status: $gitStatus
        |
        |Refusing to automatically overwrite.
        |
        |Please confirm that path ${localRepoFile.canonicalPath} is correct, and manually refresh or delete it.
      """.trimMargin()
    }

    logger.lifecycle("initialising remote '$remoteUri' into local '$localRepoFile'")

    gitService.init(
      repoDir = localRepoFile,
//      branch = branch,
    )

    val remoteOriginUrl = gitService.configGetRemoteOriginUrl(localRepoFile).getOrElse("")

    when {
      remoteOriginUrl.isBlank()    -> // remote isn't defined
        gitService.remoteAdd(
          repoDir = localRepoFile,
          remoteUri = remoteUri,
        )

      remoteOriginUrl != remoteUri ->
        error("invalid remote, expected URL $remoteUri but was $remoteOriginUrl")
    }

    gitService.fetch(
      repoDir = localRepoFile,
      depth = 1,
    )

    val branchExists = gitService.doesBranchExistOnRemote(
      repoDir = localRepoFile,
      branch = branch,
    ).get()

    if (branchExists) {
      logger.lifecycle("checking out existing $branch in $localRepoFile")
      gitService.checkout(
        repoDir = localRepoFile,
        branch = branch,
      )
    } else {
      logger.lifecycle("checking out new branch $branch in $localRepoFile")
      when (artifactBranchCreateMode.get()) {
        VcsMvnGitRepo.BranchCreateMode.CreateOrphan -> {
          gitService.checkoutOrphan(
            localRepoFile,
            branch,
            force = true
          )

        }
        VcsMvnGitRepo.BranchCreateMode.Disabled     -> error(
          "branch $branch does not exist in $remoteUri, and artifactBranchCreateMode is $artifactBranchCreateMode"
        )
      }
    }

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
    const val NAME = "gitRepoInit"
  }
}
