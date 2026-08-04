package no.elg.infiniteBootleg.core.events.api

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.main.Main
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

/**
 * Capture events
 */
class EventsTracker(var log: Int = LOG_NOTHING) {

  private val recorded = ConcurrentLinkedQueue<EventEvent>()

  fun clear() {
    recorded.clear()
  }

  val recordedEvents: Collection<EventEvent> get() = recorded

  val logAnything get() = log != LOG_NOTHING
  val logEventsDispatched get() = log and LOG_EVENTS_DISPATCHED != 0
  val logEventsListenedTo get() = log and LOG_EVENTS_LISTENED_TO != 0
  val logEventListenersChange get() = log and LOG_EVENT_LISTENERS_CHANGE != 0

  fun onEventListenedTo(event: Event, listener: EventListener<out Event>) {
    val recordedEvent = EventListenedToEvent(event, listener, Thread.currentThread().name, Main.inst().world?.tick, ZonedDateTime.now())
    recorded += recordedEvent
    if (logEventsListenedTo) {
      logger.info { "Event $event listened to $recordedEvent" }
    }
  }

  fun onEventDispatched(event: Event) {
    val recordedEvent = RecordedEvent(event, Thread.currentThread().name, Main.inst().world?.tick, ZonedDateTime.now())
    recorded += recordedEvent
    if (logEventsDispatched) {
      logger.info { "Event dispatched: $recordedEvent" }
    }
  }

  fun onListenerRegistered(eventClass: KClass<out Event>, listener: EventListener<out Event>) {
    val recordedEvent = ListenerEvent("registered", eventClass, listener, Thread.currentThread().name, Main.inst().world?.tick, ZonedDateTime.now())
    recorded += recordedEvent
    if (logEventListenersChange) {
      logger.info { "Listener registered for ${eventClass.simpleName}: $listener" }
    }
  }

  fun onListenerUnregistered(eventClass: KClass<out Event>, listener: EventListener<out Event>) {
    val recordedEvent = ListenerEvent("unregistered", eventClass, listener, Thread.currentThread().name, Main.inst().world?.tick, ZonedDateTime.now())
    recorded += recordedEvent
    if (logEventListenersChange) {
      logger.info { "Listener UN-registered for ${eventClass.simpleName}: $listener" }
    }
  }

  companion object {
    const val LOG_NOTHING = 0
    const val LOG_EVENTS_DISPATCHED = 1
    const val LOG_EVENTS_LISTENED_TO = 2
    const val LOG_EVENT_LISTENERS_CHANGE = 4
    const val LOG_EVERYTHING = LOG_EVENTS_DISPATCHED or LOG_EVENTS_LISTENED_TO or LOG_EVENT_LISTENERS_CHANGE
  }
}

interface EventEvent {
  val thread: String
  val tick: Long?
  val time: ZonedDateTime
}

data class RecordedEvent(val event: Event, override val thread: String, override val tick: Long?, override val time: ZonedDateTime) : EventEvent

data class ListenerEvent(
  val action: String,
  val eventClass: KClass<out Event>,
  val listener: EventListener<out Event>,
  override val thread: String,
  override val tick: Long?,
  override val time: ZonedDateTime
) : EventEvent

data class EventListenedToEvent(val event: Event, val listener: EventListener<out Event>, override val thread: String, override val tick: Long?, override val time: ZonedDateTime) :
  EventEvent
