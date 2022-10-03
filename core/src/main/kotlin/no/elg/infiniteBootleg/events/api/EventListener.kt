package no.elg.infiniteBootleg.events.api

@FunctionalInterface
fun interface EventListener<T> {

  /**
   * React and handle an event of type [T]
   *
   * @param event The event to handle

   */
  fun handle(event: T)
}
