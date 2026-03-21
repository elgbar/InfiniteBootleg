package no.elg.infiniteBootleg.core.console

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.main.Main

private val logger = KotlinLogging.logger {}

/**
 * On which thread to call this command. If this annotation is not specified, then there is no guarantee which thread it will run on.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CallOnThreadyType(

  /**
   * Which thread this command should be called on.
   */
  val value: ExecutionThread
) {
  companion object {
    fun CallOnThreadyType?.handle(func: () -> Unit) {
      when (this?.value) {
        ExecutionThread.PHYSICS -> {
          val world = Main.inst().world
          if (world != null) {
            ThreadType.PHYSICS.launchOrRun(world) { func() }
          } else {
            logger.error { "The command is not available as there is no active world." }
          }
        }

        ExecutionThread.RENDER -> ThreadType.RENDER.launchOrRun { func() }
        null -> func()
      }
    }
  }
}

enum class ExecutionThread {
  /**
   * Execute in the context of a physics world. If no world is available then the command will not be executed.
   */
  PHYSICS,

  /**
   * Execute on the main/render thread
   */
  RENDER
}
