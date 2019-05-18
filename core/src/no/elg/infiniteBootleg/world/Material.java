package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.blocks.GeneralBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Elg
 */
public enum Material {

    AIR(0, null, false, false, false),
    STONE(1, null);


    private final int id;
    private final Class<? extends Block> impl;
    private final boolean solid;
    private final boolean blocksLight;
    private final boolean placable;
    private final TextureRegion texture;

    Material(int id, Class<? extends Block> impl) {this(id, impl, true, true, true);}

    Material(int id, Class<? extends Block> impl, boolean solid, boolean blocksLight, boolean placable) {
        this.id = id;
        this.impl = impl;
        this.solid = solid;
        this.blocksLight = blocksLight;
        this.placable = placable;
        if (Main.renderGraphic) {
            this.texture = Main.getTextureAtlas().findRegion(name().toLowerCase());
        }
        else {
            texture = null;
        }
    }

    /**
     * @param x
     * @param y
     * @param world
     *
     * @return A block of this type
     */
    @NotNull
    public Block create(int x, int y, @Nullable World world) {
        if (impl == null) {
            return new GeneralBlock(x, y, world, this);
        }
        try {
            Constructor<? extends Block> constructor = impl.getDeclaredConstructor(int.class, int.class, World.class);
            return constructor.newInstance(x, y, world);

        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nullable
    public TextureRegion getTexture() {
        return texture;
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
