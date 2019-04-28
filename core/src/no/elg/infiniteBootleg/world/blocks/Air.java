package no.elg.infiniteBootleg.world.blocks;

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
public class Air extends Block {

    private static final Texture texture;

    static {
        if (Main.HEADLESS) {
            texture = null;
        }
        else {
            Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pix.setColor(0, 0, 0, 0);
            pix.fill();
            texture = new Texture(pix);
        }
    }

    public Air(int x, int y, @Nullable World world) {
        super(x, y, world);
    }

    @NotNull
    @Override
    public Texture getTexture() {
        return texture;
    }

    @Override
    public @NotNull Material getMaterial() {
        return Material.AIR;
    }
}
