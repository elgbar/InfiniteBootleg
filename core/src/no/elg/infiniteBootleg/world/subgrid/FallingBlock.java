package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FallingBlock extends Entity {

    private final Material material;
    private final TextureRegion region;

    public FallingBlock(@NotNull World world, float x, float y, @NotNull Material material) {
        super(world, x, y);
        this.material = material;
        region = new TextureRegion(material.getTextureRegion());
        region.flip(true, false);
    }

    @Override
    public TextureRegion getTextureRegion() {
        return region;
    }

    @Nullable
    @Override
    public Location collide(float dx, float dy) {
        Location loc = super.collide(dx, dy);
        if (loc != null) {
            Gdx.app.postRunnable(() -> {
                getWorld().setBlock(loc.x, loc.y, material, true);
                getWorld().getEntities().remove(this);
            });
        }
        return loc;
    }

    @Override
    public float getWidth() {
        return Block.BLOCK_SIZE - 1;
    }

    @Override
    public float getHeight() {
        return Block.BLOCK_SIZE - 1;
    }
}
