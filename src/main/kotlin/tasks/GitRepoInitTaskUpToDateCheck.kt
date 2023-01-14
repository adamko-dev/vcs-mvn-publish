package dev.adamko.vcsmvnpub.tasks

import dev.adamko.vcsmvnpub.GitService
import java.io.File
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.specs.Spec


object GitRepoInitTaskUpToDateCheck : Spec<Task> {

  private val logger: Logger = Logging.getLogger(this::class.java)

  override fun isSatisfiedBy(task: Task): Boolean = when (task) {
    !is GitRepoInitTask -> false
    else                -> runCatching {
      val gitService: GitService = task.gitService.get()
      val localRepoFile: File = task.localRepoDir.asFile.get()
      val branch = task.branch.get()

      val localRepoFileExists = localRepoFile.exists()
      if (!localRepoFileExists) {
        logger.lifecycle("GitRepoInitTaskUpToDateCheck > local repo dir does not exist ${localRepoFile.canonicalPath}")
        return false
      }

      val gitCacheDir = localRepoFile.resolve(".git")
      val gitCacheDirExists = gitCacheDir.exists()
      if (!gitCacheDirExists) {
        logger.lifecycle("GitRepoInitTaskUpToDateCheck > .git cache dir does not exist ${gitCacheDir.canonicalPath}")
        return false
      }

      val isInsideWorkTree = gitService.isInsideWorkTree(localRepoFile).get()
      if (!isInsideWorkTree) {
        logger.lifecycle("GitRepoInitTaskUpToDateCheck > not inside work tree")
        return false
      }

      val topLevelDir = gitService.topLevelDir(localRepoFile).get()
      if (topLevelDir.canonicalPath != localRepoFile.canonicalPath) {
        logger.lifecycle("GitRepoInitTaskUpToDateCheck > top-level-dir ${topLevelDir.canonicalPath} does not match local-repo-dir ${localRepoFile.canonicalPath}")
        return false
      }

      val status = gitService.status(localRepoFile, porcelain = true).trim()

      if (
        status.isNotBlank()
//        && !("On branch $branch" !in status && "nothing to commit, working tree clean" !in status)
      ) {
        val statusFormatted = status.prependIndent("> ")
        logger.lifecycle("GitRepoInitTaskUpToDateCheck > git status not up to date\n$statusFormatted")
        return false
      }

      val currentBranch = gitService.getCurrentBranch(localRepoFile).orNull
      if (currentBranch != branch) {
        logger.lifecycle("GitRepoInitTaskUpToDateCheck > incorrect branch. expected:$branch, actual:$currentBranch")
        return false
      }

      logger.lifecycle("GitRepoInitTaskUpToDateCheck > up to date!")
      return true
    }.getOrElse { false }
  }
}
