package dev.adamko.vcsmvnpub.tasks

import dev.adamko.vcsmvnpub.GitService
import java.io.File
import org.gradle.api.Task
import org.gradle.api.specs.Spec


object GitRepoResetTaskUpToDateCheck : Spec<Task> {

  override fun isSatisfiedBy(task: Task): Boolean = when (task) {
    !is GitRepoInitTask -> false
    else                -> with(task) {
      val gitService: GitService = gitService.get()
      val localRepoFile: File = localRepoDir.asFile.get()

      if (!GitRepoInitTaskUpToDateCheck.isSatisfiedBy(this)) {
        return false
      }

//      val localRepoFileExists = localRepoFile.exists()
//      if (!localRepoFileExists) {
//        logger.lifecycle("GitRepoInitTaskUpToDateCheck > local repo dir does not exist ${localRepoFile.canonicalPath}")
//        return false
//      }
//
//      val gitCacheDir = localRepoFile.resolve(".git")
//      val gitCacheDirExists = gitCacheDir.exists()
//      if (!gitCacheDirExists) {
//        logger.lifecycle("GitRepoInitTaskUpToDateCheck > .git cache dir does not exist ${gitCacheDir.canonicalPath}")
//        return false
//      }
//
//      val isInsideWorkTree = gitService.isInsideWorkTree(localRepoFile).get()
//      if (!isInsideWorkTree) {
//        logger.lifecycle("GitRepoInitTaskUpToDateCheck > not inside work tree")
//        return false
//      }
//
//      val topLevelDir = gitService.topLevelDir(localRepoFile).get()
//      if (topLevelDir.canonicalPath != localRepoFile.canonicalPath) {
//        logger.lifecycle("GitRepoInitTaskUpToDateCheck > top-level-dir ${topLevelDir.canonicalPath} does not match local-repo-dir ${localRepoFile.canonicalPath}")
//        return false
//      }

      val isStatusEmpty = gitService.status(localRepoFile).isBlank()
      logger.lifecycle("GitRepoResetTaskUpToDateCheck > isStatusEmpty:$isStatusEmpty")
      if (!isStatusEmpty) return false

      return true
    }
  }
}
