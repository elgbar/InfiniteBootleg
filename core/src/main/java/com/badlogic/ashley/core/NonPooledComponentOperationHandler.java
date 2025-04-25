package com.badlogic.ashley.core;

import kotlin.Unit;
import no.elg.infiniteBootleg.core.world.ticker.PostRunnableHandler;

public class NonPooledComponentOperationHandler extends ComponentOperationHandler {
  private final PostRunnableHandler postRunnableHandler = new PostRunnableHandler();

  public NonPooledComponentOperationHandler(Object delayed) {
    super((BooleanInformer) delayed);
  }

  @Override
  public void add(Entity entity) {
    postRunnableHandler.postRunnable(
        () -> {
          entity.notifyComponentAdded();
          return Unit.INSTANCE;
        });
  }

  @Override
  public void remove(Entity entity) {
    postRunnableHandler.postRunnable(
        () -> {
          entity.notifyComponentRemoved();
          return Unit.INSTANCE;
        });
  }

  @Override
  public boolean hasOperationsToProcess() {
    return postRunnableHandler.hasRunnables();
  }

  @Override
  public void processOperations() {
    postRunnableHandler.executeRunnables();
  }
}
