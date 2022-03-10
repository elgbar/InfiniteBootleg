package no.elg.infiniteBootleg.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public interface Metadata {

  @Nullable
  default <T> T get(@NotNull String key) {
    return get(key, null);
  }

  /**
   * @param key Key to the castle
   * @param defaultVal Value to return if no object with the given key exists
   * @param <T> The type of the value
   * @return The value stored at {@code key}
   * @throws IllegalStateException If the object at {@code key} cannot be cast to {@code T}
   */
  @Nullable
  <T> T get(@NotNull String key, @Nullable T defaultVal);

  <T> void set(@NotNull String key, T value);
}
