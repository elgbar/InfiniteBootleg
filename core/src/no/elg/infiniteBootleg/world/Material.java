package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.blocks.TntBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Elg
 */
public enum Material {

    AIR(null, false, false, false),
    STONE(),
    BRICK(),
    DIRT(),
    GRASS(),
    TNT(TntBlock.class),
    ;

    private final Class<? extends Block> impl;
    private final boolean solid;
    private final boolean blocksLight;
    private final boolean placable;
    private final TextureRegion texture;

    Material() {this(null);}

    Material(Class<? extends Block> impl) {this(impl, true, true, true);}

    Material(Class<? extends Block> impl, boolean solid, boolean blocksLight, boolean placable) {
        this.impl = impl;
        this.solid = solid;
        this.blocksLight = blocksLight;
        this.placable = placable;
        if (Main.renderGraphic && !"AIR".equals(name())) {
            this.texture = Main.inst().getTextureAtlas().findRegion(name().toLowerCase());
            texture.flip(false, false);
        }
        else { texture = null; }
    }

    /**
     * @param world
     *     World this block this exists in
     * @param chunk
     * @param localX
     *     Relative x in the chunk
     * @param localY
     *     Relative y in the chunk
     *
     * @return A block of this type
     */
    @NotNull
    public Block create(@NotNull World world, @NotNull Chunk chunk, int localX, int localY) {


        if (impl == null) {
            return new Block(world, chunk, localX, localY, this);
        }
        try {
            Constructor<? extends Block> constructor =
                impl.getDeclaredConstructor(World.class, Chunk.class, int.class, int.class, Material.class);
            return constructor.newInstance(world, chunk, localX, localY, this);

        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nullable
    public TextureRegion getTexture() {
        return texture;
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
