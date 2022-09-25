package no.elg.infiniteBootleg.events.api

interface CancellableEvent : Event {

  val cancelled: Boolean
}
