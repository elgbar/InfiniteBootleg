package no.elg.infiniteBootleg.core.events.api

interface ReasonedEvent : Event {

  val reason: String
}
