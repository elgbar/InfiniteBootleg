package no.elg.infiniteBootleg.world.render;

import static java.lang.Math.round;
import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.api.Renderer;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.blocks.TntBlock;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import org.jetbrains.annotations.NotNull;

public record EntityRenderer(@NotNull ClientWorldRender worldRender) implements Renderer {

  @Override
  public void render() {
    Batch batch = worldRender.getBatch();
    ClientWorld world = worldRender.getWorld();
    WorldBody worldBody = world.getWorldBody();
    float worldOffsetX = worldBody.getWorldOffsetX();
    float worldOffsetY = worldBody.getWorldOffsetY();
    for (Entity entity : world.getEntities()) {
      TextureRegion textureRegion = entity.getTextureRegion();
      if (textureRegion == null) {
        continue;
      }
      Vector2 centerPos = entity.getPosition();
      float worldX = (centerPos.x - entity.getHalfBox2dWidth());
      float worldY = (centerPos.y - entity.getHalfBox2dHeight());
      float lightX = 0f;
      float lightY = 0f;
      if (Settings.renderLight) {
        int blockX = round(centerPos.x - entity.getHalfBox2dWidth() / 2);
        int blockY = round(centerPos.y);
        var topX = world.getTopBlockWorldY(blockX, BLOCKS_LIGHT_FLAG);
        if (blockY > topX) {
          lightX = CoordUtil.worldToScreen(blockX, worldOffsetX);
          lightY = CoordUtil.worldToScreen(topX + 1, worldOffsetY);
          batch.setColor(Color.WHITE);
        } else {
          var blockLight = world.getBlockLight(blockX, blockY, false);
          if (blockLight != null) {
            if (blockLight.isSkylight()) {
              batch.setColor(Color.WHITE);
            } else if (blockLight.isLit()) {
              float v = blockLight.getAverageBrightness();
              batch.setColor(v, v, v, 1);
            } else {
              batch.setColor(Color.BLACK);
            }
            lightX = CoordUtil.worldToScreen(blockX, worldOffsetX);
            lightY = CoordUtil.worldToScreen(blockY, worldOffsetY);
          }
        }
      }
      float screenX = CoordUtil.worldToScreen(worldX, worldOffsetX);
      float screenY = CoordUtil.worldToScreen(worldY, worldOffsetY);
      batch.draw(textureRegion, screenX, screenY, entity.getWidth(), entity.getHeight());
      batch.setColor(Color.WHITE);
      if (Settings.debugEntityLight) {
        batch.draw(TntBlock.Companion.getWhiteTexture(), lightX, lightY, BLOCK_SIZE, BLOCK_SIZE);
      }
    }
    batch.setColor(Color.WHITE);
  }
}
