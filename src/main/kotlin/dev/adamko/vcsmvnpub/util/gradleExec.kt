package dev.adamko.vcsmvnpub.util

import java.io.ByteArrayOutputStream
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec


private val logger: Logger = Logging.getLogger("ExecOperations.execCapture")

internal fun ExecOperations.execCapture(
  throwError: Boolean = false,
  configure: ExecSpec.() -> Unit,
): ExecCaptureResult {

  val (result, output) = ByteArrayOutputStream().use { os ->
    exec {
      isIgnoreExitValue = true
      standardOutput = os
      errorOutput = os
      configure()
    } to os.toString()
  }

  return if (result.exitValue != 0) {
    logger.error(output)
    if (throwError) result.rethrowFailure()
    ExecCaptureResult.Error(output, result)
  } else {
    ExecCaptureResult.Success(output, result)
  }
}


internal sealed class ExecCaptureResult(
  val output: String,
  private val result: ExecResult,
) : ExecResult by result {

  internal class Success(output: String, result: ExecResult) : ExecCaptureResult(output, result)

  internal class Error(output: String, result: ExecResult) : ExecCaptureResult(output, result)

}
