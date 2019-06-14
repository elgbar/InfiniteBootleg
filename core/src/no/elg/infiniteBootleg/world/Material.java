package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.blocks.SandBlock;
import no.elg.infiniteBootleg.world.blocks.TntBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Elg
 */
public enum Material {

    AIR(null, false, false, false, 0),
    STONE(1.5f),
    BRICK(2f),
    DIRT(1f),
    GRASS(0.8f),
    TNT(TntBlock.class, 0.5f),
    SAND(SandBlock.class, 1f),
    ;

    private final Class<? extends Block> impl;
    private final boolean solid;
    private final boolean blocksLight;
    private final boolean placable;
    private final float hardness;
    private final TextureRegion texture;

    Material(float hardness) {this(null, hardness);}

    Material(Class<? extends Block> impl, float hardness) {this(impl, true, true, true, hardness);}

    Material(Class<? extends Block> impl, boolean solid, boolean blocksLight, boolean placable, float hardness) {
        this.impl = impl;
        this.solid = solid;
        this.blocksLight = blocksLight;
        this.placable = placable;
        this.hardness = hardness;
        if (Main.renderGraphic && !"AIR".equals(name())) {
            texture = Main.inst().getTextureAtlas().findRegion(name().toLowerCase());
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
    public TextureRegion getTextureRegion() {
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

    public float getHardness() {
        return hardness;
    }

    public static Material fromByte(byte b) {
        return values()[b];
    }
}
