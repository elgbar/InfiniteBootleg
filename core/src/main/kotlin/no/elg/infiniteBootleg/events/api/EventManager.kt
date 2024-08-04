package no.elg.infiniteBootleg.events.api

import com.google.errorprone.annotations.concurrent.GuardedBy
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.events.api.RegisteredEventListener.Companion.createRegisteredEventListener
import no.elg.infiniteBootleg.util.launchOnAsync
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

object EventManager {

  /**
   * The inner set is in reality a [WeakHashMap]
   */
  @GuardedBy("itself")
  val weakListeners: WeakHashMap<KClass<out Event>, MutableSet<EventListener<out Event>>> = WeakHashMap()

  @GuardedBy("itself")
  val strongListeners: MutableMap<KClass<out Event>, MutableSet<EventListener<out Event>>> = ConcurrentHashMap()
  val oneShotStrongRefs: MutableMap<EventListener<out Event>, RegisteredEventListener> = ConcurrentHashMap()

  val isLoggingAnyEvents: Boolean get() = eventsTracker?.logAnything ?: false
  val isLoggingEventsDispatched get() = eventsTracker?.logEventsDispatched ?: false
  val isLoggingEventsListenedTo get() = eventsTracker?.logEventsListenedTo ?: false
  val isLoggingEventListenersChange get() = eventsTracker?.logEventListenersChange ?: false

  var eventsTracker: EventsTracker? = if (Settings.debug) EventsTracker() else null
    private set

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
    val eventListeners: MutableSet<EventListener<out Event>> =
      if (keepStrongReference) {
        strongListeners.getOrPut(eventClass) { Collections.newSetFromMap(ConcurrentHashMap()) }
      } else {
        synchronized(weakListeners) {
          weakListeners.getOrPut(eventClass) { Collections.newSetFromMap(WeakHashMap()) }
        }
      }
    synchronized(eventListeners) {
      eventListeners.add(listener)
      eventsTracker?.onListenerRegistered(eventClass, listener)
    }
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
    oneShotStrongRefs[listener] = registerListener<T> {
      if (handled) {
        return@registerListener
      }
      handled = true

      listener.handle(it)

      removeOneShotRef(listener)
    }
  }

  fun removeOneShotRef(listener: EventListener<out Event>) {
    // Remove from another thread to not cause concurrent modification
    launchOnAsync {
      val storedThis = oneShotStrongRefs.remove(listener)
      if (storedThis != null) {
        storedThis.removeListener()
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
  inline fun <reified T : Event> dispatchEvent(event: T) {
    eventsTracker?.onEventDispatched(event)
    val eventListeners: MutableSet<EventListener<out Event>> =
      synchronized(weakListeners) { weakListeners[T::class] } ?: strongListeners[T::class] ?: return

    synchronized(eventListeners) {
      val correctListeners = eventListeners.filterIsInstance<EventListener<T>>()
      for (listener in correctListeners) {
        eventsTracker?.onEventListenedTo(event, listener)
        listener.handle(event)
      }
    }
  }

  /**
   * Prevent (by removing) the given listener from receiving any more events
   */
  @Deprecated("Use registeredEventListerne")
  fun <T : Event> removeListener(listener: EventListener<T>, eventClass: KClass<T>) {
    val eventListeners: MutableSet<EventListener<out Event>> =
      synchronized(weakListeners) { weakListeners[eventClass] } ?: strongListeners[eventClass] ?: return
    synchronized(eventListeners) {
      eventListeners.remove(listener)
      eventsTracker?.onListenerUnregistered(eventClass, listener)
    }
  }

  /**
   * Remove all listeners registered
   */
  fun clear() {
    synchronized(weakListeners) {
      weakListeners.clear()
    }
    strongListeners.clear()
    oneShotStrongRefs.clear()
  }
}
