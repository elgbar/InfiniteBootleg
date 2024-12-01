package no.elg.infiniteBootleg.events.api

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.events.api.RegisteredEventListener.Companion.createRegisteredEventListener
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.util.launchOnEvents
import java.lang.Boolean.TRUE
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

object EventManager {

  val weakListeners: Cache<KClass<out Event>, Cache<EventListener<out Event>, Boolean>> = Caffeine.newBuilder().build()
  val strongListeners: Cache<KClass<out Event>, Cache<EventListener<out Event>, Boolean>> = Caffeine.newBuilder().build()

  val oneShotStrongRefs: Cache<EventListener<out Event>, RegisteredEventListener> = Caffeine.newBuilder().build()

  val isLoggingAnyEvents: Boolean get() = eventsTracker?.logAnything ?: false
  val isLoggingEventsDispatched get() = eventsTracker?.logEventsDispatched ?: false
  val isLoggingEventsListenedTo get() = eventsTracker?.logEventsListenedTo ?: false
  val isLoggingEventListenersChange get() = eventsTracker?.logEventListenersChange ?: false

  var eventsTracker: EventsTracker? = if (Settings.debug) EventsTracker() else null
    private set

  val activeListeners = AtomicLong(0) // active number of listeners
  val activeOneTimeRefListeners = AtomicLong(0)
  val registeredWeakListeners = AtomicLong(0)
  val registeredStrongListeners = AtomicLong(0)

  val unregisteredListeners = AtomicLong(0)
  val dispatchedEvents = AtomicLong(0)
  val listenerListenedToEvent = AtomicLong(0)

  fun getOrCreateEventsTracker(): EventsTracker = eventsTracker ?: EventsTracker().also { eventsTracker = it }

  /**
   * React to events [dispatchEvent]-ed by someone else.
   *
   * Note that the listeners are stored as [WeakReference]s, unless [keepStrongReference] is `true`, which mean that they might be garbage-collected if they are not stored as (store) references somewhere.
   * This is to automatically un-register listeners which no longer can react to events.
   */
  inline fun <reified T : Event> registerListener(keepStrongReference: Boolean = false, listener: EventListener<T>): RegisteredEventListener =
    registerListener(keepStrongReference, T::class, listener)

  fun <T : Event> registerListener(keepStrongReference: Boolean = false, eventClass: KClass<T>, listener: EventListener<T>): RegisteredEventListener {
    activeListeners.incrementAndGet()
    val eventListeners: Cache<EventListener<out Event>, Boolean> =
      if (keepStrongReference) {
        registeredStrongListeners.incrementAndGet()
        strongListeners.get(eventClass) { Caffeine.newBuilder().build<EventListener<out Event>, Boolean>() }
      } else {
        registeredWeakListeners.incrementAndGet()
        weakListeners.get(eventClass) { Caffeine.newBuilder().weakKeys().build<EventListener<out Event>, Boolean>() }
      }

    eventListeners.put(listener, TRUE)
    eventsTracker?.onListenerRegistered(eventClass, listener)
    return createRegisteredEventListener(listener, eventClass)
  }

  /**
   * React to events [dispatchEvent]-ed by someone else.
   *
   * Note that the listeners are stored as [WeakReference]s, unless [keepStrongReference] is `true`, which mean that they might be garbage-collected if they are not stored as (store) references somewhere.
   * This is to automatically un-register listeners which no longer can react to events.
   */
  inline fun <reified T : Event> registerListener(
    keepStrongReference: Boolean = false,
    crossinline filter: (T) -> Boolean,
    crossinline listener: T.() -> Unit
  ): RegisteredEventListener =
    registerListener<T>(keepStrongReference) { event ->
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
  inline fun <reified T : AsyncEvent> dispatchEventAsync(event: T, reason: String? = null) {
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
    forEachListener<T>(eventClass) { listener ->
      listenerListenedToEvent.incrementAndGet()
      eventsTracker?.onEventListenedTo(event, listener)
      listener.handle(event)
    }
    EventStatistics.reportDispatch(event, reason)
    eventsTracker?.onEventDispatched(event)
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T : Event> forEachListener(eventClass: KClass<T>, action: (EventListener<T>) -> Unit) {
    weakListeners.getIfPresent(eventClass)?.asMap()?.keys?.forEach { action(it as EventListener<T>) }
    strongListeners.getIfPresent(eventClass)?.asMap()?.keys?.forEach { action(it as EventListener<T>) }
  }

  /**
   * Remove all listeners registered
   */
  fun clear() {
    weakListeners.invalidateAll()
    strongListeners.invalidateAll()
    oneShotStrongRefs.invalidateAll()
    EventStatistics.clear()
  }
}
