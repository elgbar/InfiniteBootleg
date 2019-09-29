package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
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

    /**
     * How much information to show
     */
    public enum HUDModus {
        DEBUG,
        NORMAL,
        NONE
    }

    private SpriteBatch batch;
    private BitmapFont font;
    private HUDModus modus;

    public HUDRenderer() {
        modus = Main.debug ? HUDModus.DEBUG : HUDModus.NORMAL;
        batch = new SpriteBatch();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        System.out.println("ubuntu+" + Gdx.files.internal(Main.FONTS_FOLDER + "UbuntuMono-R.ttf").file()
                                                .getAbsolutePath());

        final FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal(Main.FONTS_FOLDER + "UbuntuMono-R.ttf"));
        final FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 20;
        parameter.minFilter = Texture.TextureFilter.Linear;
        font = generator.generateFont(parameter);
    }

    @Override
    public void render() {
        if (modus == HUDModus.NONE) {
            return;
        }
        Main main = Main.inst();
        World world = main.getWorld();
        int h = Gdx.graphics.getHeight();

        batch.begin();
        if (modus == HUDModus.DEBUG) {
            Block block = world.getRawBlock(main.getMouseBlockX(), main.getMouseBlockY());

            int[] vChunks = world.getRender().getChunksInView();

            int chunksInView = Math.abs(vChunks[WorldRender.HOR_END] - vChunks[WorldRender.HOR_START]) * Math.abs(
                vChunks[WorldRender.VERT_END] - vChunks[WorldRender.VERT_START]);

            Chunk pointChunk = world.getChunkFromWorld(main.getMouseBlockX(), main.getMouseBlockY());
            Entity player = world.getPlayers().iterator().next();

            String fps = String.format("FPS: %4d delta: %.5f", Gdx.graphics.getFramesPerSecond(),
                                       Gdx.graphics.getDeltaTime());
            String pointing = String.format("Pointing at %-5s (% 8.2f,% 8.2f) block (% 5d,% 5d) exists? %-5b",
                                            block != null ? block.getMaterial() : Material.AIR, //
                                            main.getMouseX(), main.getMouseY(), //
                                            main.getMouseBlockX(), main.getMouseBlockY(), //
                                            block != null);
            String chunk = String.format("chunk (% 4d,% 4d) : type: %-9.9s just air? %-5b can unload? %-5b)", //
                                         pointChunk.getChunkX(), pointChunk.getChunkY(), //
                                         world.getChunkLoader().getGenerator().getBiome(main.getMouseBlockX()), //
                                         pointChunk.isAllAir(), pointChunk.isAllowingUnloading() //
                                        );
            String viewChunk = String.format("Viewing %d chunks (%d blocks) with zoom: %.3f", chunksInView,
                                             chunksInView * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE,
                                             world.getRender().getCamera().zoom);

            String pos = String.format("p: (% 8.2f,% 8.2f) v: (% 8.2f,% 8.2f) g?%5b f?%5b", player.getPosition().x,
                                       player.getPosition().y, player.getVelocity().x, player.getVelocity().y, //
                                       player.isOnGround(), player.isFlying()//
                                      );

            String nl = "\n    ";
            StringBuilder ents = new StringBuilder("E = ");
            //noinspection LibGDXUnsafeIterator
            for (Entity entity : world.getEntities(Main.inst().getMouseX(), Main.inst().getMouseY())) {
                ents.append(entity.simpleName()).append(nl);
            }
            int index = ents.lastIndexOf(nl);
            if (index != -1) { ents.deleteCharAt(index); }


            font.draw(batch, fps, 10, h - 10);
            font.draw(batch, pointing, 10, h - 30);
            font.draw(batch, chunk, 10, h - 50);
            font.draw(batch, viewChunk, 10, h - 70);
            font.draw(batch, pos, 10, h - 90);
            font.draw(batch, ents.toString().trim(), 10, h - 110);
        }
        //noinspection ConstantConditions
        TextureRegion tr = world.getInput().getSelected().getTextureRegion();
        if (tr != null) {
            batch.draw(tr, Gdx.graphics.getWidth() - BLOCK_SIZE * 3, h - BLOCK_SIZE * 3, BLOCK_SIZE * 2,
                       BLOCK_SIZE * 2);
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

    public HUDModus getModus() {
        return modus;
    }

    public void setModus(HUDModus modus) {
        this.modus = modus;
    }
}
