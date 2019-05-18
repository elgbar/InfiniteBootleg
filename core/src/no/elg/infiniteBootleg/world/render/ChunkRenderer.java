package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Queue;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_WIDTH;
import static no.elg.infiniteBootleg.world.Material.AIR;

/**
 * @author Elg
 */
public class ChunkRenderer implements Renderer {

    private final SpriteBatch batch;
    private final Queue<Chunk> renderQueue;
    private final WorldRender worldRender;
    private final OrthographicCamera chunkCam;

    public ChunkRenderer(@NotNull WorldRender worldRender) {
        this.worldRender = worldRender;
        this.batch = new SpriteBatch();
        this.renderQueue = new Queue<>();
        chunkCam = new OrthographicCamera();
        chunkCam.setToOrtho(false, WorldRender.CHUNK_TEXT_WIDTH, WorldRender.CHUNK_TEXT_HEIGHT);
        batch.setProjectionMatrix(chunkCam.combined);
    }

    public void queueRendering(@NotNull Chunk chunk) {
        for (Chunk lchunk : renderQueue) {
            if (lchunk.equals(chunk)) {
                return;
            }
        }
        renderQueue.addLast(chunk);
    }

    @Override
    public void update() {
        //does nothing
    }

    @Override
    public void render() {
        System.out.println("There are " + renderQueue.size + " chunks waiting to be rendered");
        if (renderQueue.isEmpty()) { return; } //nothing to render
        Chunk chunk = renderQueue.removeFirst();

        if (!worldRender.inInView(chunk) || chunk.isAllAir()) {
            return;
        }

        FrameBuffer fbo =
            new FrameBuffer(Pixmap.Format.RGBA4444, WorldRender.CHUNK_TEXT_WIDTH, WorldRender.CHUNK_TEXT_HEIGHT, false);


        // this is the main render function
        Block[][] blocks = chunk.getBlocks();
//        batch.setProjectionMatrix(chunkCam.combined);
        fbo.begin();
        batch.begin();
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                Block block = blocks[x][y];
                if (block == null || block.getMaterial() == AIR) {
                    continue;
                }
                Location blkLoc = block.getLocation();
                int dx = blkLoc.x * World.BLOCK_SIZE;
                int dy = blkLoc.y * World.BLOCK_SIZE;
                //noinspection ConstantConditions
                batch.draw(block.getTexture(), dx, dy, World.BLOCK_SIZE, World.BLOCK_SIZE);
            }
        }
        batch.end();
        fbo.end();
        chunk.setFbo(fbo);
    }
}
