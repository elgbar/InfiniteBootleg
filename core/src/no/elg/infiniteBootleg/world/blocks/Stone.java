package no.elg.infiniteBootleg.world.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class Stone extends Block {

    private static final Texture texture;

    static {
        if (Main.HEADLESS) {
            texture = null;
        }
        else {
            Pixmap pix = new Pixmap(World.BLOCK_SIZE, World.BLOCK_SIZE, Pixmap.Format.RGBA8888);
            pix.setColor(Color.GRAY);
            pix.fill();
            texture = new Texture(pix);
        }
    }

    public Stone(int x, int y, @Nullable World world) {
        super(x, y, world);
    }

    @NotNull
    @Override
    public Texture getTexture() {
        return texture;
    }

    @Override
    public @NotNull Material getMaterial() {
        return Material.STONE;
    }
}
