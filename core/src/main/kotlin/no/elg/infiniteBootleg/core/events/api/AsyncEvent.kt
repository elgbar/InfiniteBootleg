package no.elg.infiniteBootleg.core.events.api

import no.elg.infiniteBootleg.core.Settings

open class AsyncEvent(vararg expectedThreadType: ThreadType) : Event {

  val dispatchedThreadType: ThreadType = ThreadType.currentThreadType()

  init {
    if (expectedThreadType.isNotEmpty() && dispatchedThreadType !in expectedThreadType) {
      Settings.handleWrongThreadAsyncEvents.handle {
        "Expected the event ${this::class.simpleName} to be dispatched on one of ${expectedThreadType.contentToString()}, but it was created on a $dispatchedThreadType thread type"
      }
    }
  }
}
