package dev.adamko.vcsmvnpub

import java.io.File
import javax.inject.Inject
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import org.gradle.process.ExecOutput
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs


@Suppress("UnstableApiUsage") // providers.exec is incubating
abstract class GitService @Inject constructor(
  private val executor: ExecOperations,
  private val providers: ProviderFactory,
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
    //    val porcelainEnabled: Property<Boolean>
    val logLevel: Property<LogLevel>
  }


  fun isInsideWorkTree(
    repoDir: File,
  ): Provider<Boolean> {
    val result = repoDir git "rev-parse --is-inside-work-tree"
    return result.standardOutput.asText.map { it.trim().toBoolean() }
  }


  fun topLevelDir(
    repoDir: File,
  ): Provider<File> {
    val topLevel = repoDir git "rev-parse --show-toplevel"
    return topLevel.standardOutput.asText.map {
      File(it.trim())
    }
  }


  fun fetch(
    repoDir: File,
    origin: String? = defaultOrigin,
    depth: Int? = null,
  ): String {
    val depthFlag = if (depth != null) "--depth $depth" else ""
    val result = repoDir git "fetch $depthFlag $origin"
    return result.getAndLog()
  }


  fun init(
    repoDir: File,
    branch: String? = null,
  ): String {
    val initialBranch = if (branch.isNullOrBlank()) "" else "--initial-branch=$branch"
    val result = repoDir git "init ${repoDir.canonicalPath} $initialBranch"
    return result.getAndLog()
  }


  fun status(
    repoDir: File,
    branch: Boolean = false
  ): String {
    val branchFlag = if (branch) "--branch" else ""

    return (repoDir git "status --porcelain $branchFlag").getAndLog()
  }


  // https://git-scm.com/docs/git-remote#Documentation/git-remote.txt-emaddem
  fun remoteAdd(
    repoDir: File,
    remoteUri: String,
    branch: String? = null,
    origin: String? = defaultOrigin,
    fetch: Boolean = false,
  ): String {
    val branchTrack = if (branch.isNullOrBlank()) "" else "-t $branch"
    val fetchFlag = if (fetch) "-f" else ""

    val result = repoDir git "remote add $fetchFlag $branchTrack $origin $remoteUri"
    return result.getAndLog()
  }

//  /**
//   * 1. `git init`
//   * 2. `git remote add`
//   * 3. `git fetch`
//   * 4. `git checkout`
//   */
//  fun clone(
//    repoDir: File,
//    remoteUri: String,
//    origin: String? = defaultOrigin,
//    depth: Int? = null,
//    branch: String? = null,
//  ): String {
//
//    var output = ""
//
//    output += init(repoDir)
//    output += remoteAdd(repoDir, remoteUri = remoteUri, origin = origin)
//    output += fetch(repoDir, depth = depth)
//    if (!branch.isNullOrBlank())
//      output += checkout(repoDir, branch = branch)
//
//    return output
//  }

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
    val trackFlag = when {
      origin.isNullOrBlank() -> "      -b $branch"
      else                   -> " --track $origin/$branch"
      //                         track without -b implies creation, git will guess branch name
    }.trim()

    val forceFlag = if (force) "--force" else ""

    val result = repoDir git "checkout $trackFlag $forceFlag ."
    return result.getAndLog()
  }


  // https://git-scm.com/docs/git-checkout#Documentation/git-checkout
  fun checkoutOrphan(
    repoDir: File,
    branch: String,
    force: Boolean = false,
  ): String {
    val forceFlag = if (force) "--force" else ""

    val result = repoDir git "checkout --orphan $branch $forceFlag"
    return result.getAndLog()
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

    val result = repoDir git "clean $forceFlag $directoriesFlag"
    return result.getAndLog()
  }


  /**
   * @param[message] The raw message. Line breaks and quot marks `"` will be escaped.
   */
  fun commit(
    repoDir: File,
    message: String,
  ): String {
    require(message.isNotBlank()) { "commit message must not be blank" }

    val escapedMessage = message
      .trim()
      .replace('\"', '\'')
      .lines()
      .joinToString(separator = " ") { line -> "-m \"${line}\"" }

    return (repoDir git "commit $escapedMessage").getAndLog()
  }


  fun addAll(
    repoDir: File,
  ): String = (repoDir git "add --all").getAndLog()


  // https://git-scm.com/docs/git-push
  fun push(
    repoDir: File,
    origin: String? = defaultOrigin,
    head: Boolean = true,
  ): String {
    val headFlag = if (head) "HEAD" else ""

    return (repoDir git "push $origin $headFlag").getAndLog()
  }


  private fun configGet(
    repoDir: File,
    property: String,
  ): Provider<String> = providers.provider {
    runCatching {
      val result = repoDir git "config --get $property"
      result.standardOutput.asText.get().trim()
    }.getOrElse { "" }
  }


  fun configGetRemoteOriginUrl(
    repoDir: File,
    origin: String? = defaultOrigin,
  ): Provider<String> = configGet(repoDir, "remote.${origin}.url")


  fun doesBranchExistOnRemote(
    repoDir: File,
    branch: String,
    origin: String? = defaultOrigin,
  ): Provider<Boolean> {
    val result = repoDir git "ls-remote --heads $origin $branch"
    return result.standardOutput.asText.map { it.toBoolean() }
  }


  private infix fun File.git(
    cmd: String
  ): ExecOutput {
    logger.lifecycle("GitService git exec $canonicalPath $gitExec $cmd")
    return providers.exec {
//      isIgnoreExitValue = true
      workingDir(canonicalPath)
      commandLine(parseSpaceSeparatedArgs("$gitExec $cmd"))
    }
//    val result = executor.execCapture(throwError = true) {
//      executable(gitExec)
//      workingDir(dir.canonicalPath)
//      commandLine(
//        parseSpaceSeparatedArgs("$gitExec $cmd")
//      )
//    }
//    logger.log(
//      logLevel,
//      """
//        |---
//        |git exec [${result.exitValue}]
//        |  cmd: $cmd
//        |  dir: ${dir.canonicalPath}
//        |  result:
//        |${result.output.prependIndent("  ")}
//        |
//      """.trimMargin()
//    )
//    if (result.exitValue != 0) error("git command $cmd failed $result")
//    return result.output
  }


  private fun ExecOutput.getAndLog(): String {
    val stdOut = standardOutput.asText.orNull
    val stdErr = standardError.asText.orNull
    val result = result.orNull

    logger.log(
      logLevel,
      """
        |---
        |  git exec [${result?.exitValue}]
        |  result: ${(stdOut ?: stdErr ?: "null").prependIndent("    ")}
        |---  
      """.trimMargin()
    )
    return stdOut ?: stdErr ?: ""
  }


  private val gitExec: String
    get() = parameters.gitExec.get()

  private val defaultOrigin: String?
    get() = parameters.defaultOrigin.orNull

//  private val porcelainFlag: String
//    get() = parameters
//      .porcelainEnabled.getOrElse(false)
//      .let { if (it) "--porcelain" else "" }

  private val logLevel: LogLevel
    get() = parameters.logLevel.orNull ?: LogLevel.LIFECYCLE


  companion object {
    const val NAME: String = "GitService"
  }
}
