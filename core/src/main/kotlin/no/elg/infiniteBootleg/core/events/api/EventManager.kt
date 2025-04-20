package no.elg.infiniteBootleg.core.events.api

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.core.events.api.EventManager.dispatchEventAsync
import no.elg.infiniteBootleg.core.util.launchOnAsync
import no.elg.infiniteBootleg.core.util.launchOnEvents
import java.lang.Boolean.TRUE
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

object EventManager {

  val weakListeners: Cache<KClass<out Event>, Cache<EventListener<out Event>, Boolean>> = Caffeine.newBuilder().build()

  val oneShotStrongRefs: Cache<EventListener<out Event>, RegisteredEventListener> = Caffeine.newBuilder().build()

  val isLoggingAnyEvents: Boolean get() = eventsTracker?.logAnything ?: false
  val isLoggingEventsDispatched get() = eventsTracker?.logEventsDispatched ?: false
  val isLoggingEventsListenedTo get() = eventsTracker?.logEventsListenedTo ?: false
  val isLoggingEventListenersChange get() = eventsTracker?.logEventListenersChange ?: false

  var eventsTracker: EventsTracker? = null
    private set

  val activeListeners = AtomicLong(0) // active number of listeners
  val activeOneTimeRefListeners = AtomicLong(0)
  val registeredWeakListeners = AtomicLong(0)

  val unregisteredListeners = AtomicLong(0)
  val dispatchedEvents = AtomicLong(0)
  val listenerListenedToEvent = AtomicLong(0)

  fun getOrCreateEventsTracker(): EventsTracker = eventsTracker ?: EventsTracker().also { eventsTracker = it }

  /**
   * React to events [dispatchEvent]-ed by someone else.
   *
   * Note that the listeners are stored as [WeakReference]s, which mean that they might be garbage-collected if they are not stored as references somewhere.
   * This is to automatically un-register listeners which no longer can react to events.
   */
  inline fun <reified T : Event> registerListener(listener: EventListener<T>): RegisteredEventListener = registerListener(T::class, listener)

  /**
   * React to events [dispatchEvent]-ed by someone else.
   *
   * Note that the listeners are stored as [WeakReference]s, which mean that they might be garbage-collected if they are not stored as references somewhere.
   * This is to automatically un-register listeners which no longer can react to events.
   */
  fun <T : Event> registerListener(eventClass: KClass<T>, listener: EventListener<T>): RegisteredEventListener {
    activeListeners.incrementAndGet()
    val eventListeners: Cache<EventListener<out Event>, Boolean> =
      weakListeners.get(eventClass) { Caffeine.newBuilder().weakKeys().build<EventListener<out Event>, Boolean>() }.also {
        registeredWeakListeners.incrementAndGet()
      }

    eventListeners.put(listener, TRUE)
    eventsTracker?.onListenerRegistered(eventClass, listener)
    return RegisteredEventListener.Companion.createRegisteredEventListener(listener, eventClass)
  }

  /**
   * React to events [dispatchEvent]-ed by someone else.
   *
   * Note that the listeners are stored as [WeakReference]s, which mean that they might be garbage-collected if they are not stored as references somewhere.
   * This is to automatically un-register listeners which no longer can react to events.
   */
  inline fun <reified T : Event> registerListener(crossinline filter: (T) -> Boolean, crossinline listener: T.() -> Unit): RegisteredEventListener =
    registerListener<T> { event ->
      if (filter(event)) {
        listener(event)
      }
    }

  /**
   * React to the next event [dispatchEvent]-ed of a certain type.
   *
   * As the listener is only meant to listen to a single event it is not a requirement to have a strong reference to the [listener].
   */
  inline fun <reified T : Event> oneShotListener(listener: EventListener<T>) {
    var handled = false // Prevents the listener from being called multiple times
    activeOneTimeRefListeners.incrementAndGet()
    oneShotStrongRefs.put(
      listener,
      registerListener<T> {
        if (handled) {
          return@registerListener
        }
        handled = true

        listener.handle(it)

        removeOneShotRef(listener)
      }
    )
  }

  fun removeOneShotRef(listener: EventListener<out Event>) {
    // Remove from another thread to not cause concurrent modification
    launchOnAsync {
      val storedThis = oneShotStrongRefs.getIfPresent(listener)
      if (storedThis != null) {
        storedThis.removeListener()
        activeOneTimeRefListeners.decrementAndGet()
      } else {
        logger.error { "Could not remove one shot listener $listener" }
      }
    }
  }

  /**
   * Notify listeners of the given event
   *
   * @param event The event to notify about
   */
  inline fun <reified T : Event> dispatchEventAsync(event: T, reason: String? = null) {
    launchOnEvents { dispatchEvent(T::class, event, reason) }
  }

  /**
   * Notify listeners of the given event
   *
   * @param event The event to notify about
   *
   * @see dispatchEventAsync
   */
  inline fun <reified T : Event> dispatchEvent(event: T, reason: String? = null) = dispatchEvent(T::class, event, reason)

  // impl note: Not inline to make it easier to track during profiling/debugging
  fun <T : Event> dispatchEvent(eventClass: KClass<T>, event: T, reason: String? = null) {
    dispatchedEvents.incrementAndGet()

    weakListeners.getIfPresent(eventClass)?.asMap()?.keys?.forEach {
      try {
        @Suppress("UNCHECKED_CAST")
        val listener = it as EventListener<T>
        listenerListenedToEvent.incrementAndGet()
        eventsTracker?.onEventListenedTo(event, listener)
        listener.handle(event)
      } catch (e: Exception) {
        logger.error(e) { "Failed to dispatch event $event to listener $it" }
      }
    }
    EventStatistics.reportDispatch(event, reason)
    eventsTracker?.onEventDispatched(event)
  }

  /**
   * Remove all listeners registered
   */
  fun clear() {
    weakListeners.invalidateAll()
    oneShotStrongRefs.invalidateAll()
    EventStatistics.clear()
  }
}
