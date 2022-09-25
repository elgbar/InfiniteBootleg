package no.elg.infiniteBootleg.events

interface CancellableEvent : Event {

  val cancelled: Boolean
}
