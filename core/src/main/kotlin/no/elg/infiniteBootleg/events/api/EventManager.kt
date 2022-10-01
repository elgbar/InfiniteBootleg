package no.elg.infiniteBootleg.events.api

import java.lang.ref.WeakReference
import java.util.WeakHashMap
import javax.annotation.concurrent.GuardedBy

object EventManager {

  @GuardedBy("itself")
  private val listeners: WeakHashMap<Class<out Event>, MutableList<WeakReference<EventListener<out Event>>>> = WeakHashMap()

  inline fun <reified T : Event> registerListener(listener: EventListener<T>) {
    javaRegisterListener(T::class.java, listener)
  }

  @Deprecated("Only to be used by java code", replaceWith = ReplaceWith("register(event)"))
  fun <T : Event> javaRegisterListener(eventClass: Class<T>, listener: EventListener<T>) {
    val eventListeners: MutableList<WeakReference<EventListener<out Event>>>
    synchronized(listeners) {
      eventListeners = listeners.getOrPut(eventClass) { ArrayList() }
    }
    synchronized(eventListeners) {
      eventListeners.add(WeakReference(listener))
    }
  }

  inline fun <reified T : Event> dispatchEvent(event: T) {
    javaDispatchEvent(T::class.java, event)
  }

  @Deprecated("Only to be used by java code", replaceWith = ReplaceWith("dispatchEvent(event)"))
  fun <T : Event> javaDispatchEvent(eventClass: Class<T>, event: T) {
    require(eventClass == event::class.java) { "Wrong event class" }
//    Main.logger().debug("Events", "Dispatched event ${eventClass::class.simpleName} $event")
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
//          Main.logger().log("Listener have been garbage collected!")
          backingListeners.remove(listenerRef as WeakReference<*>)
          continue
        }
        listener.handle(event)
      }
    }
  }
}
