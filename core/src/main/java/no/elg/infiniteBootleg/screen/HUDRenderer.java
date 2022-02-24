package no.elg.infiniteBootleg.screen;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;

import box2dLight.PublicRayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Renderer;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.input.EntityControls;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import no.elg.infiniteBootleg.world.render.ClientChunksInView;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import no.elg.infiniteBootleg.world.time.WorldTime;

/** @author Elg */
public class HUDRenderer implements Renderer {

  private HUDModus modus;

  public HUDRenderer() {
    modus = Settings.debug ? HUDModus.DEBUG : HUDModus.NORMAL;
  }

  @Override
  public void render() {
    if (modus == HUDModus.NONE || Main.isServer()) {
      return;
    }
    ClientMain main = ClientMain.inst();
    ClientWorld world = main.getWorld();
    int h = Gdx.graphics.getHeight();

    LivingEntity player = ClientMain.inst().getPlayer();
    ScreenRenderer sr = ClientMain.inst().getScreenRenderer();
    if (modus == HUDModus.DEBUG) {
      int mouseBlockX = main.getMouseBlockX();
      int mouseBlockY = main.getMouseBlockY();

      sr.begin();
      sr.drawTop(fpsString(world), 1);
      sr.drawTop(pointing(mouseBlockX, mouseBlockY, world), 3);
      sr.drawTop(chunk(mouseBlockX, mouseBlockY, world), 5);
      sr.drawTop(viewChunk(world, player), 7);
      sr.drawTop(pos(player), 9);
      sr.drawTop(time(world), 11);
      sr.drawTop(lights(world), 13);
      sr.drawTop(ents(world), 15);
    } else {
      sr.begin();
    }
    if (player != null) {
      final EntityControls controls = player.getControls();
      Material mat = controls != null ? controls.getSelected() : null;
      if (mat != null && mat.getTextureRegion() != null) {
        sr.getBatch()
            .draw(
                mat.getTextureRegion(),
                Gdx.graphics.getWidth() - BLOCK_SIZE * 3f * ClientMain.SCALE,
                h - BLOCK_SIZE * 3f * ClientMain.SCALE,
                BLOCK_SIZE * 2f * ClientMain.SCALE,
                BLOCK_SIZE * 2f * ClientMain.SCALE);
      }
    }
    sr.end();
  }

  private String lights(ClientWorld world) {
    final PublicRayHandler handler = world.getRender().getRayHandler();
    return "Active Lights:" + handler.getEnabledLights().size;
  }

  private String ents(ClientWorld world) {
    String nl = "\n    ";
    StringBuilder ents = new StringBuilder("E = ");

    for (Entity entity :
        world.getEntities(ClientMain.inst().getMouseX(), ClientMain.inst().getMouseY())) {
      ents.append(entity.simpleName()).append("[").append(entity.hudDebug()).append("]").append(nl);
    }
    int index = ents.lastIndexOf(nl);
    if (index != -1) {
      ents.deleteCharAt(index);
    }
    return ents.toString().trim();
  }

  private String fpsString(ClientWorld world) {
    int activeThreads = Main.inst().getScheduler().getActiveThreads();
    long tpsDelta = TimeUtils.nanosToMillis(world.getWorldTicker().getTpsDelta());
    long realTPS = world.getWorldTicker().getRealTPS();
    float fpsDelta = Gdx.graphics.getDeltaTime();
    int tps = Gdx.graphics.getFramesPerSecond();

    String format = "FPS: %4d delta: %.5f tps: %2d tps delta: %3d ms active threads %d";
    return String.format(format, tps, fpsDelta, realTPS, tpsDelta, activeThreads);
  }

  private String pointing(int mouseBlockX, int mouseBlockY, ClientWorld world) {

    Block block = world.getBlock(mouseBlockX, mouseBlockY, true);
    Material material = block != null ? block.getMaterial() : Material.AIR;
    float rawX = ClientMain.inst().getMouseX();
    float rawY = ClientMain.inst().getMouseY();
    boolean exists = block != null;
    final String blockDebug = block != null ? block.hudDebug() : "";

    String format = "Pointing at %-5s (% 8.2f,% 8.2f) block (% 5d,% 5d) exists? %-5b %s";
    return String.format(
        format, material, rawX, rawY, mouseBlockX, mouseBlockY, exists, blockDebug);
  }

  private String chunk(int mouseBlockX, int mouseBlockY, ClientWorld world) {

    Chunk pc = world.getChunkFromWorld(mouseBlockX, mouseBlockY);
    int chunkY = CoordUtil.worldToChunk(mouseBlockY);
    int chunkX = CoordUtil.worldToChunk(mouseBlockX);
    if (pc == null) {
      String format = "chunk (% 4d,% 4d) : not loaded";
      return String.format(format, chunkX, chunkY);
    } else {
      Biome biome = world.getChunkLoader().getGenerator().getBiome(mouseBlockX);
      boolean allAir = pc.isAllAir();
      boolean allowUnloading = pc.isAllowingUnloading();

      String format = "chunk (% 4d,% 4d) : type: %-9.9s just air? %-5b can unload? %-5b";
      return String.format(format, chunkX, chunkY, biome, allAir, allowUnloading);
    }
  }

  private String time(ClientWorld world) {
    WorldTime worldTime = world.getWorldTime();
    String format = "time: %.2f scale: %.2f sky brightness: %s";
    return String.format(
        format, worldTime.getTime(), worldTime.getTimeScale(), worldTime.getSkyBrightness());
  }

  private String viewChunk(ClientWorld world, Entity player) {
    ClientChunksInView viewingChunks = world.getRender().getChunksInView();

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
    if (player == null) {
      return "No player";
    }

    Vector2 velocity = player.getVelocity();
    Vector2 position = player.getPosition();
    Vector2 physicsPosition = player.getPhysicsPosition();

    boolean onGround = player.isOnGround();
    boolean flying = player.isFlying();

    String format = "p: (% 8.2f,% 8.2f) v: (% 8.2f,% 8.2f) php: (% 8.2f,% 8.2f) g? %-5b f? %-5b";
    return String.format(
        format,
        position.x,
        position.y,
        velocity.x,
        velocity.y,
        physicsPosition.x,
        physicsPosition.y,
        onGround,
        flying);
  }

  public HUDModus getModus() {
    return modus;
  }

  public void setModus(HUDModus modus) {
    this.modus = modus;
  }

  /** How much information to show */
  public enum HUDModus {
    DEBUG,
    NORMAL,
    NONE
  }
}
