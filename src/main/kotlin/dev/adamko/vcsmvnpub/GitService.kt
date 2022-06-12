package dev.adamko.vcsmvnpub

import dev.adamko.vcsmvnpub.util.execCapture
import java.io.File
import javax.inject.Inject
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs

abstract class GitService @Inject constructor(
  private val executor: ExecOperations
) : BuildService<GitService.Params> {

  private val logger: Logger = Logging.getLogger(GitService::class.java)

  interface Params : BuildServiceParameters {
    /**
     * The full path of the git executable.
     *
     * If `git` is already on `PATH`, then this can just be `git`
     *
     * @see dev.adamko.vcsmvnpub.VcsMvnPublishSettings.gitExec
     */
    val gitExec: Property<String>

    val defaultOrigin: Property<String>
    val porcelainEnabled: Property<Boolean>
    val logLevel: Property<LogLevel>
  }


  fun isRepo(
    repoDir: File,
  ): Boolean {
    val output = repoDir git "rev-parse --is-inside-work-tree $porcelainFlag"
    return output.trim().toBoolean()
  }


  fun fetch(
    repoDir: File,
    origin: String? = defaultOrigin,
  ): String = repoDir git "fetch $origin"


  fun clone(
    repoDir: File,
    remoteUri: String,
    origin: String? = defaultOrigin,
    noCheckout: Boolean = true,
  ): String {
    val originFlag = if (origin.isNullOrBlank()) "" else "--origin $origin"
    val noCheckoutFlag = if (noCheckout) "--no-checkout" else ""

    return repoDir git "clone $remoteUri $noCheckoutFlag $originFlag"
  }

// public  fun init(
//    repoDir: File,
//    remoteUri: String,
//    origin: String? = defaultOrigin,
//  ): String {
//    repoDir git "init"
//    repoDir git "remote add $origin $remoteUri"
//
//    return repoDir git "status"
//  }


  // https://git-scm.com/docs/git-checkout#Documentation/git-checkout
  fun checkout(
    repoDir: File,
    branch: String,
    origin: String? = defaultOrigin,
    force: Boolean = false,
  ): String {
    // "track without -b implies creation" -> will guess name after last /

    val trackFlag = when {
      origin.isNullOrBlank() -> "      -b $branch"
      else                   -> " --track $origin/$branch"
    }.trim()

    val forceFlag = if (force) "--force" else ""

    return repoDir git "checkout $trackFlag $forceFlag"
  }


  // https://git-scm.com/docs/git-checkout#Documentation/git-checkout
  fun checkoutOrphan(
    repoDir: File,
    branch: String,
    force: Boolean = false,
  ): String {
    val forceFlag = if (force) "--force" else ""

    return repoDir git "checkout --orphan $branch $forceFlag"
  }


// public  fun hardReset(
//    repoDir: File,
//    branch: String,
//    remoteName: String? = defaultOrigin,
//  ): String {
//    repoDir git "fetch $remoteName"
//    repoDir git "reset --hard $remoteName/$branch"
//    return repoDir git "status"
//  }


  fun clean(
    repoDir: File,
    force: Boolean,
    directories: Boolean,
  ): String {
    val forceFlag = if (force) "--force" else ""
    val directoriesFlag = if (directories) "-d" else ""

    return repoDir git "clean $forceFlag $directoriesFlag"
  }


  fun commit(
    repoDir: File,
    message: String,
    addAll: Boolean = true,
  ): String {
    require(message.isNotBlank()) { "commit message must not be blank" }
    val allFlag = if (addAll) "--all" else ""

    return repoDir git "commit $allFlag --message $message"
  }


  fun push(
    repoDir: File,
    origin: String? = defaultOrigin,
  ): String = repoDir git "push $origin"


  fun configGet(
    repoDir: File,
    property: String,
  ): String = repoDir git "config --get $property"


  fun doesBranchExistOnRemote(
    repoDir: File,
    branch: String,
    origin: String? = defaultOrigin,
  ): Boolean {
    val result = repoDir git "ls-remote --heads $origin $branch"
    return result.isNotBlank()
  }


  private infix fun File.git(
    cmd: String
  ): String {
    val dir = this
    val result = executor.execCapture {
      executable(gitExec)
      workingDir(dir.canonicalPath)
      commandLine(
        parseSpaceSeparatedArgs("$gitExec $cmd")
      )
    }
    logger.log(logLevel, result.output)
    return result.output
  }

  private val gitExec: String
    get() = parameters.gitExec.get()

  private val defaultOrigin: String?
    get() = parameters.defaultOrigin.orNull

  private val porcelainFlag: String
    get() = parameters
      .porcelainEnabled.getOrElse(false)
      .let { if (it) "--porcelain" else "" }

  private val logLevel: LogLevel
    get() = parameters.logLevel.orNull ?: LogLevel.DEBUG


  companion object {
    const val NAME: String = "GitService"
  }
}
