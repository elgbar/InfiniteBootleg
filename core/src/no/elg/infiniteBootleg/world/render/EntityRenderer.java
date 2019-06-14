package no.elg.infiniteBootleg.world.render;

import no.elg.infiniteBootleg.world.subgrid.Entity;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.World.BLOCK_SIZE;

public class EntityRenderer implements Renderer {

    private WorldRender worldRender;

    public EntityRenderer(@NotNull WorldRender worldRender) {
        this.worldRender = worldRender;
    }


    @Override
    public void render() {
        for (Entity entity : worldRender.getWorld().getEntities()) {
//            System.out.println("Drawing entity at " + entity.getPosition().toString() + " velocity: " + entity.getVelocity());
            worldRender.getBatch()
                       .draw(entity.getTextureRegion(), entity.getPosition().x * BLOCK_SIZE, entity.getPosition().y * BLOCK_SIZE);
        }
    }
}
