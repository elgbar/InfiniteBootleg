package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

public class Player extends LivingEntity {

    public static final float SPEED = 5f;

    private final TextureRegion region;

    public Player(@NotNull World world) {
        super(world, world.getRender().getCamera().position.x / Block.BLOCK_SIZE,
              world.getRender().getCamera().position.y / Block.BLOCK_SIZE);
        setFlying(true);
        region = new TextureRegion(Material.BRICK.getTextureRegion());
        region.flip(true, false);
    }

    @Override
    public TextureRegion getTextureRegion() {
        return region;
    }

    @Override
    public void update() {
        super.update();
        Vector3 camPos = getWorld().getRender().getCamera().position;
        //find the vector between the current pos and the camera pos
        //to get the player to move the the correct direction negate the vector
        Vector2 dv = getPosition().cpy().sub(camPos.x / Block.BLOCK_SIZE, camPos.y / Block.BLOCK_SIZE).scl(-SPEED);
        getVelocity().set(dv);
    }
}
