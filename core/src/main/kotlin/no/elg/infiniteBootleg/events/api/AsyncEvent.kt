package no.elg.infiniteBootleg.events.api

import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.Settings.handleWrongThreadAsyncEvents
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.stacktrace

open class AsyncEvent(vararg expectedThreadType: ThreadType) : Event {

  val dispatchedThreadType: ThreadType = ThreadType.currentThreadType()

  init {
    if (expectedThreadType.isNotEmpty() && dispatchedThreadType !in expectedThreadType) {
      val message = {
        "Expected the event ${this::class.simpleName} to be dispatched on one of ${expectedThreadType.contentToString()}, but it was created on a $dispatchedThreadType thread type"
      }
      when (handleWrongThreadAsyncEvents) {
        Settings.WrongThreadAsyncEventAction.LOG -> Main.logger().warn("AsyncEvent", message)
        Settings.WrongThreadAsyncEventAction.STACKTRACE -> Main.logger().warn("AsyncEvent", "${message()}\n${stacktrace()}")
        Settings.WrongThreadAsyncEventAction.THROW -> throw RuntimeException(message())
      }
    }
  }
}
