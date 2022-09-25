package no.elg.infiniteBootleg.events.api

import no.elg.infiniteBootleg.Main
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import javax.annotation.concurrent.GuardedBy

object EventManager {

  @GuardedBy("itself")
  private val listeners: WeakHashMap<Class<out Event>, MutableList<WeakReference<EventListener<out Event>>>> = WeakHashMap()

  inline fun <reified T : Event> register(listener: EventListener<T>) {
    javaRegister(listener, T::class.java)
  }

  @Deprecated("Only to be used by java code", replaceWith = ReplaceWith("fireEvent(event)"))
  fun <T : Event> javaRegister(listener: EventListener<T>, eventClass: Class<T>) {
    val eventListeners: MutableList<WeakReference<EventListener<out Event>>>
    synchronized(listeners) {
      eventListeners = listeners.getOrPut(eventClass) { ArrayList() }
    }
    synchronized(eventListeners) {
      eventListeners.add(WeakReference(listener))
    }
  }

  inline fun <reified T : Event> fireEvent(event: T) {
    javaFireEvent(event, T::class.java)
  }

  @Deprecated("Only to be used by java code", replaceWith = ReplaceWith("fireEvent(event)"))
  fun <T : Event> javaFireEvent(event: T, eventClass: Class<T>) {
    val backingListeners: MutableList<WeakReference<EventListener<out Event>>>
    val correctListeners: List<WeakReference<EventListener<T>>>
    synchronized(listeners) {
      backingListeners = listeners[event::class.java] ?: return
      correctListeners = backingListeners.filterIsInstance<WeakReference<EventListener<T>>>()
    }

    synchronized(backingListeners) {
      for (listenerRef in correctListeners) {
        val listener = listenerRef.get()
        if (listener == null) {
          Main.logger().log("Listener have been garbage collected!")
          backingListeners.remove(listenerRef as WeakReference<*>)
          continue
        }
        listener.handle(event)
      }
    }
  }
}
