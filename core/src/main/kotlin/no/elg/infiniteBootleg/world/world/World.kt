package no.elg.infiniteBootleg.world.world

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.LongMap
import com.badlogic.gdx.utils.ObjectSet
import com.google.common.base.Preconditions
import com.google.protobuf.InvalidProtocolBufferException
import ktx.collections.GdxArray
import ktx.collections.GdxLongArray
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Resizable
import no.elg.infiniteBootleg.events.InitialChunksOfWorldLoadedEvent
import no.elg.infiniteBootleg.events.WorldLoadedEvent
import no.elg.infiniteBootleg.events.api.EventManager.clear
import no.elg.infiniteBootleg.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.events.api.EventManager.oneShotListener
import no.elg.infiniteBootleg.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.ProtoWorld.WorldOrBuilder
import no.elg.infiniteBootleg.server.despawnEntity
import no.elg.infiniteBootleg.util.Util
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.decompactLocX
import no.elg.infiniteBootleg.util.decompactLocY
import no.elg.infiniteBootleg.util.generateUUIDFromLong
import no.elg.infiniteBootleg.util.isAir
import no.elg.infiniteBootleg.util.isBlockInsideRadius
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.BOX2D_LOCK
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Block.Companion.remove
import no.elg.infiniteBootleg.world.Block.Companion.worldX
import no.elg.infiniteBootleg.world.Block.Companion.worldY
import no.elg.infiniteBootleg.world.BlockLight
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.ChunkColumn
import no.elg.infiniteBootleg.world.ChunkColumnImpl
import no.elg.infiniteBootleg.world.ChunkColumnImpl.Companion.fromProtobuf
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.Location.Companion.fromVector2i
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.TickingBlock
import no.elg.infiniteBootleg.world.box2d.WorldBody
import no.elg.infiniteBootleg.world.ecs.basicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.world.ecs.createSPPlayerEntity
import no.elg.infiniteBootleg.world.ecs.disposeEntitiesOnRemoval
import no.elg.infiniteBootleg.world.ecs.ensureUniquenessListener
import no.elg.infiniteBootleg.world.ecs.playerFamily
import no.elg.infiniteBootleg.world.ecs.save
import no.elg.infiniteBootleg.world.ecs.system.MaxVelocitySystem
import no.elg.infiniteBootleg.world.ecs.system.ReadBox2DStateSystem
import no.elg.infiniteBootleg.world.ecs.system.UpdateBlockGridSystem
import no.elg.infiniteBootleg.world.ecs.system.WriteBox2DStateSystem
import no.elg.infiniteBootleg.world.ecs.system.client.ControlSystem
import no.elg.infiniteBootleg.world.ecs.system.client.FollowEntitySystem
import no.elg.infiniteBootleg.world.ecs.system.event.InputSystem
import no.elg.infiniteBootleg.world.ecs.system.event.PhysicsSystem
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.loader.WorldLoader
import no.elg.infiniteBootleg.world.loader.WorldLoader.canWriteToWorld
import no.elg.infiniteBootleg.world.loader.WorldLoader.deleteLockFile
import no.elg.infiniteBootleg.world.loader.WorldLoader.generatorFromProto
import no.elg.infiniteBootleg.world.loader.WorldLoader.getWorldFolder
import no.elg.infiniteBootleg.world.loader.WorldLoader.writeLockFile
import no.elg.infiniteBootleg.world.loader.chunk.ChunkLoader
import no.elg.infiniteBootleg.world.loader.chunk.FullChunkLoader
import no.elg.infiniteBootleg.world.loader.chunk.ServerClientChunkLoader
import no.elg.infiniteBootleg.world.render.WorldRender
import no.elg.infiniteBootleg.world.ticker.WorldTicker
import no.elg.infiniteBootleg.world.time.WorldTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.concurrent.GuardedBy
import kotlin.math.abs

/**
 * Different kind of views
 *
 *
 *  * Chunk view: One unit in chunk view is [Chunk.CHUNK_SIZE] times larger than a unit in
 * world view
 *  * World view: One unit in world view is [Block.BLOCK_SIZE] times larger than a unit in
 * Box2D view
 *  * Box2D view: 1 (ie base unit)
 *
 *
 * @author Elg
 */
abstract class World(
  generator: ChunkGenerator,
  /**
   * @return The random seed of this world
   */
  val seed: Long,
  worldName: String
) : Disposable, Resizable {

  /**
   * @return Unique identification of this world
   */
  val uuid: String

  val worldTicker: WorldTicker

  val chunkLoader: ChunkLoader = if (this is ServerClientWorld) {
    ServerClientChunkLoader(this, generator)
  } else {
    FullChunkLoader(this, generator)
  }

  val worldBody: WorldBody
  val worldTime: WorldTime
  val engine: Engine

  /**
   * must be accessed under [chunksLock]
   */
  @get:GuardedBy("chunksLock")
  @GuardedBy("chunksLock")
  val chunks = LongMap<Chunk>()

  /**
   * Must be accessed under `synchronized(chunkColumns)`
   */
  protected val chunkColumns = IntMap<ChunkColumn>()

  @Volatile
  private var worldFile: FileHandle? = null

  /**
   * @return The name of the world
   */
  val name: String

  /**
   * Spawn in world coordinates
   */
  var spawn: Location

  @JvmField
  val chunksLock: ReadWriteLock = ReentrantReadWriteLock()

  private var transientWorld = !Settings.loadWorldFromDisk || Main.isServerClient()

  val tick get() = worldTicker.tickId

  constructor(protoWorld: ProtoWorld.World) : this(generatorFromProto(protoWorld), protoWorld.seed, protoWorld.name)

  init {
    MathUtils.random.setSeed(seed)
    uuid = generateUUIDFromLong(seed).toString()
    name = worldName
    worldTicker = WorldTicker(this, false)
    worldTime = WorldTime(this)
    spawn = Location(0, 0)
    engine = initializeEngine()
    worldBody = WorldBody(this)
    oneShotListener<InitialChunksOfWorldLoadedEvent> {
      Main.logger().debug("World") { "Handling InitialChunksOfWorldLoadedEvent, will try to start world ticker now!" }
      check(!worldTicker.isStarted) { "World has already been started" }
      worldTicker.start()
      if (this is SinglePlayerWorld) {
        if (engine.getEntitiesFor(playerFamily).size() == 0) {
          Main.logger().debug("World", "Spawning new singleplayer player")
          engine.createSPPlayerEntity(this, spawn.x.toFloat(), spawn.y.toFloat(), 0f, 0f, "Player", null)
        } else {
          Main.logger().debug("World") { "Will not spawn a new player in as the world contains a singleplayer entity" }
        }
      }
      Main.logger().debug("World") { "World ticker started, sending WorldLoadedEvent" }
      dispatchEvent(WorldLoadedEvent(this))
    }
  }

  protected open fun initializeEngine(): Engine {
    val engine = PooledEngine()
    engine.addSystem(MaxVelocitySystem)
    engine.addSystem(ReadBox2DStateSystem)
    engine.addSystem(WriteBox2DStateSystem)
    engine.addSystem(InputSystem)
    engine.addSystem(ControlSystem)
    engine.addSystem(PhysicsSystem)
    engine.addSystem(UpdateBlockGridSystem)
    if (Main.isClient()) {
      engine.addSystem(FollowEntitySystem)
    }
    ensureUniquenessListener(engine)
    disposeEntitiesOnRemoval(engine)
    return engine
  }

  fun initialize() {
    if (Settings.loadWorldFromDisk) {
      val worldFolder = worldFolder
      if (!transientWorld && worldFolder != null && worldFolder.isDirectory &&
        !canWriteToWorld(uuid)
      ) {
        if (!Settings.ignoreWorldLock) {
          transientWorld = true
          Main.logger().warn("World", "World found is already in use. Initializing world as a transient.")
        } else {
          Main.logger().warn("World", "World found is already in use. However, ignore world lock is enabled therefore the world will be loaded normally. Here be corrupt worlds!")
        }
      }
      if (transientWorld || worldFolder == null) {
        Main.logger().log("No world save found")
      } else {
        Main.logger().log("Loading world from '${worldFolder.file().absolutePath}'")
        if (writeLockFile(uuid)) {
          val worldInfoFile = worldFolder.child(WorldLoader.WORLD_INFO_PATH)
          if (worldInfoFile.exists() && !worldInfoFile.isDirectory) {
            try {
              val protoWorld = ProtoWorld.World.parseFrom(worldInfoFile.readBytes())
              loadFromProtoWorld(protoWorld)
            } catch (e: InvalidProtocolBufferException) {
              e.printStackTrace()
            }
          }
        } else {
          Main.logger().error("Failed to write world lock file! Setting world to transient to be safe")
          transientWorld = true
        }
      }
    }
    render.update()
    Main.inst().scheduler.executeAsync {
      loadChunk(spawn.x.worldToChunk(), spawn.y.worldToChunk())
      for (location in render.chunkLocationsInView) {
        loadChunk(location.x, location.y)
      }
      Main.inst().scheduler.executeSync { dispatchEvent(InitialChunksOfWorldLoadedEvent(this)) }
    }
  }

  fun loadFromProtoWorld(protoWorld: WorldOrBuilder) {
    spawn = fromVector2i(protoWorld.spawn)
    worldTime.timeScale = protoWorld.timeScale
    worldTime.time = protoWorld.time
    synchronized(chunkColumns) {
      for (protoCC in protoWorld.chunkColumnsList) {
        val chunkColumn = fromProtobuf(this, protoCC)
        val chunkX = protoCC.chunkX
        chunkColumns.put(chunkX, chunkColumn)
      }
    }
  }

  fun save() {
    if (transientWorld) {
      return
    }
    val worldFolder = worldFolder ?: return
    chunksLock.writeLock().lock()
    try {
      val chunkLoader = chunkLoader
      if (chunkLoader is FullChunkLoader) {
        for (chunk in chunks.values()) {
          chunkLoader.save(chunk)
        }
      }

      //      for (Player player : players.values()) {
      //        saveServerPlayer(player);
      //      }
      val builder = toProtobuf()
      val worldInfoFile = worldFolder.child(WorldLoader.WORLD_INFO_PATH)
      if (worldInfoFile.exists()) {
        worldInfoFile.moveTo(worldFolder.child(WorldLoader.WORLD_INFO_PATH + ".old"))
      }
      worldInfoFile.writeBytes(builder.toByteArray(), false)
    } finally {
      chunksLock.writeLock().unlock()
    }
  }

  fun toProtobuf(): ProtoWorld.World {
    val builder = ProtoWorld.World.newBuilder()
    builder.name = name
    builder.seed = seed
    builder.time = worldTime.time
    builder.timeScale = worldTime.timeScale
    builder.spawn = spawn.toVector2i()
    builder.generator = ChunkGenerator.getGeneratorType(chunkLoader.generator)
    synchronized(chunkColumns) {
      for (chunkColumn in chunkColumns.values()) {
        builder.addChunkColumns(chunkColumn.toProtobuf())
      }
    }
    if (Main.isSingleplayer()) {
      val entities = engine.getEntitiesFor(playerFamily)
      if (entities != null && entities.size() > 0) {
        builder.player = entities.first().save(true)
      }
    }
    return builder.build()
  }

  val worldFolder: FileHandle?
    /**
     * @return The current folder of the world or `null` if no disk should be used
     */
    get() {
      if (transientWorld) {
        return null
      }
      if (worldFile == null) {
        worldFile = getWorldFolder(uuid)
      }
      return worldFile
    }

  fun getChunkColumn(chunkX: Int): ChunkColumn {
    synchronized(chunkColumns) {
      val column = chunkColumns[chunkX]
      if (column == null) {
        val newCol: ChunkColumn = ChunkColumnImpl(this, chunkX, null, null)
        chunkColumns.put(chunkX, newCol)
        return newCol
      }
      return column
    }
  }

  /**
   * @param features What kind of top block to return
   * @return The block (including Air!) at the given local x, if `null` the chunk failed to load.
   * @see no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag
   */
  fun getTopBlock(worldX: Int, features: Int): Block? {
    return getChunkColumn(worldX.worldToChunk())
      .topBlock(worldX.chunkOffset(), features)
  }

  /**
   * @param worldX   The block column to query for the worldX for
   * @param features What kind of top block to return
   * @return The worldY coordinate of the top block of the given worldX
   * @see no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag
   */
  fun getTopBlockWorldY(worldX: Int, features: Int): Int {
    return getChunkColumn(worldX.worldToChunk())
      .topBlockHeight(worldX.chunkOffset(), features)
  }

  fun getChunkFromWorld(worldX: Int, worldY: Int, load: Boolean): Chunk? {
    val chunkX = worldX.worldToChunk()
    val chunkY = worldY.worldToChunk()
    return getChunk(chunkX, chunkY, load)
  }

  fun updateChunk(chunk: Chunk) {
    Preconditions.checkState(chunk.isValid)
    chunksLock.writeLock().lock()
    val old: Chunk?
    old = try {
      chunks.put(chunk.compactLocation, chunk)
    } finally {
      chunksLock.writeLock().unlock()
    }
    old?.dispose()
  }

  fun getChunk(chunkLoc: Location): Chunk? {
    return getChunk(chunkLoc.toCompactLocation(), true)
  }

  fun getChunk(chunkX: Int, chunkY: Int, load: Boolean): Chunk? {
    return getChunk(compactLoc(chunkX, chunkY), load)
  }

  fun getChunk(chunkLoc: Long): Chunk? {
    return getChunk(chunkLoc, true)
  }

  fun getChunk(chunkLoc: Long, load: Boolean): Chunk? {
    // This is a long lock, it must appear to be an atomic operation though
    var readChunk: Chunk? = null
    var acquiredLock = false
    try {
      acquiredLock = chunksLock.readLock().tryLock(TRY_LOCK_CHUNKS_DURATION_MS, TimeUnit.MILLISECONDS)
      if (acquiredLock) {
        readChunk = chunks[chunkLoc]
      }
    } catch (ignore: InterruptedException) {
    } finally {
      if (acquiredLock) {
        chunksLock.readLock().unlock()
      }
    }
    if (!acquiredLock) {
      Main.logger().warn("World", "Failed to acquire chunks read lock in $TRY_LOCK_CHUNKS_DURATION_MS ms")
      return null
    }
    return if (readChunk == null || readChunk.isInvalid) {
      if (!load) {
        null
      } else {
        loadChunk(chunkLoc, true)
      }
    } else {
      readChunk
    }
  }

  /**
   * Load a chunk into memory, either from disk or generate the chunk from its position.
   *
   *
   * A chunk will not be loaded if there exists a valid chunk at the chunk position.
   *
   * @param chunkX The x-coordinate of the chunk to load (in Chunk coordinate-view)
   * @param chunkY The y-coordinate of the chunk to load (in Chunk coordinate-view)
   * @return The loaded chunk
   */
  fun loadChunk(chunkX: Int, chunkY: Int): Chunk? {
    return loadChunk(compactLoc(chunkX, chunkY), true)
  }

  /**
   * Load a chunk into memory, either from disk or generate the chunk from its position
   *
   * @param chunkLoc       The location of the chunk (in Chunk coordinate-view)
   * @param returnIfLoaded Whether to check if there is a loaded and valid chunk at the given
   * `chunkLocation` and thus not reload the chunk
   * @return The loaded chunk
   */
  fun loadChunk(chunkLoc: Long, returnIfLoaded: Boolean): Chunk? {
    if (worldTicker.isPaused) {
      Main.logger().debug("World", "Ticker paused will not load chunk")
      return null
    }
    var old: Chunk? = null
    chunksLock.writeLock().lock()
    return try {
      if (returnIfLoaded) {
        val current = chunks[chunkLoc]
        if (current != null) {
          if (current.isValid) {
            return current
          }
          if (current.isNotDisposed) {
            // If the current chunk is not valid, but not disposed either, so it should be loading
            // We don't want to load a new chunk when the current one is finishing its loading
            return null
          }
        }
      }

      //      Main.logger().log("Loading world chunk at " + CoordUtil.stringifyCompactLoc(chunkLoc)
      // + "\n" + DebugUtilsKt.stacktrace());
      val loadedChunk = chunkLoader.fetchChunk(chunkLoc)
      val chunk = loadedChunk.chunk
      if (chunk == null) {
        // If we failed to load the old chunk assume the loaded chunk (if any) is corrupt, out of
        // date, and the loading should be re-tried
        old = chunks.remove(chunkLoc)
        null
      } else {
        Preconditions.checkState(chunk.isValid)
        old = chunks.put(chunkLoc, chunk)
        dispatchEvent(ChunkLoadedEvent(chunk, loadedChunk.isNewlyGenerated))
        chunk
      }
    } finally {
      chunksLock.writeLock().unlock()
      old?.dispose()
    }
  }

  /**
   * Set a block at a given location
   *
   * @param worldX        The x coordinate from world view
   * @param worldY        The y coordinate from world view
   * @param material      The new material to at given location
   * @param updateTexture If the texture of the corresponding chunk should be updated
   * @see Chunk.setBlock
   */
  fun setBlock(worldX: Int, worldY: Int, material: Material?, updateTexture: Boolean = true, prioritize: Boolean = false, loadChunk: Boolean = true): Block? =
    actionOnBlock(worldX, worldY, loadChunk) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlock null
      chunk.setBlock(localX, localY, material, updateTexture, prioritize)
    }

  /**
   * Set a block at a given location
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @param block  The block at the given location
   * @param updateTexture If the texture of the corresponding chunk should be updated
   */
  fun setBlock(worldX: Int, worldY: Int, block: Block?, updateTexture: Boolean = true, prioritize: Boolean = false, loadChunk: Boolean = true): Block? =
    actionOnBlock(worldX, worldY, loadChunk) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlock null
      chunk.setBlock(localX, localY, block, updateTexture, prioritize)
    }

  fun setBlock(worldX: Int, worldY: Int, protoBlock: ProtoWorld.Block?, updateTexture: Boolean = true, prioritize: Boolean = false, sendUpdatePacket: Boolean = true): Block? =
    actionOnBlock(worldX, worldY, false) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlock null
      val block = Block.fromProto(this, chunk, localX, localY, protoBlock)
      chunk.setBlock(localX, localY, block, updateTexture, prioritize, sendUpdatePacket)
    }

  /**
   * Remove anything that is at the given location be it a [Block] or [MaterialEntity]
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @param updateTexture If the texture of the corresponding chunk should be updated
   */
  fun removeBlock(worldX: Int, worldY: Int, loadChunk: Boolean = true, updateTexture: Boolean = true, prioritize: Boolean = false, sendUpdatePacket: Boolean = true) =
    actionOnBlock(worldX, worldY, loadChunk) { localX, localY, chunk -> chunk?.removeBlock(localX, localY, updateTexture, prioritize, sendUpdatePacket) }

  /**
   * Check if a given location in the world is [Material.AIR] (or internally, doesn't exists)
   * this is faster than a standard `getBlock(worldX, worldY).getMaterial == Material.AIR` as
   * the [getRawBlock] method might createBlock and store a new air block
   * at the given location
   *
   * **note** this does not if there are entities at this location
   *
   * @param worldLoc The world location to check
   * @return If the block at the given location is air.
   */
  fun isAirBlock(compactWorldLoc: Long): Boolean = isAirBlock(compactWorldLoc.decompactLocX(), compactWorldLoc.decompactLocY())

  /**
   * Check if a given location in the world is [Material.AIR] (or internally, does not exist)
   * this is faster than a standard `getBlock(worldX, worldY).getMaterial == Material.AIR` as
   * the [getRawBlock] method might create a Block and store a new air
   * block at the given location.
   *
   * If the chunk at the given coordinates isn't loaded yet this method return `false` to prevent
   * teleportation and other actions that depend on an empty space.
   *
   * **note** this does not if there are entities at this location
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @return If the block at the given location is air.
   */
  fun isAirBlock(worldX: Int, worldY: Int, loadChunk: Boolean = true): Boolean = actionOnBlock(worldX, worldY, loadChunk) { localX, localY, nullableChunk ->
    val chunk = nullableChunk ?: return@actionOnBlock false
    chunk.getRawBlock(localX, localY).isAir()
  }

  /**
   * Check whether a block can be placed at the given location
   */
  fun canEntityPlaceBlock(blockX: Int, blockY: Int, entity: Entity): Boolean {
    if (entity.ignorePlaceableCheck) {
      return true
    }
    if (canPlaceBlock(blockX, blockY, false)) {
      return true
    }
    for (direction in Direction.CARDINAL) {
      if (canPlaceBlock(blockX + direction.dx, blockY + direction.dy, false)) {
        return true
      }
    }
    return false
  }

  private fun canPlaceBlock(worldX: Int, worldY: Int, loadChunk: Boolean = true): Boolean =
    actionOnBlock(worldX, worldY, loadChunk) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlock false
      val material = chunk.getRawBlock(localX, localY)?.material ?: Material.AIR
      material.adjacentPlaceable
    }

  private inline fun <R> actionOnBlock(
    worldX: Int,
    worldY: Int,
    loadChunk: Boolean = true,
    action: (localX: Int, localY: Int, chunk: Chunk?) -> R
  ): R {
    val chunkX: Int = worldX.worldToChunk()
    val chunkY: Int = worldY.worldToChunk()
    val chunk: Chunk? = getChunk(chunkX, chunkY, loadChunk)

    val localX: Int = worldX.chunkOffset()
    val localY: Int = worldY.chunkOffset()
    return action(localX, localY, chunk)
  }

  fun getBlocks(locs: GdxLongArray, loadChunk: Boolean = true): Iterable<Block> = getBlocks(locs.toArray(), loadChunk)
  fun getBlocks(locs: LongArray, loadChunk: Boolean = true): Iterable<Block> = locs.map { (blockX, blockY) -> getBlock(blockX, blockY, loadChunk) }.filterNotNullTo(mutableSetOf())
  fun getBlocks(locs: Iterable<Long>, loadChunk: Boolean = true): Iterable<Block> = locs.mapNotNullTo(mutableSetOf()) { (blockX, blockY) -> getBlock(blockX, blockY, loadChunk) }

  fun removeBlocks(blocks: Iterable<Block>, prioritize: Boolean = false) {
    val blockChunks = ObjectSet<Chunk>()
    for (block in blocks) {
      block.remove(updateTexture = false)
      blockChunks.add(block.chunk)
    }
    for (chunk in blockChunks) {
      chunk.updateTexture(prioritize)
    }
    // only update once all the correct blocks have been removed
    for (block in blocks) {
      updateBlocksAround(block.worldX, block.worldY)
    }
  }

  fun getEntities(worldX: Float, worldY: Float): Array<Entity> {
    val foundEntities = Array<Entity>(false, 4)
    for (entity in engine.getEntitiesFor(basicEntityFamily)) {
      val (x, y) = entity.getComponent(PositionComponent::class.java)
      val size = entity.getComponent(Box2DBodyComponent::class.java)
      if (Util.isBetween(
          MathUtils.floor((x - size.halfBox2dWidth)).toFloat(),
          worldX,
          MathUtils.ceil((x + size.halfBox2dWidth)).toFloat()
        ) &&
        //
        Util.isBetween(
          MathUtils.floor((y - size.halfBox2dHeight)).toFloat(),
          worldY,
          MathUtils.ceil((y + size.halfBox2dHeight)).toFloat()
        )
      ) {
        foundEntities.add(entity)
      }
    }
    return foundEntities
  }

  /**
   * Update blocks around the given location
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   */
  fun updateBlocksAround(worldX: Int, worldY: Int) {
    val blocksAABB = getBlocksAABBFromCenter(worldX + HALF_BLOCK_SIZE, worldY + HALF_BLOCK_SIZE, LIGHT_SOURCE_LOOK_BLOCKS, LIGHT_SOURCE_LOOK_BLOCKS, true, false, false)
    val chunks = LongMap<Chunk>()
    for (block in blocksAABB) {
      chunks.put(block.chunk.compactLocation, block.chunk)
      (block as? TickingBlock)?.enableTick()
    }
    for (chunk in chunks.values()) {
      chunk.dirty()
    }
  }

  /**
   * @param compactWorldLoc The coordinates from world view in a compact form
   * @param load            Load the chunk at the coordinates
   * @return The block at the given x and y
   */
  fun getRawBlock(compactWorldLoc: Long, load: Boolean): Block? {
    val worldY = compactWorldLoc.decompactLocY()
    val worldX = compactWorldLoc.decompactLocX()
    return getRawBlock(worldX, worldY, load)
  }

  /**
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @param load   Load the chunk at the coordinates
   * @return The block at the given x and y (or null if air block)
   */
  fun getRawBlock(worldX: Int, worldY: Int, load: Boolean): Block? {
    val chunkX = worldX.worldToChunk()
    val chunkY = worldY.worldToChunk()
    val localX = worldX - chunkX * Chunk.CHUNK_SIZE
    val localY = worldY - chunkY * Chunk.CHUNK_SIZE
    val chunk = getChunk(chunkX, chunkY, load) ?: return null
    return chunk.getRawBlock(localX, localY)
  }

  fun getBlockLight(worldX: Int, worldY: Int, loadChunk: Boolean = true): BlockLight? {
    val chunk = getChunkFromWorld(worldX, worldY, loadChunk) ?: return null
    return chunk.getBlockLight(worldX.chunkOffset(), worldY.chunkOffset())
  }

  /**
   * Note an air block will be created if the chunk is loaded and there is no other block at the
   * given location
   *
   * @param worldX    The x coordinate from world view
   * @param worldY    The y coordinate from world view
   * @param loadChunk
   * @return The block at the given x and y
   */
  fun getBlock(worldX: Int, worldY: Int, loadChunk: Boolean = true): Block? {
    val chunkX = worldX.worldToChunk()
    val chunkY = worldY.worldToChunk()
    val localX = worldX - chunkX * Chunk.CHUNK_SIZE
    val localY = worldY - chunkY * Chunk.CHUNK_SIZE
    val chunk = getChunk(chunkX, chunkY, loadChunk) ?: return null
    return chunk.getBlock(localX, localY)
  }

  /**
   * Use [isChunkLoaded] or [isChunkLoaded] if possible
   *
   * @param chunkLoc Chunk location in chunk coordinates
   * @return If the given chunk is loaded in memory
   */
  fun isChunkLoaded(chunkLoc: Location): Boolean {
    return isChunkLoaded(chunkLoc.toCompactLocation())
  }

  /**
   * @return If the given chunk is loaded in memory
   */
  fun isChunkLoaded(chunkX: Int, chunkY: Int): Boolean {
    return isChunkLoaded(compactLoc(chunkX, chunkY))
  }

  fun isChunkLoaded(compactedChunkLoc: Long): Boolean {
    val chunk: Chunk?
    chunksLock.readLock().lock()
    chunk = try {
      chunks[compactedChunkLoc]
    } finally {
      chunksLock.readLock().unlock()
    }
    return chunk != null && chunk.isNotDisposed
  }

  /**
   * Unload and save all chunks in this world.
   *
   *
   * Must be called on main thread!
   */
  fun reload() {
    Main.inst().scheduler.executeSync {
      // Reload on render thread to make sure it does not try to load chunks while we're
      // waiting
      val wasNotPaused = !worldTicker.isPaused
      if (wasNotPaused) {
        worldTicker.pause()
      }
      Main.inst().scheduler.waitForTasks()

      chunksLock.writeLock().lock()
      try {
        for (chunk in chunks.values()) {
          if (chunk != null && !unloadChunk(chunk, force = true, save = false)) {
            Main.logger().warn("Failed to unload chunk ${stringifyCompactLoc(chunk)}")
          }
        }
        val loadedChunks = chunks.size
        if (loadedChunks != 0) {
          Main.logger().warn("Failed to clear chunks during reload, there are $loadedChunks loaded chunks")
        }
      } finally {
        chunksLock.writeLock().unlock()
      }
      engine.removeAllEntities()
      postBox2dRunnable {
        val bodies = GdxArray<Body>(false, worldBody.box2dWorld.bodyCount)
        worldBody.box2dWorld.getBodies(bodies)
        if (!bodies.isEmpty) {
          Main.logger().error("BOX2D", "There existed dangling bodies after reload!")
        }
        for (body in bodies) {
          worldBody.destroyBody(body)
        }
      }
      initialize()
      if (wasNotPaused) {
        worldTicker.resume()
      }
      Main.logger().log("World", "World reloaded last save")
    }
  }

  val loadedChunks: Array<Chunk>
    /**
     * @return All currently loaded chunks
     */
    get() {
      chunksLock.readLock().lock()
      return try {
        val loadedChunks = Array<Chunk>(true, chunks.size, Chunk::class.java)
        for (chunk in chunks.values()) {
          if (chunk != null && chunk.isNotDisposed) {
            loadedChunks.add(chunk)
          }
        }
        loadedChunks
      } finally {
        chunksLock.readLock().unlock()
      }
    }

  fun getLoadedChunk(compactChunkLoc: Long): Chunk? {
    chunksLock.readLock().lock()
    return try {
      chunks[compactChunkLoc]
    } finally {
      chunksLock.readLock().unlock()
    }
  }

  /**
   * Unload the given chunks and save it to disk
   *
   * @param chunk The chunk to unload
   * @param force If the chunk will be forced to unload
   * @param save  If the chunk will be saved
   * @return If the unloading was successful
   */
  fun unloadChunk(chunk: Chunk?, force: Boolean = false, save: Boolean = true): Boolean {
    if (chunk != null && chunk.isNotDisposed && (force || chunk.isAllowedToUnload)) {
      if (chunk.world !== this) {
        Main.logger().warn("Tried to unload chunk from different world")
        return false
      }
      chunksLock.writeLock().lock()
      val removedChunk: Chunk? = try {
        val loader = chunkLoader
        if (save && loader is FullChunkLoader) {
          loader.save(chunk)
        }

        //                for (Entity entity : chunk.getEntities()) {
        //                  removeEntity(entity, CHUNK_UNLOADED);
        //                }
        chunks.remove(chunk.compactLocation)
      } finally {
        chunksLock.writeLock().unlock()
      }
      chunk.dispose()
      if (removedChunk != null && chunk !== removedChunk) {
        Main.logger().warn("Removed unloaded chunk ${stringifyCompactLoc(chunk)} was different from chunk in list of loaded chunks: ${stringifyCompactLoc(removedChunk)}")
        removedChunk.dispose()
      }
      return true
    }
    return false
  }

  fun containsEntity(uuid: String): Boolean {
    return getEntity(uuid) != null
  }

  fun getEntity(uuid: String): Entity? {
    val entitiesFor = engine.getEntitiesFor(basicEntityFamily)
    for (entity in entitiesFor) {
      if (entity.getComponent(IdComponent::class.java).id == uuid) {
        return entity
      }
    }
    return null
  }

  /**
   * Remove and disposes the given entity.
   *
   *
   * Even if the given entity is not a part of this world, it will be disposed
   *
   * @param entity The entity to remove
   * @throws IllegalArgumentException if the given entity is not part of this world
   */
  @JvmOverloads
  fun removeEntity(
    entity: Entity,
    reason: DespawnReason = DespawnReason.UNKNOWN_REASON
  ) {
    despawnEntity(entity, reason)
    engine.removeEntity(entity)
  }

  fun getPlayer(uuid: String): Entity? {
    for (entity in engine.getEntitiesFor(playerFamily)) {
      if (entity.id == uuid) {
        return entity
      }
    }
    return null
  }

  fun hasPlayer(uuid: String): Boolean {
    return getPlayer(uuid) != null
  }

  /**
   * @param worldX X center (center of each block
   * @param worldY Y center
   * @param radius Radius to be equal or less from center
   * @return Set of blocks within the given radius
   */
  fun getBlocksWithin(worldX: Int, worldY: Int, radius: Float): ObjectSet<Block> = getBlocksWithin(worldX + HALF_BLOCK_SIZE, worldY + HALF_BLOCK_SIZE, radius)
  fun getBlocksWithin(worldX: Float, worldY: Float, radius: Float): ObjectSet<Block> {
    Preconditions.checkArgument(radius >= 0, "Radius should be a non-negative number")
    val blocks = ObjectSet<Block>()
    for (compact in getLocationsAABB(worldX, worldY, radius, radius)) {
      val blockWorldX = compact.decompactLocX()
      val blockWorldY = compact.decompactLocY()
      val distance = abs(Vector2.dst2(worldX, worldY, blockWorldX + HALF_BLOCK_SIZE, blockWorldY + HALF_BLOCK_SIZE))
      if (distance < radius * radius) {
        val block = getBlock(blockWorldX, blockWorldY) ?: continue
        blocks.add(block)
      }
    }
    return blocks
  }

  /**
   * @param centerWorldX Center world X coordinate
   * @param centerWorldY Center world Y coordinate
   * @param width        How far to go on either side of the x-axis.
   * @param height       How far to go on either side of the y-axis.
   * @param raw          If non-existing blocks (i.e., air) should be created to be included
   * @param loadChunk    Whether to load the chunks the blocks exist in. If false no blocks from
   * unloaded chunks will be included
   * @return An Axis-Aligned Bounding Box of blocks
   */
  fun getBlocksAABBFromCenter(
    centerWorldX: Float,
    centerWorldY: Float,
    width: Float,
    height: Float,
    raw: Boolean,
    loadChunk: Boolean,
    includeAir: Boolean,
    cancel: () -> Boolean = { false },
    filter: (Block) -> Boolean = { true }
  ): Array<Block> {
    Preconditions.checkArgument(width >= 0, "Width must be >= 0, was $width")
    Preconditions.checkArgument(height >= 0, "Height must be >= 0, was $height")
    val worldX = centerWorldX - width
    val worldY = centerWorldY - height
    return getBlocksAABB(worldX, worldY, width * 2f, height * 2f, raw, loadChunk, includeAir, cancel, filter)
  }

  /**
   * @param worldX     Lower world X coordinate
   * @param worldY     Lower world Y coordinate
   * @param offsetX    Offset from the world X coordinate
   * @param offsetY    Offset from the world Y coordinate
   * @param raw        If non-existing blocks (i.e., air) should be created to be included
   * @param loadChunk  Whether to load the chunks the blocks exist in. If false no blocks from
   * unloaded chunks will be included
   * @param includeAir Whether air should be included
   * @param cancel     Allows for early termination of this method
   * @return An Axis-Aligned Bounding Box of blocks
   */
  fun getBlocksAABB(
    worldX: Float,
    worldY: Float,
    offsetX: Float,
    offsetY: Float,
    raw: Boolean,
    loadChunk: Boolean,
    includeAir: Boolean,
    cancel: () -> Boolean = { false },
    filter: (Block) -> Boolean = { true }
  ): GdxArray<Block> {
    val effectiveRaw: Boolean
    effectiveRaw = if (!raw && !includeAir) {
      Main.logger().warn("getBlocksAABB", "Will not include air AND air blocks will be created! (raw: false, includeAir: false)")
      true
    } else {
      raw
    }
    Preconditions.checkArgument(offsetX >= 0, "offsetX must be >= 0, was $offsetX")
    Preconditions.checkArgument(offsetY >= 0, "offsetY must be >= 0, was $offsetY")
    val capacity = MathUtils.floorPositive(abs(offsetX)) * MathUtils.floorPositive(abs(offsetY))
    val blocks = Array<Block>(true, capacity)
    var x = MathUtils.floor(worldX)
    val startY = MathUtils.floor(worldY)
    val maxX = worldX + offsetX
    val maxY = worldY + offsetY
    val chunks = LongMap<Chunk>()
    while (x <= maxX) {
      var y = startY
      while (y <= maxY) {
        if (cancel.invoke()) {
          return blocks
        }
        val chunkPos = compactLoc(x.worldToChunk(), y.worldToChunk())
        var chunk = chunks[chunkPos]
        if (chunk == null || chunk.isInvalid) {
          chunk = getChunk(chunkPos, loadChunk)
          if (chunk == null) {
            y++
            continue
          }
          chunks.put(chunkPos, chunk)
        }
        val localX = x.chunkOffset()
        val localY = y.chunkOffset()
        val b = if (effectiveRaw) chunk.getRawBlock(localX, localY) else chunk.getBlock(localX, localY)
        if (b == null) {
          y++
          continue
        }
        if ((includeAir || !b.isAir()) && filter.invoke(b)) {
          blocks.add(b)
        }
        y++
      }
      x++
    }
    return blocks
  }

  /**
   * @param worldX The x coordinate in world view
   * @param worldY The y coordinate in world view
   * @return The material at the given location
   */
  fun getMaterial(worldX: Int, worldY: Int): Material = getRawBlock(worldX, worldY, true)?.material ?: Material.AIR

  /**
   * Alias to `WorldBody#postBox2dRunnable`
   */
  fun postBox2dRunnable(runnable: () -> Unit) = worldBody.postBox2dRunnable(runnable)

  fun postWorldTickerRunnable(runnable: () -> Unit) = worldTicker.postRunnable(runnable)

  abstract val render: WorldRender

  override fun hashCode(): Int = uuid.hashCode()

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val world = other as World
    return uuid == world.uuid
  }

  override fun resize(width: Int, height: Int) {}
  override fun toString(): String {
    return "World{name='$name', uuid=$uuid}"
  }

  override fun dispose() {
    clear()
    worldTicker.dispose()
    synchronized(BOX2D_LOCK) { worldBody.dispose() }
    //    for (Entity entity : entities.values()) {
    //      entity.dispose();
    //    }
    //    entities.clear();
    //    players.clear();
    chunksLock.writeLock().lock()
    try {
      for (chunk in chunks.values()) {
        chunk.dispose()
      }
      chunks.clear()
    } finally {
      chunksLock.writeLock().unlock()
    }
    if (!transientWorld) {
      val worldFolder = worldFolder
      if (worldFolder != null && worldFolder.isDirectory) {
        if (!deleteLockFile(uuid)) {
          Main.logger().error("Failed to delete world lock file!")
        }
      }
    }
  }

  companion object {
    const val HALF_BLOCK_SIZE = 0.5f
    const val BLOCK_SIZE = 1f
    const val LIGHT_SOURCE_LOOK_BLOCKS = 10f
    const val TRY_LOCK_CHUNKS_DURATION_MS = 100L

    fun getLocationsAABB(worldX: Float, worldY: Float, offsetX: Float, offsetY: Float): LongArray {
      val capacity = MathUtils.floorPositive(abs(offsetX)) * MathUtils.floorPositive(abs(offsetY))
      val blocks = GdxLongArray(true, capacity)
      var x = MathUtils.floor(worldX - offsetX)
      val maxX = worldX + offsetX
      val maxY = worldY + offsetY
      while (x <= maxX) {
        var y = MathUtils.floor(worldY - offsetY)
        while (y <= maxY) {
          blocks.add(compactLoc(x, y))
          y++
        }
        x++
      }
      return blocks.toArray()
    }

    /**
     * @param worldX X center (center of each block
     * @param worldY Y center
     * @param radius Radius to be equal or less from center
     * @return Set of blocks within the given radius
     */
    fun getLocationsWithin(worldX: Int, worldY: Int, radius: Float): LongArray = getLocationsWithin(worldX + HALF_BLOCK_SIZE, worldY + HALF_BLOCK_SIZE, radius)
    fun getLocationsWithin(worldX: Float, worldY: Float, radius: Float): LongArray {
      Preconditions.checkArgument(radius >= 0, "Radius should be a non-negative number")
      val locs = GdxLongArray(false, (radius * radius * Math.PI).toInt() + 1)
      for (compact in getLocationsAABB(worldX, worldY, radius, radius)) {
        if (isBlockInsideRadius(worldX, worldY, compact.decompactLocX(), compact.decompactLocY(), radius)) {
          locs.add(compact)
        }
      }
      return locs.toArray()
    }
  }
}
