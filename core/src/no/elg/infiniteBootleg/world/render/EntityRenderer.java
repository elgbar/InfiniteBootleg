package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.graphics.g2d.Batch;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

public class EntityRenderer implements Renderer {

    private WorldRender worldRender;

    public EntityRenderer(@NotNull WorldRender worldRender) {
        this.worldRender = worldRender;
    }


    @Override
    public void render() {
        Batch batch = worldRender.getBatch();
        for (Entity entity : worldRender.getWorld().getEntities()) {
            float x = entity.getPosition().x * BLOCK_SIZE - BLOCK_SIZE / 2f;
            float y = entity.getPosition().y * BLOCK_SIZE - BLOCK_SIZE / 2f;
            batch.draw(entity.getTextureRegion(), x, y);
        }
    }
}
