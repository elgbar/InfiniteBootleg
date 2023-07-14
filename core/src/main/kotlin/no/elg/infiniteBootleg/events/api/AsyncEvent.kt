package no.elg.infiniteBootleg.events.api

import no.elg.infiniteBootleg.main.Main

open class AsyncEvent(vararg expectedThreadType: ThreadType) : Event {

  val dispatchedThreadType: ThreadType = ThreadType.currentThreadType()

  init {
    if (expectedThreadType.isNotEmpty() && dispatchedThreadType !in expectedThreadType) {
      Main.logger().warn("AsyncEvent") {
        "Expected the event ${this::class.simpleName} to be dispatched on one of ${expectedThreadType.contentToString()}, but it was created on a $dispatchedThreadType thread type"
      }
//      RuntimeException().printStackTrace()
    }
  }
}
