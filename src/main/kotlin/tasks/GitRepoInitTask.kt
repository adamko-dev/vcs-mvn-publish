package dev.adamko.vcsmvnpub.tasks


import dev.adamko.vcsmvnpub.GitService
import dev.adamko.vcsmvnpub.VcsMvnGitRepo
import java.io.File
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction


abstract class GitRepoInitTask : VcsMvnPublishTask() {

  @get:Input
  abstract val remoteUri: Property<String>

  /** The branch into which artifacts will be committed */
  @get:Input
  abstract val branch: Property<String>

  @get:Input
  abstract val branchCreateMode: Property<VcsMvnGitRepo.BranchCreateMode>

  @get:Internal
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
    val localRepoPath: String = localRepoFile.canonicalPath

    val remoteUri: String = remoteUri.get()
    val branch: String = branch.get()

    if (!localRepoFile.exists()) {
      require(localRepoFile.mkdirs()) {
        "could not initialise repo directory $localRepoPath"
      }
    }

    if (!gitService.isInsideWorkTree(localRepoFile).get()) {
      require(
        localRepoDir.asFileTree.matching { exclude(".git") }.isEmpty
      ) {
        val topFiles = localRepoFile.walk().maxDepth(1).map { it.name }.joinToString(", ")
        val gitStatus = gitService.status(localRepoFile).trim().prependIndent("  ║ ")

        """
          |Attempted to initialise a Git directory, but the provided directory contained unexpected files.
          |
          |localRepoDir must be an up-to-date git directory, or empty.
          |
          |Contained files: $topFiles
          |git status: 
          |$gitStatus
          |
          |Refusing to automatically overwrite.
          |
          |Please confirm that path $localRepoPath is correct, and manually refresh or delete it.
        """.trimMargin()
      }
    }
    fun currentBranch() = "current-branch:" + gitService.getCurrentBranch(localRepoFile).get()

    logger.lifecycle("initialising remote '$remoteUri' into local '$localRepoFile'")

    gitService.init(
      repoDir = localRepoFile,
//      branch = branch,
    )
    logger.lifecycle("initialized repository ${currentBranch()}")

    val actualRemoteUri = gitService.configGetRemoteOriginUrl(localRepoFile).getOrElse("")

    when {
      actualRemoteUri.isBlank()    -> // remote isn't defined
        gitService.remoteAdd(
          repoDir = localRepoFile,
          remoteUri = remoteUri,
        )

      actualRemoteUri != remoteUri ->
        error("remote already defined. expected URL $remoteUri but was $actualRemoteUri")
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
      logger.lifecycle("switching to branch '$branch' ${currentBranch()}")
      gitService.switch(
        repoDir = localRepoFile,
        branch = branch,
      )
      logger.lifecycle("switched to branch '$branch' ${currentBranch()}")
    } else {
      logger.lifecycle("branch does not exist on remote - checking out new branch '$branch 'in $localRepoFile, ${currentBranch()}")
      when (branchCreateMode.get()) {
        VcsMvnGitRepo.BranchCreateMode.CreateOrphan ->
          gitService.switchCreateOrphan(
            repoDir = localRepoFile,
            branch = branch,
          )

        VcsMvnGitRepo.BranchCreateMode.Disabled     ->
          error("artifactBranchCreateMode is $branchCreateMode, but branch $branch does not exist in $remoteUri")
      }
    }

    gitService.fetch(
      repoDir = localRepoFile,
      depth = 1,
    )

    logger.lifecycle("GitRepoInitTask finished ${currentBranch()}")
  }


  companion object {
    const val TASK_NAME = "gitRepoInit"
  }
}
