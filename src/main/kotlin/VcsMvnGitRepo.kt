package dev.adamko.vcsmvnpub

import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface VcsMvnGitRepo: Named {

  /** The location of the remote Git repo */
  @get:Input
  val remoteUri: Property<String>

  /** The branch into which artifacts will be committed */
  @get:Input
  val artifactBranch: Property<String>

  /** Set how [artifactBranch] should be created if it does not exist in [remoteUri]. */
  @get:Input
  val artifactBranchCreateMode: Property<BranchCreateMode>

  /** The directory inside the remote repo to publish the artifacts */
  @get:Input
  @get:Optional
  val repoArtifactDir: Property<String>

  /** Defaults to a directory inside [VcsMvnPublishSettings.localPublishDir] */
  @get:Input
  val localRepoDir: DirectoryProperty


  enum class BranchCreateMode {
    /** If [VcsMvnGitRepo.artifactBranch] does not exist, create it as an orphan branch */
    CreateOrphan,
    /** Do not create [VcsMvnGitRepo.artifactBranch] if it does not exist */
    Disabled,
  }
}
