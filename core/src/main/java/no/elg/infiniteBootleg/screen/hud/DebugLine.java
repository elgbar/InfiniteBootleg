package no.elg.infiniteBootleg.screen.hud;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;

import box2dLight.PublicRayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import no.elg.infiniteBootleg.world.render.ClientChunksInView;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import no.elg.infiniteBootleg.world.time.WorldTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DebugLine {

  private DebugLine() {}

  public static void fpsString(@NotNull StringBuilder sb, @Nullable ClientWorld world) {
    int activeThreads = Main.inst().getScheduler().getActiveThreads();
    Ticker worldTicker = world == null ? null : world.getWorldTicker();
    long tpsDelta = worldTicker == null ? -1 : TimeUtils.nanosToMillis(worldTicker.getTpsDelta());
    long realTPS = worldTicker == null ? -1 : worldTicker.getRealTPS();
    float fpsDelta = Gdx.graphics.getDeltaTime();
    int tps = Gdx.graphics.getFramesPerSecond();

    String format = "FPS: %4d delta: %.5f tps: %2d tps delta: %3d ms active threads %d";
    sb.append(String.format(format, tps, fpsDelta, realTPS, tpsDelta, activeThreads));
  }

  public static void lights(@NotNull StringBuilder sb, @NotNull ClientWorld world) {
    sb.append("Active Lights: ");
    final PublicRayHandler handler = world.getRender().getRayHandler();
    sb.append(handler.getEnabledLights().size);
  }

  public static void pointing(
      @NotNull StringBuilder sb, @NotNull ClientWorld world, int mouseBlockX, int mouseBlockY) {
    Block block = world.getBlock(mouseBlockX, mouseBlockY, true);
    Material material = block != null ? block.getMaterial() : Material.AIR;
    float rawX = ClientMain.inst().getMouseX();
    float rawY = ClientMain.inst().getMouseY();
    boolean exists = block != null;
    final String blockDebug = block != null ? block.hudDebug() : "";

    String format = "Pointing at %-5s (% 8.2f,% 8.2f) block (% 5d,% 5d) exists? %-5b %s";
    sb.append(
        String.format(format, material, rawX, rawY, mouseBlockX, mouseBlockY, exists, blockDebug));
  }

  public static void chunk(
      @NotNull StringBuilder sb, @NotNull ClientWorld world, int mouseBlockX, int mouseBlockY) {
    Chunk pc = world.getChunkFromWorld(mouseBlockX, mouseBlockY);
    int chunkY = CoordUtil.worldToChunk(mouseBlockY);
    int chunkX = CoordUtil.worldToChunk(mouseBlockX);
    if (pc == null) {
      String format = "chunk (% 4d,% 4d) : not loaded";
      sb.append(String.format(format, chunkX, chunkY));
    } else {
      Biome biome = world.getChunkLoader().getGenerator().getBiome(mouseBlockX);
      boolean allAir = pc.isAllAir();
      boolean allowUnloading = pc.isAllowingUnloading();

      String format = "chunk (% 4d,% 4d) : type: %-9.9s just air? %-5b can unload? %-5b";
      sb.append(String.format(format, chunkX, chunkY, biome, allAir, allowUnloading));
    }
  }

  public static void time(@NotNull StringBuilder sb, @NotNull ClientWorld world) {
    WorldTime worldTime = world.getWorldTime();
    String format = "time: %.2f (%.2f) scale: %.2f sky brightness: %.2f TOD: %s";
    sb.append(
        String.format(
            format,
            worldTime.getTime(),
            worldTime.normalizedTime(),
            worldTime.getTimeScale(),
            worldTime.getSkyBrightness(),
            worldTime.timeOfDay(worldTime.getTime())));
  }

  public static void viewChunk(@NotNull StringBuilder sb, @NotNull ClientWorld world) {
    ClientChunksInView viewingChunks = world.getRender().getChunksInView();

    int chunksHor = viewingChunks.getHorizontalLength();
    int chunksVer = viewingChunks.getVerticalLength();

    int chunksInView = chunksHor * chunksVer;
    int blocks = chunksInView * CHUNK_SIZE * CHUNK_SIZE;
    int blocksHor = chunksHor * CHUNK_SIZE;
    int blocksVer = chunksVer * CHUNK_SIZE;
    float zoom = world.getRender().getCamera().zoom;

    String format = "Viewing %d chunks (total %d blocks, w %d b, h %d b) with zoom: %.3f";
    sb.append(String.format(format, chunksInView, blocks, blocksHor, blocksVer, zoom));
  }

  public static void pos(@NotNull StringBuilder sb, @Nullable LivingEntity player) {
    if (player == null) {
      sb.append("No player");
      return;
    }

    Vector2 velocity = player.getVelocity();
    Vector2 position = player.getPosition();
    Vector2 physicsPosition = player.getPhysicsPosition();

    boolean onGround = player.isOnGround();
    boolean flying = player.isFlying();

    String format = "p: (% 8.2f,% 8.2f) v: (% 8.2f,% 8.2f) php: (% 8.2f,% 8.2f) g? %-5b f? %-5b";
    sb.append(
        String.format(
            format,
            position.x,
            position.y,
            velocity.x,
            velocity.y,
            physicsPosition.x,
            physicsPosition.y,
            onGround,
            flying));
  }

  public static void ents(@NotNull StringBuilder sb, @NotNull ClientWorld world) {
    String nl = "\n    ";
    sb.append("E = ");
    for (Entity entity :
        world.getEntities(ClientMain.inst().getMouseX(), ClientMain.inst().getMouseY())) {
      sb.append(entity.simpleName()).append("[").append(entity.hudDebug()).append("]").append(nl);
    }
  }
}
