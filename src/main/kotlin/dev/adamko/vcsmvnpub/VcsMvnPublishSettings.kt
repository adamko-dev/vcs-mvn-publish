package dev.adamko.vcsmvnpub

import javax.inject.Inject
import org.gradle.api.DomainObjectSet
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.newInstance


abstract class VcsMvnPublishSettings @Inject constructor(
  private val objects: ObjectFactory,
) {

  private val logger: Logger = Logging.getLogger(this::class.java)

  /**
   * The local working directory that will contain the cloned remote repositories.
   *
   * New releases will be copied into this directory before they are committed and pushed.
   */
  @get:Input
  @get:Optional
  abstract val localPublishDir: DirectoryProperty


  /**
   * The full path of the git executable.
   *
   * If `git` is already on `PATH`, then this can just be `git`
   */
  @get:Input
  @get:Optional
  abstract val gitExec: Property<String>


  /** Automatically push, once committed. */
  @get:Input
  @get:Optional
  abstract val gitPushToRemoteEnabled: Property<Boolean>

  /**
   * vcs-mvn-publish will guess a default remote URL for all [gitRepos] based on the provided
   * directory. This defaults to [org.gradle.api.Project.getRootDir].
   *
   * To disable this behaviour, set this property to null.
   */
  @get:Input
  @get:Optional
  abstract val gitProjectRepoDir: RegularFileProperty


  @get:Input
  abstract val gitRepos: DomainObjectSet<VcsMvnGitRepo>

  fun gitRepo(
    configure: VcsMvnGitRepo.() -> Unit= {} ,
  ) {
    logger.lifecycle("Creating GitRepo")
    val repo = objects.newInstance<VcsMvnGitRepo>().apply {
      localRepoDir.convention(localPublishDir)
      artifactBranchCreateMode.convention(VcsMvnGitRepo.BranchCreateMode.CreateOrphan)
      artifactBranch.convention("artifacts")
      repoArtifactDir.convention("m2")
    }

    gitRepos.add(repo.apply(configure))
  }

}
