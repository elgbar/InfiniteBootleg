package no.elg.infiniteBootleg.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thread safe configuration
 *
 * @author Elg
 */
public class Config implements Metadata {

    private final Map<String, Object> config = new ConcurrentHashMap<>();

    @Nullable
    @Override
    public <T> T get(@NotNull String key, @Nullable T defaultVal) {
        Object obj = config.getOrDefault(key, defaultVal);
        try {
            //noinspection unchecked we are literaly in a try catch block
            return (T) obj;
        } catch (ClassCastException e) {
            throw new IllegalStateException("Failed to cast object to type T", e);
        }
    }

    @Override
    public <T> void set(@NotNull String key, T value) {
        config.put(key, value);
    }
}
