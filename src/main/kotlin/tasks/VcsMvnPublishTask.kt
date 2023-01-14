package dev.adamko.vcsmvnpub.tasks

import dev.adamko.vcsmvnpub.VcsMvnPublishPlugin
import org.gradle.api.DefaultTask

abstract class VcsMvnPublishTask: DefaultTask() {

  init {
    group = VcsMvnPublishPlugin.TASK_GROUP
  }
}
