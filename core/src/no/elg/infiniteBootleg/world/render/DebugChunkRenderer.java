package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.screen.ScreenRenderer;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;

public class DebugChunkRenderer implements Renderer {

    private final WorldRender worldRender;
    private final ShapeRenderer lr;
    private final OrthographicCamera camera;
    private final SpriteBatch batch;

    public DebugChunkRenderer(WorldRender worldRender) {
        this.worldRender = worldRender;
        camera = worldRender.getCamera();
        lr = new ShapeRenderer(1000);
        batch = new SpriteBatch();
    }

    @Override
    public void render() {

        WorldRender.ChunkViewed chunksInView = worldRender.getChunksInView();

        int yEnd = chunksInView.vertical_end;
        int xEnd = chunksInView.horizontal_end;


        lr.begin(ShapeRenderer.ShapeType.Line);
        lr.setProjectionMatrix(camera.combined);

        float offset = Chunk.CHUNK_SIZE * Block.BLOCK_SIZE;
        for (float y = chunksInView.vertical_start; y < yEnd; y++) {
            for (float x = chunksInView.horizontal_start; x < xEnd; x++) {
                Color c;
                if (y == chunksInView.vertical_end - 1 || x == chunksInView.horizontal_start ||
                    x == chunksInView.horizontal_end - 1) {
                    c = Color.GOLDENROD;
                }
                else {
                    c = Color.FOREST;
                }
                lr.setColor(c);
                lr.rect(x * offset + 0.5f, y * offset + 0.5f, offset - 1, offset - 1);
            }
        }

        lr.end();
        ScreenRenderer sr = Main.inst().getScreenRenderer();
        sr.begin();
        sr.drawBottom("Debug Chunk outline legend", 5);
        sr.getFont().setColor(Color.FOREST);
        sr.drawBottom("  Green are chunks that you see", 3);
        sr.getFont().setColor(Color.GOLDENROD);
        sr.drawBottom("  Orange means only the physics are active", 1);
        sr.end();
        sr.resetFontColor();
    }
}
