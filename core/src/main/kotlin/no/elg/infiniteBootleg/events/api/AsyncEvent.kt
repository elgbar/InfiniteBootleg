package no.elg.infiniteBootleg.events.api

import no.elg.infiniteBootleg.Settings.handleWrongThreadAsyncEvents

open class AsyncEvent(vararg expectedThreadType: ThreadType) : Event {

  val dispatchedThreadType: ThreadType = ThreadType.currentThreadType()

  init {
    if (expectedThreadType.isNotEmpty() && dispatchedThreadType !in expectedThreadType) {
      handleWrongThreadAsyncEvents.handle("AsyncEvent") {
        "Expected the event ${this::class.simpleName} to be dispatched on one of ${expectedThreadType.contentToString()}, but it was created on a $dispatchedThreadType thread type"
      }
    }
  }
}
