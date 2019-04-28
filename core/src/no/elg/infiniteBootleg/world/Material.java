package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.world.blocks.Air;
import no.elg.infiniteBootleg.world.blocks.Stone;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Elg
 */
public enum Material {

    AIR(0, Air.class),
    STONE(1, Stone.class);


    private final int id;
    private final Class<? extends Block> impl;

    Material(int id, Class<? extends Block> impl) {

        this.id = id;
        this.impl = impl;
    }

    /**
     * @param x
     * @param y
     * @param world
     *
     * @return A block of this type
     */
    @Nullable
    public Block create(int x, int y, @Nullable World world) {
        try {
            Constructor<? extends Block> constructor = impl.getDeclaredConstructor(int.class, int.class, World.class);
            return constructor.newInstance(x, y, world);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
