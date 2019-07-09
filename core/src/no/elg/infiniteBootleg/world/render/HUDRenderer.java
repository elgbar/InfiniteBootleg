package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.Resizable;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.Entity;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

/**
 * @author Elg
 */
public class HUDRenderer implements Renderer, Disposable, Resizable {


    private SpriteBatch batch;
    private BitmapFont font;

    public HUDRenderer() {

        batch = new SpriteBatch();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        font = new BitmapFont(false);
    }

    @Override
    public void render() {
        World world = Main.inst().getWorld();
        Main main = Main.inst();
        Block block = world.getRawBlock(main.getMouseBlockX(), main.getMouseBlockY());

        int[] vChunks = world.getRender().getChunksInView();

        int chunksInView = Math.abs(vChunks[WorldRender.HOR_END] - vChunks[WorldRender.HOR_START]) *
                           Math.abs(vChunks[WorldRender.VERT_END] - vChunks[WorldRender.VERT_START]);
        int h = Gdx.graphics.getHeight();

        Chunk pointChunk = world.getChunkFromWorld(main.getMouseBlockX(), main.getMouseBlockY());
        String pointing = String.format("Pointing at %s (%d, %d) (exists? %b) in chunk (%d, %d) (type: %s just air? %b)",//
                                        block != null ? block.getMaterial() : Material.AIR, //
                                        main.getMouseBlockX(), main.getMouseBlockY(), //
                                        block != null, //
                                        pointChunk.getChunkX(), pointChunk.getChunkY(), //
                                        world.getChunkLoader().getGenerator().getBiome(main.getMouseBlockX()), //
                                        pointChunk.isAllAir());

        batch.begin();
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, h - 10);
        font.draw(batch, "Delta time: " + Gdx.graphics.getDeltaTime(), 10, h - 25);
        font.draw(batch, pointing, 10, h - 40);
        font.draw(batch,
                  "Viewing " + chunksInView + " chunks (" + chunksInView * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE + " blocks)", 10,
                  h - 55);
        font.draw(batch, "Zoom: " + world.getRender().getCamera().zoom, 10, h - 70);
        Entity player = world.getEntities().iterator().next(); //assume this is the player

        String pos = String.format("p: (%.2f,%.2f) v: (%.2f,%.2f)", player.getPosition().x, player.getPosition().y,
                                   player.getBody().getLinearVelocity().x, player.getBody().getLinearVelocity().y);
        font.draw(batch, "player " + pos, 10, h - 85);

        TextureRegion tr = world.getInput().getSelected().getTextureRegion();
        if (tr != null) {
            batch.draw(tr, Gdx.graphics.getWidth() - BLOCK_SIZE * 3, h - BLOCK_SIZE * 3, BLOCK_SIZE * 2, BLOCK_SIZE * 2);
        }
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }

    @Override
    public void resize(int width, int height) {
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));
    }
}
