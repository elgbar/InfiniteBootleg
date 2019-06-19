package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

public class Player extends LivingEntity {

    private final TextureRegion region;

    public Player(@NotNull World world, int x, int y) {
        super(world, x, y);
        setFlying(true);
        region = new TextureRegion(Material.BRICK.getTextureRegion());
        region.flip(true, false);
        System.out.println("player created");
    }

    @Override
    public TextureRegion getTextureRegion() {
        return region;
    }

    @Override
    public void update() {
        super.update();
        Vector3 camPos = getWorld().getRender().getCamera().position;
        getPosition().set(camPos.x / World.BLOCK_SIZE, camPos.y / World.BLOCK_SIZE);
    }
}
