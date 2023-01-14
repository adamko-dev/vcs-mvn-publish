package dev.adamko.vcsmvnpub

import dev.adamko.vcsmvnpub.tasks.GitRepoInitTask
import dev.adamko.vcsmvnpub.tasks.GitRepoPublishTask
import dev.adamko.vcsmvnpub.tasks.VcsMvnPublishTask
import dev.adamko.vcsmvnpub.util.asConsumer
import dev.adamko.vcsmvnpub.util.asProvider
import dev.adamko.vcsmvnpub.util.uppercaseFirstChar
import java.net.URI
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.kotlin.dsl.*


abstract class VcsMvnPublishPlugin : Plugin<Project> {


  private val log: Logger = Logging.getLogger(this::class.java)


  override fun apply(project: Project) {
    log.lifecycle("configuring GitRepo")

    val configurations = project.createConfigurations()

    val settings: VcsMvnPublishSettings = project.createExtension()
    val gitServiceProvider = project.gradle.registerGitService(settings)


    settings.gitRepos.all {
      val gitRepo = this

      project.plugins.withType<PublishingPlugin>().configureEach {
        log.lifecycle("applying $PROJECT_NAME")

        project.setupLocalGitRepoPublication(settings, gitRepo, gitServiceProvider)
      }

      // register an 'init-repo' task specifically for this GitRepo
      val gitRepoInitTask =
        project.tasks.register<GitRepoInitTask>(GitRepoInitTask.TASK_NAME + gitRepo.name.uppercaseFirstChar()) {
          branch.convention(gitRepo.artifactBranch)
          localRepoDir.convention(gitRepo.localRepoDir)
          remoteUri.convention(gitRepo.remoteUri)
          branchCreateMode.convention(gitRepo.artifactBranchCreateMode)

          gitService.convention(gitServiceProvider)
        }

      // ensure all git repos are initialized before any publishing tasks run
      project.tasks.withType<PublishToMavenRepository>().configureEach {
        mustRunAfter(gitRepoInitTask)
      }

      // find the publishing tasks created by maven-publish plugin
      // (there should only be one, but Gradle API doesn't have a 'get one' provider)
      val gitRepoPublishingTasks = project.tasks
        .withType<PublishToMavenRepository>()
        .matching { publishTask ->
          val matching = publishTask.repository?.url.sameFileAs(gitRepo.localRepoDir)
          log.info("gitRepo '${gitRepo.name}' has publishing task ${publishTask.path}")
          matching
        }

      configurations.publicationElements.configure {
        outgoing {
          artifact(gitRepo.localRepoDir) {
            builtBy(gitRepoPublishingTasks)
          }
        }
      }

      // publishing depends on the git repo being set up and checked out
      gitRepoPublishingTasks.configureEach {
        dependsOn(gitRepoInitTask)
      }

      // create a task to commit and push the artifacts
      project.tasks.register<GitRepoPublishTask>(GitRepoPublishTask.TASK_NAME + gitRepo.name.uppercaseFirstChar()) {
        dependsOn(gitRepoPublishingTasks)
        dependsOn(gitRepoInitTask)
        mustRunAfter(project.tasks.withType<GitRepoInitTask>())
        publishedRepos.from(configurations.publications.map {
          it.incoming.artifactView { lenient(true) }.files
        })
        gitService.convention(gitServiceProvider)
        localRepoDir.convention(gitRepo.localRepoDir)
//      publishTasks.addAllLater(gitRepoPublishingTasksProvider)
        gitPushToRemoteEnabled.convention(settings.gitPushToRemoteEnabled)
      }


      // register lifecycle tasks
      val gitRepoInitLifecycleTask =
        project.tasks.maybeCreate<VcsMvnPublishTask>(GitRepoInitTask.TASK_NAME).apply {
          description = "Run all GitRepoInit tasks"
          dependsOn(project.tasks.withType<GitRepoInitTask>())
        }
      project.tasks.maybeCreate<VcsMvnPublishTask>("").apply {
        description = "Run all GitRepoPublish tasks"
        dependsOn(gitRepoInitLifecycleTask)
        dependsOn(project.tasks.withType<GitRepoPublishTask>())
      }
    }
  }


  private fun Project.createExtension(): VcsMvnPublishSettings {
    log.lifecycle("creating $PROJECT_NAME extension on ${project.name}")

    return extensions.create<VcsMvnPublishSettings>(EXTENSION_NAME)
      .apply {
        localPublishDir.convention(
          rootProject.layout.projectDirectory.dir(".gradle/$BASE_REPO_NAME")
        )
        gitExec.convention("git")
        gitPushToRemoteEnabled.convention(true)
        gitProjectRepoDir.convention { rootProject.layout.projectDirectory.asFile }
      }
  }


  private fun Project.createConfigurations(): Configurations {
    val configurations = Configurations(project)

    if (project.isRootProject()) {
      configurations.publications.configure {
        defaultDependencies {
          addAllLater(
            providers.provider {
              project.allprojects.map { subproject ->
                project.dependencies.create(subproject)
              }
            }
          )
        }
      }
    }

    return configurations
  }

  /** Add a new Maven repo to the [PublishingPlugin] config */
  private fun Project.setupLocalGitRepoPublication(
    settings: VcsMvnPublishSettings,
    gitRepo: VcsMvnGitRepo,
    gitServiceProvider: Provider<GitService>,
  ) {
    log.lifecycle("[$PROJECT_NAME] configuring git repo '${gitRepo.name}'")

    gitRepo.remoteUri.convention(
      gitRemoteUriConvention(
        gitServiceProvider,
        settings.gitProjectRepoDir,
      )
    )

    // create a new local Maven repository
    publishing.repositories.maven {
      name = "VcsMvnPublish${gitRepo.name.uppercaseFirstChar()}"
      setUrl(
        providers.zip(
          gitRepo.localRepoDir,
          gitRepo.repoArtifactDir.orElse(""),
        ) { localDir, artifactDir ->
          when {
            artifactDir.isEmpty() -> localDir
            else                  -> localDir.dir(artifactDir)
          }.asFile
        }
      )
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
        gitExec.convention(settings.gitExec)
        defaultOrigin.convention("origin")
        gitDirFlagEnabled.convention(true)
      }
    }
  }


  /** Get the origin URL of the current project. */
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


//  /** task provider helpers - help make the script configurations shorter & more legible */
//  private val TaskContainer.provider: TaskProviders get() = TaskProviders(this)
//
//
//  /** Lazy task providers */
//  private inner class TaskProviders(private val tasks: TaskContainer) {
//
//    val publish: Provider<Task>
//      get() = provider(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
//
//    // Workaround for https://github.com/gradle/gradle/issues/16543
//    private inline fun <reified T : Task> provider(taskName: String): Provider<T> =
//      providers
//        .provider { taskName }
//        .flatMap { tasks.named<T>(it) }
//  }

  class Configurations(
    project: Project
  ) {
    private val objects = project.objects

    private fun AttributeContainer.publicationAttributes() {
      attribute(Usage.USAGE_ATTRIBUTE, objects.named("dev.adamko.vcs-mvn-publish"))
      attribute(Category.CATEGORY_ATTRIBUTE, objects.named("publication"))
    }

    val publicationElements: NamedDomainObjectProvider<Configuration> =
      project.configurations.register(PUBLICATION_ELEMENTS_NAME) {
        description = "provide a vcs-mvn-publish publication to other subprojects"
        asProvider()
        attributes { publicationAttributes() }
      }

    val publications: NamedDomainObjectProvider<Configuration> =
      project.configurations.register(PUBLICATION_NAME) {
        description = "consume vcs-mvn-publish publications from other subprojects"
        asConsumer()
        attributes { publicationAttributes() }
      }

    companion object {
      const val PUBLICATION_ELEMENTS_NAME = "vcsMvnPublicationElements"
      const val PUBLICATION_NAME = "vcsMvnPublication"
    }
  }

  companion object {
    const val PROJECT_NAME = "VcsMvnPublish"
    const val EXTENSION_NAME = "vcsMvnPublish"
    const val TASK_GROUP = "vcs mvn publish"
    const val BASE_REPO_NAME = "vcs-mvn-publish"


    internal val Project.publishing: PublishingExtension
      get() = extensions.getByType()


    private fun URI?.sameFileAs(dir: DirectoryProperty): Boolean =
      this != null && toURL().sameFile(dir.asFile.get().toURI().toURL())


//    private val Project.pathEscaped: String
//      get() {
//        logger.lifecycle("escaping path $path, display name $displayName, is root ${project == rootProject}")
//        return name.replace('-') { !it.isLetterOrDigit() }
//      }

    private fun Project.isRootProject(): Boolean = rootProject == project
  }
}
