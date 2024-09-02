package no.elg.infiniteBootleg.events.api

interface ReasonedEvent : Event {

  val reason: String
}
