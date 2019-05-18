package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.World;
import org.apache.commons.collections4.list.SetUniqueList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_WIDTH;
import static no.elg.infiniteBootleg.world.Material.AIR;

/**
 * @author Elg
 */
public class ChunkRenderer implements Renderer {

    private final SpriteBatch batch;
    private final SetUniqueList<Chunk> renderQueue;
    private final WorldRender worldRender;

    public ChunkRenderer(@NotNull WorldRender worldRender) {
        this.worldRender = worldRender;
        this.batch = new SpriteBatch();
        this.renderQueue = SetUniqueList.setUniqueList(new ArrayList<>());

        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, WorldRender.CHUNK_TEXT_WIDTH, WorldRender.CHUNK_TEXT_HEIGHT));
    }

    public void queueRendering(@NotNull Chunk chunk, boolean prioritize) {
        if (prioritize) {
            renderQueue.add(0, chunk);
        }
        else { renderQueue.add(chunk); }
    }

    @Override
    public void update() {
        //does nothing
        //TODO maybe clear all chunk not viewed in n seconds?
    }

    @Override
    public void render() {
        //get the first valid chunk to render
        Chunk chunk;
        do {
            if (renderQueue.isEmpty()) { return; } //nothing to render
            chunk = renderQueue.remove(0);
        } while (chunk.isAllAir() || !worldRender.inInView(chunk));

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
