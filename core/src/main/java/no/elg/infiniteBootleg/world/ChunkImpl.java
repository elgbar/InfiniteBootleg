package no.elg.infiniteBootleg.world;

import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static no.elg.infiniteBootleg.world.Material.AIR;
import static no.elg.infiniteBootleg.world.World.LIGHT_SOURCE_LOOK_BLOCKS;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.google.common.base.Preconditions;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.concurrent.GuardedBy;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.events.BlockChangedEvent;
import no.elg.infiniteBootleg.events.api.EventListener;
import no.elg.infiniteBootleg.events.api.EventManager;
import no.elg.infiniteBootleg.events.chunks.ChunkLightUpdatedEvent;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.util.ChunkUtilKt;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.blocks.TickingBlock;
import no.elg.infiniteBootleg.world.box2d.ChunkBody;
import no.elg.infiniteBootleg.world.render.ClientWorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChunkImpl implements Chunk {

  private static final ProtoWorld.Block.Builder AIR_BLOCK_BUILDER = Block.save(AIR);

  public final AtomicInteger currentUpdateId = new AtomicInteger();

  @NotNull private final World world;
  @Nullable private final Block @NotNull [] @NotNull [] blocks;

  @NotNull
  private final BlockLight @NotNull [] @NotNull [] blockLights =
      new BlockLight[CHUNK_SIZE][CHUNK_SIZE];

  private final int chunkX;
  private final int chunkY;

  /**
   * Must be accessed under a synchronized self block i.e, {@code synchronized(tickingBlocks){...}}
   */
  @NotNull private final Array<@NotNull TickingBlock> tickingBlocks;

  @NotNull private final ChunkBody chunkBody;
  @Nullable private volatile ScheduledFuture<?> lightUpdater;

  @NotNull
  private final ObjectSet<ForkJoinTask<?>> tasks = new ObjectSet<>(CHUNK_SIZE * CHUNK_SIZE, 0.99f);

  // if texture/allair needs to be updated
  /** Use {@link #dirty()} to mark chunk as dirty */
  private volatile boolean dirty;

  // if this chunk should be prioritized to be updated
  private boolean prioritize;
  private volatile boolean modified; // if the chunk has been modified since loaded
  private volatile boolean allowUnload;
  /**
   * If the chunk is still being initialized, meaning not all blocks are in {@link #blocks} and
   * {@link #tickingBlocks} does not contain all blocks it should
   */
  private volatile boolean initializing;

  private volatile boolean allAir;
  private volatile boolean disposed;
  private volatile long lastViewedTick;

  @GuardedBy("fboLock")
  @Nullable
  private TextureRegion fboRegion;

  @GuardedBy("fboLock")
  @Nullable
  private FrameBuffer fbo;

  private final Object fboLock = new Object();

  private final EventListener<ChunkLightUpdatedEvent> updateChunkLightEventListener =
      (ChunkLightUpdatedEvent event) -> {
        if (ChunkUtilKt.isNeighbor(this, event.getChunk())) {
          Direction dir = ChunkUtilKt.directionTo(event.getChunk(), this);

          int localX = (int) (event.getLocalX() + dir.dx * LIGHT_SOURCE_LOOK_BLOCKS);
          int localY = (int) (event.getLocalY() + dir.dy * LIGHT_SOURCE_LOOK_BLOCKS);

          boolean xCheck =
              switch (dir.dx) {
                case -1 -> localX < 0;
                case 0 -> true;
                case 1 -> localX > CHUNK_SIZE;
                default -> false;
              };

          boolean yCheck =
              switch (dir.dy) {
                case -1 -> localY < 0;
                case 0 -> true;
                case 1 -> localY > CHUNK_SIZE;
                default -> false;
              };

          if (xCheck && yCheck) {
            updateBlockLights(CoordUtil.chunkOffset(localX), CoordUtil.chunkOffset(localY), false);
          }
        }
      };

  /**
   * Create a new empty chunk
   *
   * @param world The world this chunk exists in
   * @param chunkX The position x of this chunk the given world
   * @param chunkY The position y of this chunk the given world
   */
  public ChunkImpl(@NotNull World world, int chunkX, int chunkY) {
    this(world, chunkX, chunkY, new Block[CHUNK_SIZE][CHUNK_SIZE]);
  }

  /**
   * @param world The world this chunk exists in
   * @param chunkX The position x of this chunk the given world
   * @param chunkY The position y of this chunk the given world
   * @param blocks The initial blocks of this chunk (note: must be {@link #CHUNK_SIZE}x{@link
   *     #CHUNK_SIZE})
   */
  public ChunkImpl(
      @NotNull World world, int chunkX, int chunkY, @NotNull Block @NotNull [] @NotNull [] blocks) {
    Preconditions.checkArgument(blocks.length == CHUNK_SIZE);
    Preconditions.checkArgument(blocks[0].length == CHUNK_SIZE);
    this.world = world;
    this.blocks = blocks;
    this.chunkX = chunkX;
    this.chunkY = chunkY;

    tickingBlocks = new Array<>(false, CHUNK_SIZE);
    chunkBody = new ChunkBody(this);

    dirty();
    prioritize = false;

    allAir = false;
    disposed = false;
    allowUnload = true;
    modified = false;
    initializing = true;

    for (int x = 0; x < CHUNK_SIZE; x++) {
      for (int y = 0; y < CHUNK_SIZE; y++) {
        blockLights[x][y] = new BlockLight(this, x, y);
      }
    }
  }

  @Override
  @Contract("_,_,!null->!null;_,_,null->null")
  public Block setBlock(int localX, int localY, @Nullable Material material) {
    return setBlock(localX, localY, material, true);
  }

  @Override
  @Contract("_,_,!null,_->!null;_,_,null,_->null")
  public Block setBlock(int localX, int localY, @Nullable Material material, boolean update) {
    return setBlock(localX, localY, material, update, false);
  }

  @Override
  @Contract("_, _, !null, _, _ -> !null; _, _, null, _, _ -> null")
  public Block setBlock(
      int localX, int localY, @Nullable Material material, boolean update, boolean prioritize) {
    Block block = material == null ? null : material.createBlock(world, this, localX, localY);
    return setBlock(localX, localY, block, update, prioritize);
  }

  @Override
  @Contract("_,_,!null,_->!null;_,_,null,_->null")
  public Block setBlock(int localX, int localY, @Nullable Block block, boolean updateTexture) {
    return setBlock(localX, localY, block, updateTexture, false);
  }

  @Override
  @Contract("_, _, !null, _, _ -> !null; _, _, null, _, _ -> null")
  public Block setBlock(
      int localX, int localY, @Nullable Block block, boolean updateTexture, boolean prioritize) {
    return setBlock(localX, localY, block, updateTexture, prioritize, true);
  }

  @Override
  public Block setBlock(
      int localX,
      int localY,
      @Nullable Block block,
      boolean updateTexture,
      boolean prioritize,
      boolean sendUpdatePacket) {
    if (isDisposed()) {
      Main.logger()
          .warn(
              "Changed block in disposed chunk "
                  + CoordUtil.stringifyChunkToWorld(this, localX, localY)
                  + ", block: "
                  + block);
      return null;
    }

    if (block != null) {
      Preconditions.checkArgument(block.getLocalX() == localX);
      Preconditions.checkArgument(block.getLocalY() == localY);
      Preconditions.checkArgument(block.getChunk() == this);
    }
    boolean bothAirish;
    synchronized (this) {
      @Nullable Block currBlock = blocks[localX][localY];

      if (currBlock == block) {
        return currBlock;
      }
      // accounts for both being null also ofc
      bothAirish = areBothAirish(currBlock, block);
      if (bothAirish && currBlock != null) {
        // Ok to return here, an air block exists here and the new block is also air (or null)
        if (block != null) {
          block.dispose();
        }
        return currBlock;
      }

      if (currBlock != null) {
        currBlock.dispose();

        if (currBlock instanceof TickingBlock tickingBlock) {
          synchronized (tickingBlocks) {
            tickingBlocks.removeValue(tickingBlock, true);
          }
        }
      }

      blocks[localX][localY] = block;
      if (block instanceof TickingBlock tickingBlock) {
        synchronized (tickingBlocks) {
          tickingBlocks.add(tickingBlock);
        }
      }
      if ((block != null && block.getMaterial().isLuminescent())
          || (currBlock != null && currBlock.getMaterial().isLuminescent())) {
        updateBlockLights(localX, localY, true);
      }

      modified = true;
      if (updateTexture) {
        dirty();
        this.prioritize |= prioritize; // do not remove prioritization if it already is
      }
      if (!bothAirish) {
        if (currBlock != null) {
          chunkBody.removeBlock(currBlock);
        }
        if (block != null) {
          chunkBody.addBlock(block, null);
        }
        Main.inst()
            .getScheduler()
            .executeAsync(() -> getChunkColumn().updateTopBlock(localX, getWorldY(localY)));
      }
      EventManager.INSTANCE.javaDispatchEvent(new BlockChangedEvent(currBlock, block));
    }
    int worldX = getWorldX(localX);
    int worldY = getWorldY(localY);

    if (sendUpdatePacket && isValid() && !bothAirish) {
      if (Main.isServer()) {
        Main.inst()
            .getScheduler()
            .executeAsync(
                () -> {
                  var packet = PacketExtraKt.clientBoundBlockUpdate(worldX, worldY, block);
                  PacketExtraKt.broadcastToInView(packet, worldX, worldY, null);
                });
      } else if (Main.isServerClient()) {
        Main.inst()
            .getScheduler()
            .executeAsync(
                () -> {
                  var client = ClientMain.inst().getServerClient();
                  if (client != null) {
                    var packet =
                        PacketExtraKt.serverBoundBlockUpdate(client, worldX, worldY, block);
                    client.ctx.writeAndFlush(packet);
                  }
                });
      }
    }
    if (updateTexture) {
      Main.inst().getScheduler().executeAsync(() -> world.updateBlocksAround(worldX, worldY));
    }
    return block;
  }

  private static boolean areBothAirish(@Nullable Block blockA, @Nullable Block blockB) {
    return (blockA == null && blockB == null) //
        || (blockA == null && blockB.getMaterial() == AIR) //
        || (blockB == null && blockA.getMaterial() == AIR)
        || (blockA != null
            && blockB != null
            && blockA.getMaterial() == AIR
            && blockB.getMaterial() == AIR);
  }

  @Override
  public int getWorldX(int localX) {
    return CoordUtil.chunkToWorld(chunkX, localX);
  }

  @Override
  public int getWorldY(int localY) {
    return CoordUtil.chunkToWorld(chunkY, localY);
  }

  @Override
  public synchronized void updateTexture(boolean prioritize) {
    dirty();
    modified = true;
    this.prioritize |= prioritize;
  }

  @Override
  @Nullable
  public TextureRegion getTextureRegion() {
    synchronized (fboLock) {
      if (dirty) {
        updateIfDirty();
      }
      return fboRegion;
    }
  }

  @Override
  public boolean hasTextureRegion() {
    synchronized (fboLock) {
      return fboRegion != null;
    }
  }

  @Override
  public void updateIfDirty() {
    if (isInvalid()) {
      return;
    }
    boolean wasPrioritize;
    synchronized (this) {
      if (!dirty || initializing) {
        return;
      }
      wasPrioritize = prioritize;
      prioritize = false;
      dirty = false;

      // test if all the blocks in this chunk has the material air
      allAir = true;
      outer:
      for (int localX = 0; localX < CHUNK_SIZE; localX++) {
        for (int localY = 0; localY < CHUNK_SIZE; localY++) {
          Block b = blocks[localX][localY];
          if (b != null && b.getMaterial() != AIR) {
            allAir = false;
            break outer;
          }
        }
      }
    }

    // Render the world with the changes (but potentially without the light changes)
    if (world.getRender() instanceof ClientWorldRender clientWorldRender) {
      clientWorldRender.getChunkRenderer().queueRendering(this, wasPrioritize);
    }
  }

  private void cancelCurrentBlockLightUpdate() {
    if (Settings.renderLight) {
      synchronized (blockLights) {
        // If we reached this point before the light is done recalculating then we must start again
        var currLU = lightUpdater;
        if (currLU != null) {
          // Note that the previous thread will dispose itself (therefor it should not be
          // interrupted)
          currLU.cancel(false);
          lightUpdater = null;
        }
      }
    }
  }

  public void updateBlockLights(int localX, int localY, boolean dispatchEvent) {
    if (Settings.renderLight) {
      if (dispatchEvent) {
        EventManager.INSTANCE.javaDispatchEvent(new ChunkLightUpdatedEvent(this, localX, localY));
      }
      synchronized (blockLights) {
        // If we reached this point before the light is done recalculating then we must start again
        cancelCurrentBlockLightUpdate();
        int updateId = currentUpdateId.incrementAndGet();
        lightUpdater = Main.inst().getScheduler().executeAsync(() -> updateBlockLights(updateId));
      }
    }
  }

  /** Should only be used by {@link #updateBlockLights()} */
  private void updateBlockLights(int updateId) {
    var pool = ForkJoinPool.commonPool();
    synchronized (tasks) {
      outer:
      for (int localX = 0; localX < CHUNK_SIZE; localX++) {
        for (int localY = CHUNK_SIZE - 1; localY >= 0; localY--) {
          if (updateId != currentUpdateId.get()) {
            break outer;
          }
          BlockLight bl = blockLights[localX][localY];
          ForkJoinTask<?> task = pool.submit(() -> bl.recalculateLighting(updateId));
          task.fork();
          tasks.add(task);
        }
      }

      for (ForkJoinTask<?> task : tasks) {
        if (updateId == currentUpdateId.get()) {
          try {
            task.join();
          } catch (CancellationException ignore) {
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else {
          task.cancel(true);
        }
      }
      tasks.clear();
    }
    if (updateId == currentUpdateId.get()) {
      // TODO only re-render if any lights changed

      // Re-render the chunk with the new lighting
      if (world.getRender() instanceof ClientWorldRender clientWorldRender) {
        clientWorldRender.getChunkRenderer().queueRendering(this, true, true);
      }
    }
  }

  @Override
  public void view() {
    lastViewedTick = world.getTick();
  }

  @Override
  @Nullable
  public FrameBuffer getFbo() {
    if (isDisposed()) {
      return null;
    }
    synchronized (fboLock) {
      if (fbo != null) {
        return fbo;
      }
      fbo = new FrameBuffer(Pixmap.Format.RGBA4444, CHUNK_TEXTURE_SIZE, CHUNK_TEXTURE_SIZE, true);
      fbo.getColorBufferTexture()
          .setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      fboRegion = new TextureRegion(fbo.getColorBufferTexture());
      fboRegion.flip(false, true);

      return fbo;
    }
  }

  /** Update all updatable blocks in this chunk */
  @Override
  public void tick() {
    if (isInvalid()) {
      return;
    }
    synchronized (tickingBlocks) {
      for (TickingBlock block : tickingBlocks) {
        block.tryTick(false);
      }
    }
  }

  @Override
  public void tickRare() {
    if (isInvalid()) {
      return;
    }
    synchronized (tickingBlocks) {
      for (TickingBlock block : tickingBlocks) {
        block.tryTick(true);
      }
    }
  }

  @NotNull
  @Override
  public Block[][] getBlocks() {
    return blocks;
  }

  @Override
  public @NotNull BlockLight getBlockLight(int localX, int localY) {
    return blockLights[localX][localY];
  }

  @NotNull
  @Override
  public Array<@NotNull TickingBlock> getTickingBlocks() {
    return tickingBlocks;
  }

  @Override
  @Nullable
  public Block getRawBlock(int localX, int localY) {
    return blocks[localX][localY];
  }

  @Override
  public boolean isAllAir() {
    if (dirty) {
      updateIfDirty();
    }
    return allAir;
  }

  @Override
  public boolean isNotDisposed() {
    return !disposed;
  }

  @Override
  public boolean isValid() {
    return !disposed && !initializing;
  }

  @Override
  public boolean isInvalid() {
    return disposed || initializing;
  }

  @Override
  public void setAllowUnload(boolean allowUnload) {
    if (disposed) {
      return; // already unloaded
    }
    this.allowUnload = allowUnload;
  }

  @Override
  public boolean isAllowingUnloading() {
    if (Settings.client) {
      //      var player = ClientMain.inst().getPlayer();
      //      if (player != null && equals(player.getChunk())) {
      //        return false;
      //      }
    }
    return allowUnload;
  }

  @Override
  @NotNull
  public World getWorld() {
    return world;
  }

  @Override
  public @NotNull ChunkColumn getChunkColumn() {
    return world.getChunkColumn(chunkX);
  }

  @Override
  public int getChunkX() {
    return chunkX;
  }

  @Override
  public int getChunkY() {
    return chunkY;
  }

  @Override
  @Contract(pure = true)
  public long getCompactLocation() {
    return CoordUtil.compactLoc(chunkX, chunkY);
  }

  /**
   * @return Location of this chunk in world coordinates
   * @see CoordUtil#chunkToWorld(Location)
   */
  @Override
  public int getWorldX() {
    return CoordUtil.chunkToWorld(chunkX);
  }

  /**
   * This is the same as doing {@code CoordUtil.chunkToWorld(getLocation())}
   *
   * @return Location of this chunk in world coordinates
   * @see CoordUtil#chunkToWorld(Location)
   */
  @Override
  public int getWorldY() {
    return CoordUtil.chunkToWorld(chunkY);
  }

  /**
   * f
   *
   * @return The last tick this chunk's texture was pulled
   */
  @Override
  public long getLastViewedTick() {
    return lastViewedTick;
  }

  /**
   * @return If the chunk has been modified since creation
   */
  @Override
  public boolean shouldSave() {
    return modified || !getEntities().isEmpty();
  }

  @Override
  public Stream<@Nullable Block> stream() {
    Spliterator<@Nullable Block> spliterator =
        Spliterators.spliterator(
            iterator(), (long) CHUNK_SIZE * CHUNK_SIZE, SIZED | DISTINCT | ORDERED);
    return StreamSupport.stream(spliterator, false);
  }

  @NotNull
  @Override
  public Iterator<@Nullable Block> iterator() {
    return new Iterator<>() {
      int x;
      int y;

      @Override
      public boolean hasNext() {
        return y < CHUNK_SIZE - 1 || x < CHUNK_SIZE;
      }

      @Override
      public Block next() {
        if (x == CHUNK_SIZE) {
          x = 0;
          y++;
        }
        if (y >= CHUNK_SIZE) {
          throw new NoSuchElementException();
        }
        return getRawBlock(x++, y);
      }
    };
  }

  /**
   * @param localX The local x ie a value between 0 and {@link #CHUNK_SIZE}
   * @param localY The local y ie a value between 0 and {@link #CHUNK_SIZE}
   * @return A block from the relative coordinates
   */
  @Override
  @NotNull
  public Block getBlock(int localX, int localY) {
    if (!isValid()) {
      Main.logger()
          .warn(
              "Fetched block from invalid chunk "
                  + CoordUtil.stringifyChunkToWorld(this, localX, localY));
    }
    Preconditions.checkArgument(
        CoordUtil.isInsideChunk(localX, localY),
        "Given arguments are not inside this chunk, localX=" + localX + " localY=" + localY);
    synchronized (this) {
      Block block = blocks[localX][localY];

      if (block == null) {
        return setBlock(localX, localY, AIR, false);
      }
      return block;
    }
  }

  public boolean hasEntities() {
    //    float minX = getWorldX();
    //    float maxX = minX + Chunk.CHUNK_SIZE;
    //    float minY = getWorldY();
    //    float maxY = minY + Chunk.CHUNK_SIZE;
    //    for (Entity entity : world.getEntities()) {
    //      Vector2 pos = entity.getPosition();
    //      if (Util.isBetween(minX, pos.x, maxX) && Util.isBetween(minY, pos.y, maxY)) {
    //        return true;
    //      }
    //    }
    return false;
  }

  @Override
  public Array<Entity> getEntities() {
    Array<Entity> foundEntities = new Array<>(false, 5);

    //    float minX = getWorldX();
    //    float maxX = minX + Chunk.CHUNK_SIZE;
    //    float minY = getWorldY();
    //    float maxY = minY + Chunk.CHUNK_SIZE;

    //    for (Entity entity : world.getEntities()) {
    //      Vector2 pos = entity.getPosition();
    //      if (Util.isBetween(minX, pos.x, maxX) && Util.isBetween(minY, pos.y, maxY)) {
    //        foundEntities.add(entity);
    //      }
    //    }
    return foundEntities;
  }

  @Override
  public synchronized boolean isDisposed() {
    return disposed;
  }

  @Override
  public synchronized void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;

    allowUnload = false;

    synchronized (fboLock) {
      if (fbo != null) {
        Main.inst().getScheduler().executeSync(fbo::dispose);
        fbo = null;
      }
      fboRegion = null;
    }

    chunkBody.dispose();
    synchronized (tickingBlocks) {
      tickingBlocks.clear();
    }

    for (Block[] blockArr : blocks) {
      for (Block block : blockArr) {
        if (block != null && !block.isDisposed()) {
          block.dispose();
        }
      }
    }
  }

  @Override
  @NotNull
  public ChunkBody getChunkBody() {
    return chunkBody;
  }

  @Override
  public boolean isNeighborsLoaded() {
    for (Direction direction : Direction.CARDINAL) {
      Location relChunk = Location.relative(chunkX, chunkY, direction);
      if (!world.isChunkLoaded(relChunk)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public synchronized boolean isDirty() {
    return dirty;
  }

  @Override
  public synchronized void dirty() {
    dirty = true;
  }

  /**
   * Internal loading of blocks have been completed, finish setting up the internal state but before
   * the chunks have been added to the world
   */
  public synchronized void finishLoading() {
    if (!initializing) {
      return;
    }
    initializing = false;
    dirty();

    synchronized (tickingBlocks) {
      tickingBlocks.clear();
      tickingBlocks.ensureCapacity(CHUNK_SIZE);
      for (int x = 0; x < CHUNK_SIZE; x++) {
        for (int y = 0; y < CHUNK_SIZE; y++) {
          Block block = blocks[x][y];
          if (block instanceof TickingBlock tickingBlock) {
            tickingBlocks.add(tickingBlock);
          }
        }
      }
    }
    chunkBody.update();
    // Register events
    EventManager.INSTANCE.javaRegisterListener(
        ChunkLightUpdatedEvent.class, updateChunkLightEventListener);
    updateBlockLights();
  }

  @NotNull
  @Override
  public ProtoWorld.Chunk save() {
    return save(true);
  }

  @NotNull
  @Override
  public ProtoWorld.Chunk saveBlocksOnly() {
    return save(false);
  }

  @NotNull
  private ProtoWorld.Chunk save(boolean includeEntities) {
    ProtoWorld.Chunk.Builder builder = ProtoWorld.Chunk.newBuilder();
    builder.setPosition(ProtoWorld.Vector2i.newBuilder().setX(chunkX).setY(chunkY).build());

    for (Block block : this) {
      builder.addBlocks(block != null ? block.save() : AIR_BLOCK_BUILDER);
    }
    if (includeEntities) {
      for (Entity entity : getEntities()) {
        if (entity instanceof Player) {
          continue;
        }
        builder.addEntities(entity.save());
      }
    }
    return builder.build();
  }

  @Override
  public boolean load(ProtoWorld.Chunk protoChunk) {

    Preconditions.checkState(
        initializing, "Cannot load from proto chunk after chunk has been initialized");
    ProtoWorld.Vector2i chunkPosition = protoChunk.getPosition();
    var posErrorMsg =
        "Invalid chunk coordinates given. Expected ("
            + chunkX
            + ", "
            + chunkY
            + ") but got ("
            + chunkPosition.getX()
            + ", "
            + chunkPosition.getY()
            + ")";
    Preconditions.checkArgument(chunkPosition.getX() == chunkX, posErrorMsg);
    Preconditions.checkArgument(chunkPosition.getY() == chunkY, posErrorMsg);
    Preconditions.checkArgument(
        protoChunk.getBlocksCount() == CHUNK_SIZE * CHUNK_SIZE,
        "Invalid number of bytes. expected "
            + CHUNK_SIZE * CHUNK_SIZE
            + ", but got "
            + protoChunk.getBlocksCount());
    int index = 0;
    var protoBlocks = protoChunk.getBlocksList();
    synchronized (this) {
      for (int localY = 0; localY < CHUNK_SIZE; localY++) {
        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
          if (blocks[localX][localY] != null) {
            throw new IllegalStateException("Double assemble");
          }
          var protoBlock = protoBlocks.get(index++);
          blocks[localX][localY] = Block.fromProto(world, this, localX, localY, protoBlock);
        }
      }
    }

    for (ProtoWorld.Entity protoEntity : protoChunk.getEntitiesList()) {
      Entity.load(world, this, protoEntity);
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = world.hashCode();
    result = 31 * result + chunkX;
    result = 31 * result + chunkY;
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ChunkImpl chunk)) {
      return false;
    }

    if (getChunkX() != chunk.getChunkX()) {
      return false;
    }
    if (getChunkY() != chunk.getChunkY()) {
      return false;
    }
    return getWorld().equals(chunk.getWorld());
  }

  @Override
  public String toString() {
    return "Chunk{"
        + "world="
        + world
        + ", chunkX="
        + chunkX
        + ", chunkY="
        + chunkY
        + ", valid="
        + isValid()
        + '}';
  }

  @Override
  public int compareTo(@NotNull Chunk o) {
    int compare = Integer.compare(chunkX, o.getChunkX());
    return compare != 0 ? compare : Integer.compare(chunkY, o.getChunkY());
  }
}
