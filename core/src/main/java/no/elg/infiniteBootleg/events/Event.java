package no.elg.infiniteBootleg.events;

public abstract class Event {

  private final boolean async;

  public Event() {
    this(false);
  }

  public Event(boolean async) {
    this.async = async;
  }

  public boolean isAsync() {
    return async;
  }
}
