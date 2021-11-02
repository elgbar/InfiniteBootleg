package no.elg.infiniteBootleg.events;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager {

  private final Map<Class<? extends Event>, Set<EventListener>> listeners;

  public EventManager() {
    listeners = new ConcurrentHashMap<>();
  }

  public void registerListener(EventListener listener) {
    for (Method method : listener.getClass().getMethods()) {
      // TODO find all method that has a event as one and only parameter and is annotated with
      // EventHandler
    }
  }
}
