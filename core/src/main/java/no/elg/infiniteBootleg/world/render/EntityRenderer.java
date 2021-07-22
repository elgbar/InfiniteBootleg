package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.graphics.g2d.Batch;
import no.elg.infiniteBootleg.Renderer;
import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import org.jetbrains.annotations.NotNull;

public class EntityRenderer implements Renderer {

    private final WorldRender worldRender;

    public EntityRenderer(@NotNull WorldRender worldRender) {
        this.worldRender = worldRender;
    }


    @Override
    public void render() {
        Batch batch = worldRender.getBatch();
        for (Entity entity : worldRender.getWorld().getEntities()) {
            if (entity.getTextureRegion() == null) { continue; }
            float x = (entity.getPosition().x - entity.getHalfBox2dWidth()) * BLOCK_SIZE;
            float y = (entity.getPosition().y - entity.getHalfBox2dHeight()) * BLOCK_SIZE;
            batch.draw(entity.getTextureRegion(), x, y, entity.getWidth(), entity.getHeight());
        }
    }
}
