package dev.adamko.vcsmvnpub

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property


abstract class VcsMvnPublishSettings {

  private val logger: Logger = Logging.getLogger(this::class.java)

  /**
   * The local working directory that will contain the cloned remote repositories.
   *
   * New releases will be copied into this directory before they are committed and pushed.
   */
  abstract val localPublishDir: DirectoryProperty

  /**
   * The full path of the git executable.
   *
   * By default, it's assumed that `git` is already on `PATH` and the value is set to `git`
   */
  abstract val gitExec: Property<String>

  /** Enable automatically pushing to the remote repository, once a release is committed. */
  abstract val gitPushToRemoteEnabled: Property<Boolean>

  /**
   * vcs-mvn-publish will guess a default remote URL for all [gitRepos] based on the provided
   * directory. This defaults to [org.gradle.api.Project.getRootDir].
   *
   * To disable this behaviour, set this property to `null`.
   */
  abstract val gitProjectRepoDir: RegularFileProperty

  abstract val gitRepos: NamedDomainObjectContainer<VcsMvnGitRepo>

  fun gitRepo(
    name: String,
    configure: VcsMvnGitRepo.() -> Unit = {},
  ) {
    logger.lifecycle("Creating GitRepo")
    gitRepos.register(name) {
      localRepoDir.convention(localPublishDir.dir(name))
      artifactBranchCreateMode.convention(VcsMvnGitRepo.BranchCreateMode.CreateOrphan)
      artifactBranch.convention(name)
      repoArtifactDir.convention("m2")
      configure()
    }
  }
}
