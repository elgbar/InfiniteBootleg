package no.elg.infiniteBootleg.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;
import no.elg.infiniteBootleg.Main;
import static no.elg.infiniteBootleg.Main.SCALE;
import no.elg.infiniteBootleg.Renderer;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.world.Block;
import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;

/**
 * @author Elg
 */
public class HUDRenderer implements Renderer {

    private HUDModus modus;

    public HUDRenderer() {
        modus = Settings.debug ? HUDModus.DEBUG : HUDModus.NORMAL;
    }

    @Override
    public void render() {
        if (modus == HUDModus.NONE) {
            return;
        }
        Main main = Main.inst();
        World world = main.getWorld();
        int h = Gdx.graphics.getHeight();

        LivingEntity player = Main.inst().getPlayer();
        ScreenRenderer sr = Main.inst().getScreenRenderer();
        if (modus == HUDModus.DEBUG) {
            Block block = world.getBlock(main.getMouseBlockX(), main.getMouseBlockY(), true);

            WorldRender.ChunkViewed vChunks = world.getRender().getChunksInView();

            int chunksHorz = vChunks.getHorizontalLength();
            int chunksVert = vChunks.getVerticalLength();
            int chunksInView = chunksHorz * chunksVert;

            Chunk pc = world.getChunkFromWorld(main.getMouseBlockX(), main.getMouseBlockY());

            String fps = String.format("FPS: %4d delta: %.5f tps: %2d tps delta: %3d ms active threads %d", Gdx.graphics.getFramesPerSecond(),
                                       Gdx.graphics.getDeltaTime(), world.getWorldTicker().getRealTPS(),
                                       TimeUtils.nanosToMillis(world.getWorldTicker().getTpsDelta()), Main.inst().getScheduler().getActiveThreads());
            String pointing = String.format("Pointing at %-5s (% 8.2f,% 8.2f) block (% 5d,% 5d) exists? %-5b",
                                            block != null ? block.getMaterial() : Material.AIR, //
                                            main.getMouseX(), main.getMouseY(), //
                                            main.getMouseBlockX(), main.getMouseBlockY(), //
                                            block != null);
            String chunk = pc == null ? "chunk (???,???) : type: ??? just air? ??? can unload? ???)" : String.format(
                "chunk (% 4d,% 4d) : type: %-9.9s just air? %-5b can unload? %-5b)", //
                pc.getChunkX(), pc.getChunkY(), //
                world.getChunkLoader().getGenerator().getBiome(main.getMouseBlockX()), //
                pc.isAllAir(), pc.isAllowingUnloading() //
                                                                                                                    );
            String viewChunk = String.format("Viewing %d chunks (total %d blocks, w %d b, h %d b) with zoom: %.3f", chunksInView,
                                             chunksInView * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE, chunksHorz * Chunk.CHUNK_SIZE, chunksVert * Chunk.CHUNK_SIZE,
                                             world.getRender().getCamera().zoom);


            String pos = player == null ? "No player" : String.format("p: (% 8.2f,% 8.2f) v: (% 8.2f,% 8.2f) g?%5b f?%5b", player.getPosition().x,
                                                                      player.getPosition().y, player.getVelocity().x, player.getVelocity().y, //
                                                                      player.isOnGround(), player.isFlying()//
                                                                     );
            String sky = String.format("time: %.2f scale: %.2f skycolor: %s", world.getWorldTime().getTime(), world.getWorldTime().getTimeScale(),
                                       world.getWorldTime().getSkyBrightness());

            String nl = "\n    ";
            StringBuilder ents = new StringBuilder("E = ");

            for (Entity entity : world.getEntities(Main.inst().getMouseX(), Main.inst().getMouseY())) {
                ents.append(entity.simpleName()).append(nl);
            }
            int index = ents.lastIndexOf(nl);
            if (index != -1) {
                ents.deleteCharAt(index);
            }

            sr.begin();
            sr.drawTop(fps, 1);
            sr.drawTop(pointing, 3);
            sr.drawTop(chunk, 5);
            sr.drawTop(viewChunk, 7);
            sr.drawTop(pos, 9);
            sr.drawTop(sky, 11);
            sr.drawTop(ents.toString().trim(), 13);
        }
        else {
            sr.begin();
        }
        if (player != null) {
            Material mat = player.getControls().getSelected();
            if (mat != null && mat.getTextureRegion() != null) {
                sr.getBatch().draw(mat.getTextureRegion(), Gdx.graphics.getWidth() - BLOCK_SIZE * 3 * SCALE, h - BLOCK_SIZE * 3 * SCALE, BLOCK_SIZE * 2 * SCALE,
                                   BLOCK_SIZE * 2 * SCALE);
            }
        }
        sr.end();
    }

    public HUDModus getModus() {
        return modus;
    }

    public void setModus(HUDModus modus) {
        this.modus = modus;
    }

    /**
     * How much information to show
     */
    public enum HUDModus {
        DEBUG,
        NORMAL,
        NONE
    }
}
