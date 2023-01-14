package dev.adamko.vcsmvnpub

import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gradle.api.Project


class VcsMvnPublishPluginTest : FunSpec({

  context("verify plugin can be applied to project") {

    val vmpPlugin = object : VcsMvnPublishPlugin() {}

    val projectMock: Project = mockk(relaxed = true) {
      every { extensions } returns mockk {
        every {
          create<VcsMvnPublishSettings>("vcsMvnPublish", any())
        } returns mockk(relaxed = true)
      }
    }

    test("expect plugin creates extension") {
      vmpPlugin.apply(projectMock)

      verify(exactly = 1) { projectMock.extensions }
    }
  }
})
