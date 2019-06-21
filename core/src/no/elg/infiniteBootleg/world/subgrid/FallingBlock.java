package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Direction;
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
        setDrag(0);
    }

    @Override
    public TextureRegion getTextureRegion() {
        return region;
    }

    @Nullable
    @Override
    public Direction collide(float dx, float dy) {
        Direction dir = super.collide(dx, dy);
        if (dir != null) {
            Gdx.app.postRunnable(() -> {
                int newX = (int) getPosition().x;
                int newY = (int) (getPosition().y + (Math.signum(getPosition().y) == -1 ? -1 : 0));

                if (getWorld().isAir(newX, newY)) {
                    getWorld().setBlock(newX, newY, material, true);
                    getWorld().getEntities().remove(this);
                }
                else {
                    getPosition().add(0, 1);
                }
            });
        }
        return dir;
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
