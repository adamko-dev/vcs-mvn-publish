package dev.adamko.vcsmvnpub

import dev.adamko.vcsmvnpub.tasks.GitRepoInitTask
import dev.adamko.vcsmvnpub.tasks.GitRepoPublishTask
import java.net.URI
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.withType


abstract class VcsMvnPublishPlugin @Inject constructor(
  private val providers: ProviderFactory,
//  private val objects: ObjectFactory,
//  private val files: FileOperations,
//  private val fileSys: FileSystemOperations,
) : Plugin<Project>, ProviderFactory by providers {


  private val log: Logger = Logging.getLogger(this::class.java)


  override fun apply(project: Project) {

    project.plugins.withType<PublishingPlugin>().configureEach {
      log.lifecycle("applying $PROJECT_NAME")

      val settings: VcsMvnPublishSettings = project.createExtension()

      val gitServiceProvider = project.gradle.registerGitService(settings)

      log.lifecycle("configuring GitRepo")

      settings.gitRepos.all {
        project.configureGitRepo(settings, this, gitServiceProvider)
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
        gitProjectRepoDir.convention { rootProject.layout.projectDirectory.asFile }
      }
  }


  private fun Project.configureGitRepo(
    settings: VcsMvnPublishSettings,
    gitRepo: VcsMvnGitRepo,
    gitServiceProvider: Provider<GitService>,
  ) {
    log.lifecycle("[$PROJECT_NAME] configuring git repo $gitRepo")

    gitRepo.remoteUri.convention(
      gitRemoteUriConvention(
        gitServiceProvider,
        settings.gitProjectRepoDir,
      )
    )

    // register an 'init-repo' task specifically for this GitRepo
    val gitRepoInitTask = project.tasks.register<GitRepoInitTask>(GitRepoInitTask.NAME) {
      artifactBranch.convention(gitRepo.artifactBranch)
      localRepoDir.convention(gitRepo.localRepoDir)
      remoteUri.convention(gitRepo.remoteUri)
      artifactBranchCreateMode.convention(gitRepo.artifactBranchCreateMode)

      gitService.convention(gitServiceProvider)
    }

    // ensure all git repos are initialized before any publishing tasks run
    tasks.withType<PublishToMavenRepository>().configureEach {
      mustRunAfter(gitRepoInitTask)
    }

    // create a new local Maven repository
    publishing.repositories.maven {
      name = PROJECT_NAME
      val artifactDir: String? = gitRepo.repoArtifactDir.orNull
      val targetDir: Provider<Directory> = when {
        artifactDir.isNullOrEmpty() -> gitRepo.localRepoDir
        else                        -> gitRepo.localRepoDir.dir(artifactDir)
      }
      setUrl(targetDir.get().asFile)
    }

    // find the publishing tasks created by the local Maven repo
    val gitRepoPublishingTasks = tasks
      .withType<PublishToMavenRepository>()
      .matching { publishTask ->
        val matching = publishTask.repository.url sameFileAs gitRepo.localRepoDir
        log.lifecycle("found matching ${publishTask.name}")
        matching
      }

    // publishing depends on the git repo being set up and checked out
    gitRepoPublishingTasks.configureEach {
      dependsOn(gitRepoInitTask)
    }

    // create a task to commit and push the artifacts
    project.tasks.register<GitRepoPublishTask>(GitRepoPublishTask.NAME) {
      dependsOn(gitRepoPublishingTasks)
      dependsOn(gitRepoInitTask)
      dependsOn(tasks.provider.publish)
      gitService.set(gitServiceProvider)
      localRepoDir.set(gitRepo.localRepoDir)
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
      }
    }
  }


  /**
   * Get the origin URL of the current project.
   */
  private fun gitRemoteUriConvention(
    gitServiceProvider: Provider<GitService>,
    gitProjectRepoDir: RegularFileProperty,
  ): Provider<String> {
    return gitServiceProvider
      .zip(gitProjectRepoDir) { service, dir -> service to dir }
      .flatMap { (service, dir) ->
        service.configGetRemoteOriginUrl(dir.asFile)
      }
  }


  /** task provider helpers - help make the script configurations shorter & more legible */
  private val TaskContainer.provider: TaskProviders get() = TaskProviders(this)


  /** Lazy task providers */
  private inner class TaskProviders(private val tasks: TaskContainer) {

    val publish: Provider<Task>
      get() = provider(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)

    // Workaround for https://github.com/gradle/gradle/issues/16543
    private inline fun <reified T : Task> provider(taskName: String): Provider<T> =
      providers
        .provider { taskName }
        .flatMap { tasks.named<T>(it) }
  }


  companion object {
    const val PROJECT_NAME = "VcsMvnPublish"
    const val EXTENSION_NAME = "vcsMvnPublish"
    const val TASK_GROUP = "vcs mvn publish"
    const val BASE_REPO_NAME = "vcs-mvn-publish"


    internal val Project.publishing: PublishingExtension
      get() = extensions.getByType()


    private infix fun URI.sameFileAs(dir: DirectoryProperty): Boolean =
      toURL().sameFile(dir.asFile.get().toURI().toURL())


    private val Project.pathEscaped: String
      get() {
        logger.lifecycle("escaping path $path, display name $displayName, is root ${project == rootProject}")
        return name.replace('-') { !it.isLetterOrDigit() }
      }


    /** Replace characters that satisfy [matcher] with [new] */
    private fun String.replace(new: Char, matcher: (Char) -> Boolean): String =
      map { c -> if (matcher(c)) new else c }.joinToString("")


  }
}
