package no.elg.infiniteBootleg.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import no.elg.infiniteBootleg.Main;
import static no.elg.infiniteBootleg.Main.SCALE;
import no.elg.infiniteBootleg.Renderer;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Block;
import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import no.elg.infiniteBootleg.world.Chunk;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import no.elg.infiniteBootleg.world.time.WorldTime;

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
            int mouseBlockX = main.getMouseBlockX();
            int mouseBlockY = main.getMouseBlockY();

            sr.begin();
            sr.drawTop(fpsString(world), 1);
            sr.drawTop(pointing(mouseBlockX, mouseBlockY, world), 3);
            sr.drawTop(chunk(mouseBlockX, mouseBlockY, world), 5);
            sr.drawTop(viewChunk(world), 7);
            sr.drawTop(pos(player), 9);
            sr.drawTop(time(world), 11);
            sr.drawTop(ents(world), 13);
        }
        else {
            sr.begin();
        }
        if (player != null) {
            Material mat = player.getControls().getSelected();
            if (mat.getTextureRegion() != null) {
                sr.getBatch().draw(mat.getTextureRegion(), Gdx.graphics.getWidth() - BLOCK_SIZE * 3f * SCALE, h - BLOCK_SIZE * 3f * SCALE,
                                   BLOCK_SIZE * 2f * SCALE, BLOCK_SIZE * 2f * SCALE);
            }
        }
        sr.end();
    }

    private String ents(World world) {
        String nl = "\n    ";
        StringBuilder ents = new StringBuilder("E = ");

        for (Entity entity : world.getEntities(Main.inst().getMouseX(), Main.inst().getMouseY())) {
            ents.append(entity.simpleName()).append(nl);
        }
        int index = ents.lastIndexOf(nl);
        if (index != -1) {
            ents.deleteCharAt(index);
        }
        return ents.toString().trim();
    }

    private String fpsString(World world) {
        int activeThreads = Main.inst().getScheduler().getActiveThreads();
        long tpsDelta = TimeUtils.nanosToMillis(world.getWorldTicker().getTpsDelta());
        long realTPS = world.getWorldTicker().getRealTPS();
        float fpsDelta = Gdx.graphics.getDeltaTime();
        int tps = Gdx.graphics.getFramesPerSecond();

        String format = "FPS: %4d delta: %.5f tps: %2d tps delta: %3d ms active threads %d";
        return String.format(format, tps, fpsDelta, realTPS, tpsDelta, activeThreads);
    }

    private String pointing(int mouseBlockX, int mouseBlockY, World world) {

        Block block = world.getBlock(mouseBlockX, mouseBlockY, true);
        Material material = block != null ? block.getMaterial() : Material.AIR;
        float rawX = Main.inst().getMouseX();
        float rawY = Main.inst().getMouseY();
        boolean exists = block != null;

        String format = "Pointing at %-5s (% 8.2f,% 8.2f) block (% 5d,% 5d) exists? %-5b";
        return String.format(format, material, rawX, rawY, mouseBlockX, mouseBlockY, exists);
    }

    private String chunk(int mouseBlockX, int mouseBlockY, World world) {


        Chunk pc = world.getChunkFromWorld(mouseBlockX, mouseBlockY);
        int chunkY = CoordUtil.worldToChunk(mouseBlockY);
        int chunkX = CoordUtil.worldToChunk(mouseBlockX);
        if (pc == null) {
            String format = "chunk (% 4d,% 4d) : not loaded";
            return String.format(format, chunkX, chunkY);
        }
        else {
            Biome biome = world.getChunkLoader().getGenerator().getBiome(mouseBlockX);
            boolean allAir = pc.isAllAir();
            boolean allowUnloading = pc.isAllowingUnloading();

            String format = "chunk (% 4d,% 4d) : type: %-9.9s just air? %-5b can unload? %-5b";
            return String.format(format, chunkX, chunkY, biome, allAir, allowUnloading);
        }
    }

    private String time(World world) {
        WorldTime worldTime = world.getWorldTime();
        String format = "time: %.2f scale: %.2f sky brightness: %s";
        return String.format(format, worldTime.getTime(), worldTime.getTimeScale(), worldTime.getSkyBrightness());
    }

    private String viewChunk(World world) {
        WorldRender.ChunkViewed viewingChunks = world.getRender().getChunksInView();

        int chunksHor = viewingChunks.getHorizontalLength();
        int chunksVer = viewingChunks.getVerticalLength();

        int chunksInView = chunksHor * chunksVer;
        int blocks = chunksInView * CHUNK_SIZE * CHUNK_SIZE;
        int blocksHor = chunksHor * CHUNK_SIZE;
        int blocksVer = chunksVer * CHUNK_SIZE;
        float zoom = world.getRender().getCamera().zoom;

        String format = "Viewing %d chunks (total %d blocks, w %d b, h %d b) with zoom: %.3f";
        return String.format(format, chunksInView, blocks, blocksHor, blocksVer, zoom);
    }

    private String pos(LivingEntity player) {
        if (player == null) { return "No player"; }

        Vector2 velocity = player.getVelocity();
        Vector2 position = player.getPosition();

        boolean onGround = player.isOnGround();
        boolean flying = player.isFlying();

        String format = "p: (% 8.2f,% 8.2f) v: (% 8.2f,% 8.2f) g? %-5b f? %-5b";
        return String.format(format, position.x, position.y, velocity.x, velocity.y, onGround, flying);
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
