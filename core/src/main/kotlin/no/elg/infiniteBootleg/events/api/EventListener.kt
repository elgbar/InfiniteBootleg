package no.elg.infiniteBootleg.events.api

@FunctionalInterface
fun interface EventListener<T> {

  fun handle(event: T)
}
