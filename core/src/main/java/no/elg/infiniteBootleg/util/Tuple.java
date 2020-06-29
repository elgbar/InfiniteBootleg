package no.elg.infiniteBootleg.util;

import org.jetbrains.annotations.NotNull;

/**
 * A generic tuple
 *
 * @author Elg
 */
public class Tuple<K, V> {

    @NotNull
    public final K key;
    @NotNull
    public final V value;

    /**
     * Creates a new pair
     *
     * @param key
     *     The key for this pair
     * @param value
     *     The value to use for this pair
     */
    public Tuple(@NotNull K key, @NotNull V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Tuple<?, ?> that = (Tuple<?, ?>) o;
        return key.equals(that.key) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
