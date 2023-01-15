package no.elg.infiniteBootleg.events.api

import no.elg.infiniteBootleg.Main
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Capture events
 */
class EventsTracker(var log: Boolean) {

  private val recorded = ConcurrentLinkedQueue<EventEvent>()

  fun clear() {
    recorded.clear()
  }

  val recordedEvents: Collection<EventEvent> get() = recorded

  fun onEventListenedTo(event: Event, listener: EventListener<out Event>) {
    val recordedEvent = EventListenedToEvent(event, listener, Thread.currentThread().name, Main.inst().world?.tick, ZonedDateTime.now())
    recorded += recordedEvent
    if (log) {
      Main.logger().debug("EVENT TRACKER") { "Event $event listened to $recordedEvent" }
    }
  }

  fun onEventDispatched(event: Event) {
    val recordedEvent = RecordedEvent(event, Thread.currentThread().name, Main.inst().world?.tick, ZonedDateTime.now())
    recorded += recordedEvent
    if (log) {
      Main.logger().debug("EVENT TRACKER") { "Event dispatched: $recordedEvent" }
    }
  }

  fun onListenerRegistered(eventClass: Class<out Event>, listener: EventListener<out Event>) {
    val recordedEvent = ListenerEvent("registered", eventClass, listener, Thread.currentThread().name, Main.inst().world?.tick, ZonedDateTime.now())
    recorded += recordedEvent
    if (log) {
      Main.logger().debug("EVENT TRACKER") { "Listener registered for ${eventClass.simpleName}: $listener" }
    }
  }

  fun onListenerUnregistered(eventClass: Class<out Event>, listener: EventListener<out Event>) {
    val recordedEvent = ListenerEvent("unregistered", eventClass, listener, Thread.currentThread().name, Main.inst().world?.tick, ZonedDateTime.now())
    recorded += recordedEvent
    if (log) {
      Main.logger().debug("EVENT TRACKER") { "Listener UN-registered for ${eventClass.simpleName}: $listener" }
    }
  }
}

interface EventEvent {
  val thread: String
  val tick: Long?
  val time: ZonedDateTime
}

data class RecordedEvent(
  val event: Event,
  override val thread: String,
  override val tick: Long?,
  override val time: ZonedDateTime
) : EventEvent

data class ListenerEvent(
  val action: String,
  val eventClass: Class<out Event>,
  val listener: EventListener<out Event>,
  override val thread: String,
  override val tick: Long?,
  override val time: ZonedDateTime
) : EventEvent

data class EventListenedToEvent(
  val event: Event,
  val listener: EventListener<out Event>,
  override val thread: String,
  override val tick: Long?,
  override val time: ZonedDateTime
) : EventEvent
