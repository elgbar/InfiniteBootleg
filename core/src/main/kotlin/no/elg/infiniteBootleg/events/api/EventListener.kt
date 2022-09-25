package no.elg.infiniteBootleg.events.api

interface EventListener<T> {

  fun handle(event: T)
}
