package no.elg.infiniteBootleg.core.events.api

import no.elg.infiniteBootleg.core.Settings

/**
 * An event which is expected to run one of the thread types specified in [expectedThreadType]
 */
open class ThreadedEvent(vararg expectedThreadType: ThreadType) : Event {

  val dispatchedThreadType: ThreadType = ThreadType.currentThreadType()

  init {
    if (expectedThreadType.isNotEmpty() && dispatchedThreadType !in expectedThreadType) {
      Settings.handleWrongThreadAsyncEvents.handle {
        "Expected the event ${this::class.simpleName} to be dispatched on one of ${expectedThreadType.contentToString()}, but it was created on a $dispatchedThreadType thread type"
      }
    }
  }
}
