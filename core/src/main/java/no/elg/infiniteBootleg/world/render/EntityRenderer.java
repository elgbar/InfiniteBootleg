package no.elg.infiniteBootleg.world.render;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import no.elg.infiniteBootleg.Renderer;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import org.jetbrains.annotations.NotNull;

public record EntityRenderer(@NotNull ClientWorldRender worldRender) implements Renderer {

  @Override
  public void render() {
    Batch batch = worldRender.getBatch();
    final WorldBody worldBody = worldRender.getWorld().getWorldBody();
    float worldOffsetX = worldBody.getWorldOffsetX() * BLOCK_SIZE;
    float worldOffsetY = worldBody.getWorldOffsetY() * BLOCK_SIZE;
    for (Entity entity : worldRender.getWorld().getEntities()) {
      TextureRegion textureRegion = entity.getTextureRegion();
      if (textureRegion == null) {
        continue;
      }
      float worldX = (entity.getPosition().x - entity.getHalfBox2dWidth());
      float worldY = (entity.getPosition().y - entity.getHalfBox2dHeight());
      float x = worldX * BLOCK_SIZE + worldOffsetX;
      float y = worldY * BLOCK_SIZE + worldOffsetY;
      if (Settings.renderLight) {
        var currentBlock =
            worldRender.getWorld().getRawBlock((int) Math.floor(worldX), (int) Math.floor(worldY));
        if (currentBlock != null) {
          var blockLight = currentBlock.getBlockLight();
          if (blockLight.isSkylight()) {
            batch.setColor(Color.WHITE);
          } else if (blockLight.isLit()) {
            float v = blockLight.getAverageBrightness();
            batch.setColor(v, v, v, 1);
          } else {
            batch.setColor(Color.BLACK);
          }
        }
      }
      batch.draw(textureRegion, x, y, entity.getWidth(), entity.getHeight());
    }
    batch.setColor(Color.WHITE);
  }
}
