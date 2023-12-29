package no.elg.infiniteBootleg.events.api

import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.Main
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.concurrent.GuardedBy
import kotlin.reflect.KClass

object EventManager {

  /**
   * The inner set is in reality a [WeakHashMap]
   */
  @GuardedBy("itself")
  val listeners: WeakHashMap<KClass<out Event>, MutableSet<EventListener<out Event>>> = WeakHashMap()
  val oneShotStrongRefs: MutableMap<EventListener<out Event>, EventListener<out Event>> = ConcurrentHashMap()

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
   * Note that the listeners are stored as [WeakReference]s, which mean that they might be garbage-collected if they are not stored as (store) references somewhere.
   * This is to automatically un-register listeners which no longer can react to events.
   */
  inline fun <reified T : Event> registerListener(listener: EventListener<T>): EventListener<T> {
    val eventListeners: MutableSet<EventListener<out Event>>
    synchronized(listeners) {
      eventListeners = listeners.getOrPut(T::class) { Collections.newSetFromMap(WeakHashMap()) }
    }
    synchronized(eventListeners) {
      eventListeners.add(listener)
      eventsTracker?.onListenerRegistered(T::class, listener)
    }
    return listener
  }

  /**
   * React to the next event [dispatchEvent]-ed of a certain type.
   *
   * As the listener is only meant to listen to a single event it is not a requirement to have a strong reference to the [listener].
   */
  inline fun <reified T : Event> oneShotListener(listener: EventListener<T>) {
    var handled = false // Prevents the listener from being called multiple times
    val wrappedListener = EventListener<T> {
      if (handled) {
        return@EventListener
      }
      handled = true

      listener.handle(it)

      // Remove from another thread to not cause concurrent modification
      Main.inst().scheduler.executeAsync {
        @Suppress("UNCHECKED_CAST") // Should be same type in practice
        val storedThis = oneShotStrongRefs.remove(listener) as? EventListener<T>
        if (storedThis != null) {
          removeListener(storedThis)
        } else {
          Main.logger().error("Could not remove one shot listener $listener")
        }
      }
    }

    oneShotStrongRefs[listener] = wrappedListener
    registerListener(wrappedListener)
  }

  /**
   * Notify listeners of the given event
   *
   * @param event The event to notify about
   */
  inline fun <reified T : Event> dispatchEvent(event: T) {
    val backingListeners: MutableSet<EventListener<out Event>>
    synchronized(listeners) {
      backingListeners = listeners[T::class] ?: return
    }

    synchronized(backingListeners) {
      val correctListeners = backingListeners.filterIsInstance<EventListener<T>>()
      eventsTracker?.onEventDispatched(event)
      for (listener in correctListeners) {
        eventsTracker?.onEventListenedTo(event, listener)
        listener.handle(event)
      }
    }
  }

  /**
   * Prevent (by removing) the given listener from receiving any more events
   */
  inline fun <reified T : Event> removeListener(listener: EventListener<T>) {
    val eventListeners: MutableSet<EventListener<out Event>>
    synchronized(listeners) {
      eventListeners = listeners[T::class] ?: return
    }
    synchronized(eventListeners) {
      eventsTracker?.onListenerUnregistered(T::class, listener)
      eventListeners.remove(listener)
    }
  }

  /**
   * Remove all listeners registered
   */
  fun clear() {
    synchronized(listeners) {
      listeners.clear()
    }
  }
}
