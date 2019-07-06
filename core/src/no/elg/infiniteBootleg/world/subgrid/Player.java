package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

public class Player extends LivingEntity {


    public static final float VERTICAL_IMPULSE = 1f;
    public static final float HORIZONTAL_IMPULSE = 1f;
    private final TextureRegion region;

    public Player(@NotNull World world) {
        super(world, world.getRender().getCamera().position.x / BLOCK_SIZE,
              world.getRender().getCamera().position.y / BLOCK_SIZE);
        region = new TextureRegion(Material.GRASS.getTextureRegion());
        region.flip(true, false);
    }

    @Override
    public TextureRegion getTextureRegion() {
        return region;
    }

    @Override
    public void update() {
        super.update();
        if (Main.inst().getConsole().isVisible()) {
            return;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.T)) {
            //teleport the player to the (last) location of the mouse
            getBody().setTransform(Main.inst().getMouseBlockX() + getBox2dWidth() / 2,
                                   Main.inst().getMouseBlockY() + getBox2dHeight() / 2, 0);
            getBody().setAngularVelocity(0);
            getBody().setLinearVelocity(0, 0);
            getBody().setAwake(true);
        }
        else {
            //TODO if shift is held, the impulse should be multiplied with 2
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                getBody().applyLinearImpulse(0, VERTICAL_IMPULSE, getPosition().x, getPosition().y, true);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                getBody().applyLinearImpulse(0, -VERTICAL_IMPULSE, getPosition().x, getPosition().y, true);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                getBody().applyLinearImpulse(-HORIZONTAL_IMPULSE, 0, getPosition().x, getPosition().y, true);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                getBody().applyLinearImpulse(HORIZONTAL_IMPULSE, 0, getPosition().x, getPosition().y, true);
            }
        }
    }

    @Override
    public float getWidth() {
        return BLOCK_SIZE;
    }

    @Override
    public float getHeight() {
        return BLOCK_SIZE;
    }
}
