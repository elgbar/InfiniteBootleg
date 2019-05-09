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

    AIR(0, Air.class, false, false, false),
    STONE(1, Stone.class);


    private final int id;
    private final Class<? extends Block> impl;
    private final boolean solid;
    private final boolean blocksLight;
    private final boolean placable;

    Material(int id, Class<? extends Block> impl) {this(id, impl, true, true, true);}

    Material(int id, Class<? extends Block> impl, boolean solid, boolean blocksLight, boolean placable) {
        this.id = id;
        this.impl = impl;
        this.solid = solid;
        this.blocksLight = blocksLight;
        this.placable = placable;
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

    public int getId() {
        return id;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean blocksLight() {
        return blocksLight;
    }

    public boolean isPlacable() {
        return placable;
    }
}
