package no.elg.infiniteBootleg.world;

import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static no.elg.infiniteBootleg.world.Material.AIR;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.google.common.base.Preconditions;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.blocks.TickingBlock;
import no.elg.infiniteBootleg.world.box2d.ChunkBody;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChunkImpl implements Chunk {

  private static final ProtoWorld.Block.Builder AIR_BLOCK_BUILDER = Block.save(AIR);

  @NotNull private final World world;
  @Nullable private final Block[][] blocks;

  private final int chunkX;
  private final int chunkY;

  /**
   * Must be accessed under a synchronized self block i.e {@code synchronized(tickingBlocks){...}}
   */
  private final Array<TickingBlock> tickingBlocks;

  private final ChunkBody chunkBody;
  // if this chunk should be prioritized to be updated
  private volatile boolean dirty; // if texture/allair needs to be updated
  private volatile boolean prioritize;
  private volatile boolean modified; // if the chunk has been modified since loaded
  private volatile boolean loaded; // once unloaded it no longer is valid
  private volatile boolean allowUnload;
  private volatile boolean initializing;
  private volatile boolean allAir;
  private volatile long lastViewedTick;
  private TextureRegion fboRegion;
  private FileHandle chunkFile;
  private FrameBuffer fbo;

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
  public ChunkImpl(@NotNull World world, int chunkX, int chunkY, @NotNull Block[][] blocks) {
    Preconditions.checkArgument(blocks.length == CHUNK_SIZE);
    Preconditions.checkArgument(blocks[0].length == CHUNK_SIZE);
    this.world = world;
    this.blocks = blocks;
    this.chunkX = chunkX;
    this.chunkY = chunkY;

    tickingBlocks = new Array<>(false, CHUNK_SIZE);
    chunkBody = new ChunkBody(this);

    dirty = true;
    prioritize = false;

    allAir = false;
    loaded = true;
    allowUnload = true;
    modified = false;
    initializing = true;
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
    Preconditions.checkState(loaded, "Chunk is not loaded");

    if (block != null) {
      Preconditions.checkArgument(block.getLocalX() == localX);
      Preconditions.checkArgument(block.getLocalY() == localY);
      Preconditions.checkArgument(block.getChunk() == this);
    }
    synchronized (this) {
      Block currBlock = blocks[localX][localY];

      if (currBlock == block) {
        return currBlock;
      }
      // accounts for both being null also ofc
      if (isAirish(currBlock, block)) {
        // Ok to return here, only internal change
        if (currBlock != null) {
          currBlock.dispose();
          blocks[localX][localY] = null;
          return null;
        }
        return block;
      }

      if (currBlock != null) {
        currBlock.dispose();

        if (currBlock instanceof TickingBlock tickingBlock) {
          tickingBlocks.removeValue(tickingBlock, true);
        }
      }

      blocks[localX][localY] = block;
      if (block instanceof TickingBlock tickingBlock) {
        tickingBlocks.add(tickingBlock);
      }

      modified = true;
      if (updateTexture) {
        dirty = true;
        this.prioritize |= prioritize; // do not remove prioritization if it already is
      }
    }
    if (sendUpdatePacket && isValid()) {
      if (Main.isServer()) {
        Main.inst()
            .getScheduler()
            .executeAsync(
                () -> {
                  final int x = getWorldX(localX);
                  final int y = getWorldY(localY);
                  var packet = PacketExtraKt.clientBoundBlockUpdate(x, y, block);
                  PacketExtraKt.broadcast(packet, null);
                });
      } else if (Main.isServerClient()) {
        Main.inst()
            .getScheduler()
            .executeAsync(
                () -> {
                  var client = ClientMain.inst().getServerClient();
                  if (client != null) {
                    var packet =
                        PacketExtraKt.serverBoundBlockUpdate(
                            client, getWorldX(localX), getWorldY(localY), block);
                    client.ctx.writeAndFlush(packet);
                  }
                });
      }
    }
    if (updateTexture) {
      // TODO maybe this can be done async? (it was before)
      world.updateBlocksAround(getWorldX(localX), getWorldY(localY));
    }
    return block;
  }

  private static boolean isAirish(@Nullable Block blockA, @Nullable Block blockB) {
    return (blockA == null && blockB == null) //
        || (blockA == null && blockB.getMaterial() == AIR) //
        || (blockB == null && blockA.getMaterial() == AIR);
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
  public void updateTexture(boolean prioritize) {
    dirty = true;
    modified = true;
    this.prioritize = prioritize;
  }

  @Override
  @Nullable
  public TextureRegion getTextureRegion() {
    if (dirty) {
      updateTextureIfDirty();
    }
    return fboRegion;
  }

  @Override
  public void updateTextureIfDirty() {
    if (initializing) {
      return;
    }
    synchronized (this) {
      if (!dirty) {
        return;
      }
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
    if (Main.isServer()) {
      Main.inst().getScheduler().executeSync(() -> this.getChunkBody().update(true));
    } else {
      world.getRender().getChunkRenderer().queueRendering(this, prioritize);
      prioritize = false;
    }
  }

  @Override
  public void view() {
    lastViewedTick = world.getTick();
  }

  @Override
  public synchronized FrameBuffer getFbo() {
    if (fbo == null) {
      fbo = new FrameBuffer(Pixmap.Format.RGBA4444, CHUNK_TEXTURE_SIZE, CHUNK_TEXTURE_SIZE, false);
      fbo.getColorBufferTexture()
          .setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      fboRegion = new TextureRegion(fbo.getColorBufferTexture());
      fboRegion.flip(false, true);
    }
    return fbo;
  }

  /** Update all updatable blocks in this chunk */
  @Override
  public void tick() {
    Preconditions.checkState(loaded, "Chunk is not loaded");
    // OK to not synchronize over tickingBlocks as the iterator implementation should not result in
    // any errors
    for (TickingBlock block : tickingBlocks) {
      block.tryTick(false);
    }
  }

  @Override
  public void tickRare() {
    Preconditions.checkState(loaded, "Chunk is not loaded");
    // OK to not synchronize over tickingBlocks as the iterator implementation should not result in
    // any errors
    for (TickingBlock block : tickingBlocks) {
      block.tryTick(true);
    }
  }

  @NotNull
  @Override
  public Block[][] getBlocks() {
    return blocks;
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
      updateTextureIfDirty();
    }

    return allAir;
  }

  @Override
  public boolean isLoaded() {
    return loaded;
  }

  @Override
  public boolean isValid() {
    return loaded && !initializing;
  }

  @Override
  public void setAllowUnload(boolean allowUnload) {
    if (!loaded) {
      return; // already unloaded
    }
    this.allowUnload = allowUnload;
  }

  @Override
  public boolean isAllowingUnloading() {
    if (Settings.client) {
      var player = ClientMain.inst().getPlayer();
      if (player != null && this.equals(player.getChunk())) {
        return false;
      }
    }
    return allowUnload;
  }

  @Override
  @NotNull
  public World getWorld() {
    return world;
  }

  @Override
  public int getChunkX() {
    return chunkX;
  }

  @Override
  public int getChunkY() {
    return chunkY;
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

  /** @return If the chunk has been modified since creation */
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
    Preconditions.checkState(loaded, "Chunk is not loaded");
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
    float minX = getWorldX();
    float maxX = minX + Chunk.CHUNK_SIZE;
    float minY = getWorldY();
    float maxY = minY + Chunk.CHUNK_SIZE;
    for (Entity entity : world.getEntities()) {
      Vector2 pos = entity.getPosition();
      if (Util.isBetween(minX, pos.x, maxX) && Util.isBetween(minY, pos.y, maxY)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Array<Entity> getEntities() {
    Array<Entity> foundEntities = new Array<>(false, 5);

    float minX = getWorldX();
    float maxX = minX + Chunk.CHUNK_SIZE;
    float minY = getWorldY();
    float maxY = minY + Chunk.CHUNK_SIZE;

    for (Entity entity : world.getEntities()) {
      Vector2 pos = entity.getPosition();
      if (Util.isBetween(minX, pos.x, maxX) && Util.isBetween(minY, pos.y, maxY)) {
        foundEntities.add(entity);
      }
    }
    return foundEntities;
  }

  @Override
  public synchronized void dispose() {
    loaded = false;
    allowUnload = false;

    if (fbo != null) {
      Main.inst().getScheduler().executeSync(fbo::dispose);
    }

    chunkBody.dispose();
    tickingBlocks.clear();

    for (Block[] blockArr : blocks) {
      for (Block block : blockArr) {
        if (block != null) {
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

  /** Allow textures to be loaded. Only call once per chunk */
  public synchronized void finishLoading() {
    if (!initializing) {
      return;
    }
    initializing = false;

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
    final ProtoWorld.Chunk.Builder builder = ProtoWorld.Chunk.newBuilder();
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

    final ProtoWorld.Vector2i chunkPosition = protoChunk.getPosition();
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
      for (int y = 0; y < CHUNK_SIZE; y++) {
        for (int x = 0; x < CHUNK_SIZE; x++) {
          if (blocks[x][y] != null) {
            throw new IllegalStateException("Double assemble");
          }
          var protoBlock = protoBlocks.get(index++);

          blocks[x][y] = Block.fromProto(world, this, x, y, protoBlock);
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
        + ", loaded="
        + loaded
        + '}';
  }
}
