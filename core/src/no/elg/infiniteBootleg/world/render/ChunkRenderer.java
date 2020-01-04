package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Renderer;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import org.apache.commons.collections4.list.SetUniqueList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_TEXTURE_SIZE;
import static no.elg.infiniteBootleg.world.Material.AIR;

/**
 * @author Elg
 */
public class ChunkRenderer implements Renderer, Disposable {

    private final SpriteBatch batch;
    private final SetUniqueList<Chunk> renderQueue;
    private final WorldRender worldRender;

    //current rendering chunk
    private Chunk curr = null;

    public ChunkRenderer(@NotNull WorldRender worldRender) {
        this.worldRender = worldRender;
        batch = new SpriteBatch();
        //use linked list for fast adding to end and beginning
        List<Chunk> chunkList = Collections.synchronizedList(new LinkedList<>());
        renderQueue = SetUniqueList.setUniqueList(chunkList);
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, CHUNK_TEXTURE_SIZE, Chunk.CHUNK_TEXTURE_SIZE));
    }

    public void queueRendering(@NotNull Chunk chunk, boolean prioritize) {
        synchronized (renderQueue) {
            if (chunk != curr && !renderQueue.contains(chunk)) {
                Main.inst().getScheduler().executeAsync(() -> chunk.getChunkBody().update(true, false));
                if (prioritize) { renderQueue.add(0, chunk); }
                else { renderQueue.add(chunk); }
            }
        }
    }

    @Override
    public void render() {
        //get the first valid chunk to render
        Chunk chunk;
        synchronized (renderQueue) {
            do {
                if (renderQueue.isEmpty()) { return; } //nothing to render
                chunk = renderQueue.remove(0);
            } while (chunk.isAllAir() || worldRender.isOutOfView(chunk) || !chunk.isLoaded());
            curr = chunk;
        }
        final Chunk finalChunk = chunk;

        FrameBuffer fbo = chunk.getFbo();

        // this is the main render function
        Block[][] blocks = chunk.getBlocks();
        fbo.begin();
        batch.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (finalChunk) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < CHUNK_SIZE; y++) {
                    Block block = blocks[x][y];
                    if (block == null || block.getMaterial() == AIR) {
                        continue;
                    }
                    int dx = block.getLocalX() * BLOCK_SIZE;
                    int dy = block.getLocalY() * BLOCK_SIZE;

                    //noinspection ConstantConditions
                    batch.draw(block.getTexture(), dx, dy, BLOCK_SIZE, BLOCK_SIZE);
                }
            }
        }
        batch.end();
        fbo.end();

        worldRender.update();

        synchronized (renderQueue) {
            curr = null;
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
