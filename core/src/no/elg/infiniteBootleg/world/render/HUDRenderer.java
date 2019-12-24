package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;

import static no.elg.infiniteBootleg.Main.SCALE;
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

    public static final int FONT_SIZE = 20;

    private final int spacing;

    public HUDRenderer() {
        modus = Main.debug ? HUDModus.DEBUG : HUDModus.NORMAL;
        batch = new SpriteBatch();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        final FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal(Main.FONTS_FOLDER + "UbuntuMono-R.ttf"));
        final FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = FONT_SIZE * SCALE;

        parameter.minFilter = Texture.TextureFilter.Linear;
        font = generator.generateFont(parameter);

        spacing = (FONT_SIZE * SCALE) / 2;
    }

    @Override
    public void render() {
        if (modus == HUDModus.NONE) {
            return;
        }
        Main main = Main.inst();
        World world = main.getWorld();
        int h = Gdx.graphics.getHeight();

        LivingEntity player = world.getLivingEntities().iterator().next();
        batch.begin();
        if (modus == HUDModus.DEBUG) {
            Block block = world.getBlock(main.getMouseBlockX(), main.getMouseBlockY(), true);

            int[] vChunks = world.getRender().getChunksInView();

            int chunksInView = Math.abs(vChunks[WorldRender.HOR_END] - vChunks[WorldRender.HOR_START]) * Math.abs(
                vChunks[WorldRender.VERT_END] - vChunks[WorldRender.VERT_START]);

            Chunk pointChunk = world.getChunkFromWorld(main.getMouseBlockX(), main.getMouseBlockY());

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
            String sky = String.format("time: %.2f scale: %.2f skycolor: %s", world.getTime(), world.getTimeScale(),
                                       world.getSkyBrightness());

            String nl = "\n    ";
            StringBuilder ents = new StringBuilder("E = ");
            //noinspection LibGDXUnsafeIterator
            for (Entity entity : world.getEntities(Main.inst().getMouseX(), Main.inst().getMouseY())) {
                ents.append(entity.simpleName()).append(nl);
            }
            int index = ents.lastIndexOf(nl);
            if (index != -1) { ents.deleteCharAt(index); }


            font.draw(batch, fps, spacing, h - spacing);
            font.draw(batch, pointing, spacing, h - spacing * 3);
            font.draw(batch, chunk, spacing, h - spacing * 5);
            font.draw(batch, viewChunk, spacing, h - spacing * 7);
            font.draw(batch, pos, spacing, h - spacing * 9);
            font.draw(batch, sky, spacing, h - spacing * 11);
            font.draw(batch, ents.toString().trim(), spacing, h - spacing * 13);
        }
        Material sel = player.getControls().getSelected();
        if (sel != null && sel.getTextureRegion() != null) {
            batch.draw(sel.getTextureRegion(), Gdx.graphics.getWidth() - BLOCK_SIZE * 3 * SCALE,
                       h - BLOCK_SIZE * 3 * SCALE, BLOCK_SIZE * 2 * SCALE, BLOCK_SIZE * 2 * SCALE);
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
