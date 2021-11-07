package no.elg.infiniteBootleg.world.render;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Renderer;
import no.elg.infiniteBootleg.screen.ScreenRenderer;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import org.jetbrains.annotations.NotNull;

public class DebugChunkRenderer implements Renderer {

  public static final Color WITHIN_CAMERA_COLOR = Color.TEAL;
  public static final Color OUTSIDE_CAMERA_COLOR = Color.FIREBRICK;

  private final @NotNull ClientWorldRender worldRender;
  private final @NotNull ShapeRenderer lr;
  private final @NotNull OrthographicCamera camera;

  public DebugChunkRenderer(@NotNull ClientWorldRender worldRender) {
    this.worldRender = worldRender;
    camera = worldRender.getCamera();
    lr = new ShapeRenderer(1000);
  }

  @Override
  public void render() {

    ChunksInView chunksInView = worldRender.getChunksInView();

    int yEnd = chunksInView.verticalEnd;
    int xEnd = chunksInView.horizontalEnd;

    lr.begin(ShapeRenderer.ShapeType.Line);

    final WorldBody worldBody = worldRender.getWorld().getWorldBody();
    float worldOffsetX = worldBody.getWorldOffsetX() * BLOCK_SIZE;
    float worldOffsetY = worldBody.getWorldOffsetY() * BLOCK_SIZE;
    lr.setProjectionMatrix(camera.combined);

    int offset = Chunk.CHUNK_SIZE * Block.BLOCK_SIZE;
    for (float y = chunksInView.verticalStart; y < yEnd; y++) {
      for (float x = chunksInView.horizontalStart; x < xEnd; x++) {
        Color c;
        if (y == chunksInView.verticalEnd - 1
            || x == chunksInView.horizontalStart
            || x == chunksInView.horizontalEnd - 1) {
          c = OUTSIDE_CAMERA_COLOR;
        } else {
          c = WITHIN_CAMERA_COLOR;
        }
        lr.setColor(c);
        lr.rect(
            x * offset + 0.5f + worldOffsetX,
            y * offset + 0.5f + worldOffsetY,
            offset - 1f,
            offset - 1f);
      }
    }

    lr.end();
    ScreenRenderer sr = ClientMain.inst().getScreenRenderer();
    sr.begin();
    sr.drawBottom("Debug Chunk outline legend", 5);
    sr.getFont().setColor(WITHIN_CAMERA_COLOR);
    sr.drawBottom("  Chunks within the camera boarders", 3);
    sr.getFont().setColor(OUTSIDE_CAMERA_COLOR);
    sr.drawBottom("  Chunks outside camera boarders, only physics active", 1);
    sr.end();
    sr.resetFontColor();
  }
}
