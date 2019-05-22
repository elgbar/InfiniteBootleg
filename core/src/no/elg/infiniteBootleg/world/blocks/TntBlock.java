package no.elg.infiniteBootleg.world.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.Updatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class TntBlock extends Block implements Updatable {

    private static final TextureRegion whiteTexture;

    static {
        Pixmap pixmap = new Pixmap(World.BLOCK_SIZE, World.BLOCK_SIZE, Pixmap.Format.RGBA4444);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whiteTexture = new TextureRegion(new Texture(pixmap));
    }

    private boolean white = false;

    public TntBlock(int x, int y, @Nullable World world, @NotNull Material material) {
        super(x, y, world, material);
    }

    @Override
    public void update() {
//        Main.getConsoleLogger().log("Tick @ " + getLocation());
        if (getWorld().getWorldTicker().getTickId() % 2 == 0) {
            white = !white;
        }
        if (Main.renderGraphic) {
            getChunk().updateTexture(true);
        }
    }

    @Override
    public @Nullable TextureRegion getTexture() {
        if (white) {
            return whiteTexture;
        }
        return super.getTexture();
    }
}
