package no.elg.infiniteBootleg.world;

import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public interface Metadata {

    <T> T get(@NotNull String key, T defaultVal);

    <T> void set(@NotNull String key, T value);
}
