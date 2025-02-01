package no.elg.infiniteBootleg.core.events.api

interface CancellableEvent : Event {

  val cancelled: Boolean
}
