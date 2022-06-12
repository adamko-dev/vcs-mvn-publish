package dev.adamko.vcsmvnpub

import dev.adamko.vcsmvnpub.tasks.GitRepoInitTask
import dev.adamko.vcsmvnpub.tasks.GitRepoPublishTask
import java.net.URI
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.withType


abstract class VcsMvnPublishPlugin @Inject constructor(
  private val providers: ProviderFactory,
  private val objects: ObjectFactory,
  private val files: FileOperations,
  private val fileSys: FileSystemOperations,
) : Plugin<Project>, ProviderFactory by providers {


  private val log: Logger = Logging.getLogger(this::class.java)


  override fun apply(project: Project) {

    project.plugins.withType<PublishingPlugin>().configureEach {
      log.lifecycle("applying $PROJECT_NAME")

      val settings: VcsMvnPublishSettings = project.createExtension()

      val gitServiceProvider = project.gradle.registerGitService(settings)

      log.lifecycle("configuring GitRepo")

      settings.gitRepos.all {
        project.configureGitRepo(this, gitServiceProvider)
      }

    }
  }


  private fun Project.createExtension(): VcsMvnPublishSettings {
    log.lifecycle("creating $PROJECT_NAME extension on ${rootProject.name}")

    return extensions.create<VcsMvnPublishSettings>(EXTENSION_NAME)
      .apply {
        localPublishDir.convention(
          rootProject.layout.projectDirectory
            .dir(".gradle/$BASE_REPO_NAME/${project.pathEscaped}")
        )
        gitExec.convention("git")
      }
  }


  private fun Project.configureGitRepo(
    gitRepo: VcsMvnGitRepo,
    gitServiceProvider: Provider<GitService>,
  ) {
    log.lifecycle("[$PROJECT_NAME] configuring git repo")

    gitRepo.remoteUri.convention(
      gitRemoteUriConvention(
        gitServiceProvider,
        rootProject.layout.projectDirectory,
      )
    )

//
    val localGitRepoDir = gitRepo.localRepoDir


    val initGitRepoTask = project.tasks.register<GitRepoInitTask>(GitRepoInitTask.NAME) {
      artifactBranch.convention(gitRepo.artifactBranch)
      localRepoDir.convention(gitRepo.localRepoDir)
      remoteUri.convention(gitRepo.remoteUri)
      artifactBranchCreateMode.convention(gitRepo.artifactBranchCreateMode)

      gitService.convention(gitServiceProvider)
    }

    // create a new local Maven repository
    publishing.repositories.maven {
      name = PROJECT_NAME

      val artifactDir: String? = gitRepo.repoArtifactDir.orNull
      val targetDir: Provider<Directory> = when {
        artifactDir.isNullOrEmpty() -> localGitRepoDir
        else                        -> localGitRepoDir.dir(artifactDir)
      }
      setUrl(targetDir.get().asFile)
    }

    // find the publishing tasks created by the local Maven repo
    val gitRepoPublishingTasks = tasks
      .withType<PublishToMavenRepository>()
      .matching {
        val matching = it.repository.url sameFileAs localGitRepoDir
        log.lifecycle("found matching ${it.name}")
        matching
      }

    // publishing depends on the git repo being set up and checked out
    gitRepoPublishingTasks.configureEach {
      dependsOn(initGitRepoTask)
    }

    // create
    project.tasks.register<GitRepoPublishTask>(GitRepoPublishTask.NAME) {
      dependsOn(gitRepoPublishingTasks)
      gitService.set(gitServiceProvider)
      localRepoDir.set(initGitRepoTask.flatMap { it.localRepoDir })
      publishTasks.addAll(gitRepoPublishingTasks)
    }
  }


  private fun Gradle.registerGitService(
    settings: VcsMvnPublishSettings
  ): Provider<GitService> {
    log.lifecycle("[$PROJECT_NAME] registering GitService")

    return gradle.sharedServices.registerIfAbsent(
      GitService.NAME,
      GitService::class,
    ) {
      maxParallelUsages.set(1)

      parameters {
        gitExec.set(settings.gitExec)
        defaultOrigin.set("origin")
        porcelainEnabled.set(true)
      }
    }
  }


  /**
   * Get the origin URL of the current project.
   */
  private fun gitRemoteUriConvention(
    gitServiceProvider: Provider<GitService>,
    rootDir: Directory,
  ): Provider<String> {

    val rootDirProvider = objects.directoryProperty()
    rootDirProvider.set(rootDir)

    return gitServiceProvider
      .flatMap { gitService ->
        val result = gitService.configGet(rootDir.asFile, "remote.origin.url").trim()

        log.lifecycle("gitRemoteUriConvention: $result")

        providers.provider { result.ifBlank { null } }
      }
  }


  companion object {
    const val PROJECT_NAME = "VcsMvnPublish"
    const val TASK_GROUP = "vcs mvn publish"
    const val BASE_REPO_NAME = "vcs-mvn-publish"
    const val EXTENSION_NAME = "vcsMvnPublish"


    internal val Project.publishing: PublishingExtension
      get() = extensions.getByType()


    private infix fun URI.sameFileAs(dir: DirectoryProperty): Boolean =
      toURL().sameFile(dir.asFile.get().toURI().toURL())


    private val Project.pathEscaped: String
      get() {
        logger.lifecycle("escaping path $path, display name $displayName, is root ${project == rootProject}")
        return name.replace('-') { !it.isLetterOrDigit() }
//        return if (project == rootProject) {
//          "base"
//        } else {
//          displayName.replace('-') { !it.isLetterOrDigit() }
//        }
      }


    /** Replace characters that satisfy [matcher] with [new] */
    private fun String.replace(new: Char, matcher: (Char) -> Boolean): String =
      map { c -> if (matcher(c)) new else c }.joinToString("")
  }
}
