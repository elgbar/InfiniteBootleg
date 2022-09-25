package no.elg.infiniteBootleg.events

import java.util.concurrent.ConcurrentHashMap

class EventManager {
  private val listeners: Map<Class<out Event>, Set<EventListener>> = ConcurrentHashMap()

  fun registerListener(listener: EventListener) {
    for (method in listener.javaClass.methods) {
      // TODO find all method that has a event as one and only parameter and is annotated with
      // EventHandler
    }
  }
}
