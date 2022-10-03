package no.elg.infiniteBootleg.events.api

import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.concurrent.GuardedBy

object EventManager {

  /**
   * The inner set is in reality a [WeakHashMap]
   */
  @GuardedBy("itself")
  private val listeners: WeakHashMap<Class<out Event>, MutableSet<EventListener<out Event>>> = WeakHashMap()
  private val oneShotStrongRefs: MutableMap<EventListener<out Event>, EventListener<out Event>> = ConcurrentHashMap()

  var eventTracker: EventsTracker? = if (Settings.debug) EventsTracker(true) else null

  /**
   * React to events [dispatchEvent]-ed by someone else.
   *
   * Note that the listeners are stored as [WeakReference]s, which mean that they might be garbage-collected if they are not stored as (store) references somewhere.
   * This is to automatically un-register listeners which no longer can react to events.
   */
  inline fun <reified T : Event> registerListener(listener: EventListener<T>): EventListener<T> {
    return javaRegisterListener(T::class.java, listener)
  }

  /**
   * React to the next event [dispatchEvent]-ed of a certain type.
   *
   * As the listener is only meant to listen to a single event it is not a requirement to have a strong reference to the [listener].
   */
  inline fun <reified T : Event> oneShotListener(listener: EventListener<T>) {
    javaOneShotListener(T::class.java, listener)
  }

  /**
   * Notify listeners of the given event
   *
   * @param event The event to notify about
   */
  inline fun <reified T : Event> dispatchEvent(event: T) {
    javaDispatchEvent(event)
  }

  /**
   * Prevent (by removing) the given listener from reciving any more events
   */
  inline fun <reified T : Event> removeListener(listener: EventListener<T>) {
    javaRemoveListener(T::class.java, listener)
  }

  /**
   * React to events [dispatchEvent]-ed by someone else.
   *
   * Note that the listeners are stored as [WeakReference]s, which mean that they might be garbage-collected if they are not stored as (store) references somewhere.
   * This is to automatically un-register listeners which no longer can react to events.
   */
  @Deprecated("Only to be used by java code", replaceWith = ReplaceWith("registerListener(event)"))
  fun <T : Event> javaRegisterListener(eventClass: Class<T>, listener: EventListener<T>): EventListener<T> {
    val eventListeners: MutableSet<EventListener<out Event>>
    synchronized(listeners) {
      eventListeners = listeners.getOrPut(eventClass) { Collections.newSetFromMap(WeakHashMap()) }
    }
    synchronized(eventListeners) {
      eventListeners.add(listener)
      eventTracker?.onListenerRegistered(eventClass, listener)
    }
    return listener
  }

  /**
   * React to the next event [dispatchEvent]-ed of a certain type.
   *
   * As the listener is only meant to listen to a single event it is not a requirement to have a strong reference to the [listener].
   */
  @Deprecated("Only to be used by java code", replaceWith = ReplaceWith("oneShotListener(event)"))
  fun <T : Event> javaOneShotListener(eventClass: Class<T>, listener: EventListener<T>) {
    var handled = false // Prevents the listener from being called multiple times
    val removalListener = EventListener<T> {
      if (handled) {
        return@EventListener
      }
      handled = true

      listener.handle(it)

      // Remove from another thread to not cause
      Main.inst().scheduler.executeAsync {
        val storedThis = oneShotStrongRefs.remove(listener) ?: throw IllegalStateException("Failed to find a strong referance to the oneshot listener")
        javaRemoveListener(eventClass, storedThis as EventListener<T>) // Kotlin is stuped OFC(!!) this wokrs
      }
    }

    oneShotStrongRefs[listener] = removalListener
    javaRegisterListener(eventClass, removalListener)
  }

  /**
   * Notify listeners of the given event
   *
   * @param event The event to notify about
   */
  @Deprecated("Only to be used by java code", replaceWith = ReplaceWith("dispatchEvent(event)"))
  fun <T : Event> javaDispatchEvent(event: T) {
    val backingListeners: MutableSet<EventListener<out Event>>
    val correctListeners: List<EventListener<T>>
    synchronized(listeners) {
      backingListeners = listeners[event::class.java] ?: return
      correctListeners = backingListeners.filterIsInstance<EventListener<T>>()
    }

    synchronized(backingListeners) {
      eventTracker?.onEventDispatched(event)
      for (listener in correctListeners) {
        eventTracker?.onEventListenedTo(event, listener)
        listener.handle(event)
      }
    }
  }

  @Deprecated("Only to be used by java code", replaceWith = ReplaceWith("removeListener(event)"))
  fun <T : Event> javaRemoveListener(eventClass: Class<T>, listener: EventListener<T>) {
    val eventListeners: MutableSet<EventListener<out Event>>
    synchronized(listeners) {
      eventListeners = listeners[eventClass] ?: return
    }
    synchronized(eventListeners) {
      eventTracker?.onListenerUnregistered(eventClass, listener)
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
