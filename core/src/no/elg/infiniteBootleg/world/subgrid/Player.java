package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

public class Player extends LivingEntity {

    private final TextureRegion region;

    public Player(@NotNull World world) {
        super(world, world.getRender().getCamera().position.x / BLOCK_SIZE,
              world.getRender().getCamera().position.y / BLOCK_SIZE);
        setFlying(true);
        region = new TextureRegion(Material.GRASS.getTextureRegion());
        region.flip(true, false);
    }

    @Override
    public TextureRegion getTextureRegion() {
        return region;
    }

    @Override
    public void update() {
        if (Gdx.input.isKeyPressed(Input.Keys.T)) {
            //teleport the player to the (last) location of the mouse
            Vector3 unproject = getWorld().getRender().getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            int blockX = (int) Math.floor(unproject.x / BLOCK_SIZE);
            int blockY = (int) Math.floor(unproject.y / BLOCK_SIZE);
            getPosition().set(blockX, blockY);
            getVelocity().setZero();
        }
        else {
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                getVelocity().add(0, getAcceleration());
            }
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                getVelocity().add(0, -getAcceleration());
            }
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                getVelocity().add(-getAcceleration(), 0);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                getVelocity().add(getAcceleration(), 0);
            }
        }
        super.update();
    }

    @Override
    public float getWidth() {
        return BLOCK_SIZE - 0.1f;
    }

    @Override
    public float getHeight() {
        return BLOCK_SIZE - 0.1f;
    }
}
