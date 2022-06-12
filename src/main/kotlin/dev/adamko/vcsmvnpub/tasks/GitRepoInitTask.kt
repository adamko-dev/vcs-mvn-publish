package dev.adamko.vcsmvnpub.tasks


import dev.adamko.vcsmvnpub.GitService
import dev.adamko.vcsmvnpub.VcsMvnGitRepo
import dev.adamko.vcsmvnpub.VcsMvnGitRepo.BranchCreateMode.CreateOrphan
import dev.adamko.vcsmvnpub.VcsMvnGitRepo.BranchCreateMode.Disabled
import dev.adamko.vcsmvnpub.VcsMvnPublishPlugin
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


  @TaskAction
  fun exec() {
    logger.lifecycle("GitRepoInitTask executing")

    val gitService = gitService.get()

    val localRepoTree: FileTree = localRepoDir.asFileTree

    val localRepoDir: File = localRepoDir.asFile.get()
    val remoteUri: String = remoteUri.get()
    val branch: String = artifactBranch.get()
    val artifactBranchCreateMode: VcsMvnGitRepo.BranchCreateMode = artifactBranchCreateMode.get()

    if (!gitService.isRepo(localRepoDir)) {
      require(localRepoTree.isEmpty) { "localRepoDir must be a git directory, or empty $localRepoDir" }

      logger.lifecycle("cloning remote '$remoteUri' into local '$localRepoDir'")

      gitService.clone(
        repoDir = localRepoDir,
        remoteUri = remoteUri,
      )
    }

    gitService.fetch(localRepoDir)

    logger.lifecycle("cleaning $branch in $localRepoDir")
    gitService.clean(
      repoDir = localRepoDir,
      force = true,
      directories = true,
    )

    val branchExists = gitService.doesBranchExistOnRemote(
      repoDir = localRepoDir,
      branch = branch,
    )

    if (branchExists) {
      logger.lifecycle("checking out $branch into $localRepoDir")
      gitService.checkout(
        repoDir = localRepoDir,
        branch = branch,
      )
    } else {
      when (artifactBranchCreateMode) {
        CreateOrphan -> gitService.initOrphanBranch(localRepoDir, branch)
        Disabled     -> error(
          "branch $branch does not exist in $remoteUri, and artifactBranchCreateMode is $artifactBranchCreateMode"
        )
      }
    }
  }


  private fun GitService.initOrphanBranch(
    localRepoDir: File,
    branch: String,
  ) {
    logger.lifecycle("initialising a new orphan branch $branch in $localRepoDir")

    checkoutOrphan(
      repoDir = localRepoDir,
      branch = branch,
    )

    commit(
      repoDir = localRepoDir,
      message = "[${VcsMvnPublishPlugin.PROJECT_NAME}] initialised new orphan branch",
    )

    push(
      repoDir = localRepoDir,
    )
  }


  companion object {
    const val NAME = "gitRepoInit"
  }
}
