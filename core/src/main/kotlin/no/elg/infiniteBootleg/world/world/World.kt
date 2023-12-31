package no.elg.infiniteBootleg.world.world

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
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
import com.google.protobuf.TextFormat
import ktx.collections.GdxArray
import ktx.collections.GdxLongArray
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Resizable
import no.elg.infiniteBootleg.events.InitialChunksOfWorldLoadedEvent
import no.elg.infiniteBootleg.events.WorldLoadedEvent
import no.elg.infiniteBootleg.events.api.EventManager.clear
import no.elg.infiniteBootleg.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.events.api.EventManager.oneShotListener
import no.elg.infiniteBootleg.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.world
import no.elg.infiniteBootleg.server.despawnEntity
import no.elg.infiniteBootleg.util.ChunkColumnFeatureFlag
import no.elg.infiniteBootleg.util.ChunkCompactLoc
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCompactLoc
import no.elg.infiniteBootleg.util.WorldCompactLocArray
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.WorldCoordNumber
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.decompactLocX
import no.elg.infiniteBootleg.util.decompactLocY
import no.elg.infiniteBootleg.util.generateUUIDFromLong
import no.elg.infiniteBootleg.util.isAir
import no.elg.infiniteBootleg.util.isBlockInsideRadius
import no.elg.infiniteBootleg.util.isMarkerBlock
import no.elg.infiniteBootleg.util.isNotAir
import no.elg.infiniteBootleg.util.removeEntityAsync
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.util.toCompact
import no.elg.infiniteBootleg.util.toVector2i
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.util.worldXYtoChunkCompactLoc
import no.elg.infiniteBootleg.world.BOX2D_LOCK
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.WorldTime
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.world.blocks.Block.Companion.remove
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.blocks.BlockLight
import no.elg.infiniteBootleg.world.box2d.WorldBody
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkColumn
import no.elg.infiniteBootleg.world.chunks.ChunkColumnImpl
import no.elg.infiniteBootleg.world.chunks.ChunkColumnImpl.Companion.fromProtobuf
import no.elg.infiniteBootleg.world.chunks.ChunkColumnListeners
import no.elg.infiniteBootleg.world.ecs.ThreadSafeEngine
import no.elg.infiniteBootleg.world.ecs.basicRequiredEntityFamily
import no.elg.infiniteBootleg.world.ecs.basicRequiredEntityFamilyToSendToClient
import no.elg.infiniteBootleg.world.ecs.basicStandaloneEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.TransientEntityTag.Companion.isTransientEntity
import no.elg.infiniteBootleg.world.ecs.creation.createNewPlayer
import no.elg.infiniteBootleg.world.ecs.disposeEntitiesOnRemoval
import no.elg.infiniteBootleg.world.ecs.ensureUniquenessListener
import no.elg.infiniteBootleg.world.ecs.load
import no.elg.infiniteBootleg.world.ecs.localPlayerFamily
import no.elg.infiniteBootleg.world.ecs.playerFamily
import no.elg.infiniteBootleg.world.ecs.save
import no.elg.infiniteBootleg.world.ecs.system.DisposedChunkCheckSystem
import no.elg.infiniteBootleg.world.ecs.system.MagicSystem
import no.elg.infiniteBootleg.world.ecs.system.MaxVelocitySystem
import no.elg.infiniteBootleg.world.ecs.system.MineBlockSystem
import no.elg.infiniteBootleg.world.ecs.system.NoGravityInUnloadedChunksSystem
import no.elg.infiniteBootleg.world.ecs.system.OutOfBoundsSystem
import no.elg.infiniteBootleg.world.ecs.system.ReadBox2DStateSystem
import no.elg.infiniteBootleg.world.ecs.system.WriteBox2DStateSystem
import no.elg.infiniteBootleg.world.ecs.system.block.ExplosiveBlockSystem
import no.elg.infiniteBootleg.world.ecs.system.block.FallingBlockSystem
import no.elg.infiniteBootleg.world.ecs.system.block.LeavesDecaySystem
import no.elg.infiniteBootleg.world.ecs.system.block.UpdateGridBlockSystem
import no.elg.infiniteBootleg.world.ecs.system.client.ControlSystem
import no.elg.infiniteBootleg.world.ecs.system.client.FollowEntitySystem
import no.elg.infiniteBootleg.world.ecs.system.event.InputSystem
import no.elg.infiniteBootleg.world.ecs.system.event.PhysicsSystem
import no.elg.infiniteBootleg.world.ecs.system.magic.SpellRemovalSystem
import no.elg.infiniteBootleg.world.ecs.system.server.KickPlayerWithoutChannel
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
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.render.WorldRender
import no.elg.infiniteBootleg.world.ticker.WorldTicker
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.concurrent.GuardedBy
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.abs
import kotlin.system.measureTimeMillis

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

  /**
   * The entity engine of this world
   *
   * Adding and removing entities to the engine must be used on [ThreadType.PHYSICS] thread. Either within a System or within the [postBox2dRunnable] method.
   */
  val engine: ThreadSafeEngine

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
  private val chunkColumnListeners = ChunkColumnListeners()

  @Volatile
  private var worldFile: FileHandle? = null

  /**
   * @return The name of the world
   */
  val name: String

  /**
   * Spawn in world coordinates
   */
  var spawn: Long

  @JvmField
  val chunksLock: ReentrantReadWriteLock = ReentrantReadWriteLock()

  private var transientWorld = !Settings.loadWorldFromDisk || Main.isServerClient

  val tick get() = worldTicker.tickId

  private var saveTask: ScheduledFuture<*>? = null

  constructor(protoWorld: ProtoWorld.World) : this(generatorFromProto(protoWorld), protoWorld.seed, protoWorld.name)

  init {
    MathUtils.random.setSeed(seed)
    uuid = generateUUIDFromLong(seed).toString()
    name = worldName
    worldTicker = WorldTicker(this, false)
    worldTime = WorldTime(this)
    spawn = compactLoc(0, chunkLoader.generator.getHeight(0))
    engine = initializeEngine()
    worldBody = WorldBody(this)

    chunkColumnListeners.registerListeners()

    oneShotListener<InitialChunksOfWorldLoadedEvent> {
      Main.logger().debug("World") { "Handling InitialChunksOfWorldLoadedEvent, will try to start world ticker now!" }
      Main.inst().scheduler.executeSync {
        addSystems()
        if (Main.isSingleplayer) {
          // Add a delay to make sure the light is calculated
          Main.inst().scheduler.scheduleAsync(200L) {
            dispatchEvent(WorldLoadedEvent(this))
          }
        }
      }
    }
  }

  val playersEntities: ImmutableArray<Entity> by lazy { engine.getEntitiesFor(playerFamily) }
  val controlledPlayerEntities: ImmutableArray<Entity> by lazy { engine.getEntitiesFor(localPlayerFamily) }
  val standaloneEntities: ImmutableArray<Entity> by lazy { engine.getEntitiesFor(basicStandaloneEntityFamily) }
  val validEntities: ImmutableArray<Entity> by lazy { engine.getEntitiesFor(basicRequiredEntityFamily) }
  val validEntitiesToSendToClient: ImmutableArray<Entity> by lazy { engine.getEntitiesFor(basicRequiredEntityFamilyToSendToClient) }

  private fun initializeEngine(): ThreadSafeEngine {
    val engine = ThreadSafeEngine()
    ensureUniquenessListener(engine)
    disposeEntitiesOnRemoval(engine)
    addEntityListeners(engine)
    return engine
  }

  private fun addSystems() {
    engine.addSystem(MaxVelocitySystem)
    engine.addSystem(ReadBox2DStateSystem)
    engine.addSystem(WriteBox2DStateSystem)
    engine.addSystem(PhysicsSystem)
    engine.addSystem(UpdateGridBlockSystem)
    engine.addSystem(OutOfBoundsSystem)
    engine.addSystem(FallingBlockSystem)
    engine.addSystem(ExplosiveBlockSystem)
    engine.addSystem(LeavesDecaySystem)
    engine.addSystem(DisposedChunkCheckSystem)
    engine.addSystem(MineBlockSystem)
    engine.addSystem(NoGravityInUnloadedChunksSystem)
    engine.addSystem(FollowEntitySystem)
    engine.addSystem(InputSystem)
    engine.addSystem(ControlSystem)
    engine.addSystem(KickPlayerWithoutChannel)
    engine.addSystem(MagicSystem)
    engine.addSystem(SpellRemovalSystem)
    addSystems(engine)
  }

  protected open fun addEntityListeners(engine: Engine) {}
  protected open fun addSystems(engine: Engine) {}

  fun initialize() {
    var willDispatchChunksLoadedEvent = false
    val worldFolder = worldFolder
    if (!transientWorld && worldFolder != null && worldFolder.isDirectory && !canWriteToWorld(uuid)) {
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
            willDispatchChunksLoadedEvent = loadFromProtoWorld(protoWorld)
          } catch (e: InvalidProtocolBufferException) {
            e.printStackTrace()
          }
        }
      } else {
        Main.logger().error("Failed to write world lock file! Setting world to transient to be safe")
        transientWorld = true
      }
    }

    worldTicker.start()

    if (!willDispatchChunksLoadedEvent) {
      render.update()

      Main.inst().scheduler.executeAsync {
        if (Main.isSingleplayer) {
          this.createNewPlayer().thenApply {
            Main.logger().debug("World", "Spawned new singleplayer player")
          }
          Main.logger().debug("World", "Spawning new singleplayer player")
        }
        render.chunkLocationsInView.forEach(::loadChunk)
        dispatchEvent(InitialChunksOfWorldLoadedEvent(this))
      }
    }
    updateSavePeriod()
  }

  fun updateSavePeriod() {
    saveTask?.cancel(false)
    val savePeriodMs = Settings.savePeriodSeconds * 1000L
    saveTask = Main.inst().scheduler.schedulePeriodicSync(savePeriodMs, savePeriodMs, ::save)
  }

  /**
   * @return Whether this will call `dispatchEvent(InitialChunksOfWorldLoadedEvent(this))`
   */
  fun loadFromProtoWorld(protoWorld: ProtoWorld.World): Boolean {
    Main.logger().debug("PB World") { TextFormat.printer().shortDebugString(protoWorld) }
    spawn = protoWorld.spawn.toCompact()
    worldTime.timeScale = protoWorld.timeScale
    worldTime.time = protoWorld.time
    synchronized(chunkColumns) {
      for (protoCC in protoWorld.chunkColumnsList) {
        val chunkColumn = fromProtobuf(this, protoCC)
        val chunkX = protoCC.chunkX
        chunkColumns.put(chunkX, chunkColumn)
      }
    }

    return if (Main.isClient && protoWorld.hasPlayer()) {
      Main.inst().scheduler.executeAsync {
        val playerPosition = protoWorld.player.position
        (render as? ClientWorldRender)?.lookAt(playerPosition.x, playerPosition.y)
        render.chunkLocationsInView.forEach(::loadChunk)
        this.load(protoWorld.player).thenApply {
          it.isTransientEntity = true
          dispatchEvent(InitialChunksOfWorldLoadedEvent(this))
        }
      }
      true
    } else {
      false
    }
  }

  fun save() {
    if (transientWorld || Main.isNotAuthoritative) {
      return
    }
    val worldFolder = worldFolder ?: return
    Main.logger().debug("World") { "Starting to save world '$name'" }
    chunksLock.write {
      val chunkLoader = chunkLoader
      if (chunkLoader is FullChunkLoader) {
        for (chunk in chunks.values()) {
          chunkLoader.save(chunk)
        }
      }

      val builder = toProtobuf()
      val worldInfoFile = worldFolder.child(WorldLoader.WORLD_INFO_PATH)
      if (worldInfoFile.exists()) {
        worldInfoFile.moveTo(worldFolder.child(WorldLoader.WORLD_INFO_PATH + ".old"))
      }
      worldInfoFile.writeBytes(builder.toByteArray(), false)
    }
    Main.logger().debug("World") { "Done saving world '$name'" }
  }

  fun toProtobuf(): ProtoWorld.World =
    world {
      name = this@World.name
      seed = this@World.seed
      time = this@World.worldTime.time
      timeScale = this@World.worldTime.timeScale
      spawn = this@World.spawn.toVector2i()
      generator = ChunkGenerator.getGeneratorType(chunkLoader.generator)
      chunkColumns += synchronized(chunkColumns) { this@World.chunkColumns.map { it.value.toProtobuf() } }
      if (Main.isSingleplayer) {
        controlledPlayerEntities.firstOrNull()?.save(toAuthoritative = true, ignoreTransient = true)?.also {
          player = it
        }
      }
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

  fun getChunkColumn(chunkX: ChunkCoord): ChunkColumn {
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
  fun getTopBlock(worldX: WorldCoord, features: ChunkColumnFeatureFlag): Block? {
    return getChunkColumn(worldX.worldToChunk()).topBlock(worldX.chunkOffset(), features)
  }

  /**
   * @param worldX   The block column to query for the worldX for
   * @param features What kind of top block to return
   * @return The worldY coordinate of the top block of the given worldX
   * @see no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag
   */
  fun getTopBlockWorldY(worldX: WorldCoord, features: ChunkColumnFeatureFlag): WorldCoord {
    return getChunkColumn(worldX.worldToChunk()).topBlockHeight(worldX.chunkOffset(), features)
  }

  fun getChunkFromWorld(worldX: WorldCoord, worldY: WorldCoord, load: Boolean): Chunk? {
    val chunkX = worldX.worldToChunk()
    val chunkY = worldY.worldToChunk()
    return getChunk(chunkX, chunkY, load)
  }

  fun updateChunk(chunk: Chunk, newlyGenerated: Boolean) {
    Preconditions.checkState(chunk.isValid)
    val old: Chunk? = chunksLock.write {
      chunks.put(chunk.compactLocation, chunk)
    }
    if (old != null) {
      old.dispose()
    } else {
      dispatchEvent(ChunkLoadedEvent(chunk, newlyGenerated))
    }
  }

  fun getChunk(chunkX: ChunkCoord, chunkY: ChunkCoord, load: Boolean): Chunk? {
    return getChunk(compactLoc(chunkX, chunkY), load)
  }

  fun getChunk(chunkLoc: Long): Chunk? {
    return getChunk(chunkLoc, true)
  }

  fun getChunk(chunkLoc: Long, load: Boolean): Chunk? {
    // This is a long lock, it must appear to be an atomic operation though
    var readChunk: Chunk? = null
    var acquiredLock = false
    val acquireTime = measureTimeMillis {
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
    }
    if (!acquiredLock) {
      Main.logger().warn("World") { "Failed to acquire chunks read lock in $acquireTime ms (wanted to read ${stringifyCompactLoc(chunkLoc)})" }
      return null
    } else {
      if (acquireTime > TRY_LOCK_CHUNKS_DURATION_MS / 2f) {
        Main.logger().debug("World") { "Acquired chunks read lock in $acquireTime ms" }
      }
    }
    val finalReadChunk = readChunk
    return if (finalReadChunk == null || finalReadChunk.isInvalid) {
      if (!load) {
        null
      } else {
        loadChunk(chunkLoc, true)
      }
    } else {
      finalReadChunk
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
  fun loadChunk(chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk? {
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
  private fun loadChunk(chunkLoc: Long, returnIfLoaded: Boolean = true): Chunk? {
    if (worldTicker.isPaused) {
      Main.logger().debug("World", "Ticker paused will not load chunk")
      return null
    }
    return chunksLock.write {
      if (returnIfLoaded) {
        val current: Chunk? = chunks[chunkLoc]
        if (current != null) {
          if (current.isValid) {
            return@write current
          } else if (current.isNotDisposed) {
            // If the current chunk is not valid, but not disposed either, so it should be loading
            // We don't want to load a new chunk when the current one is finishing its loading
            return@write null
          }
        }
      }

      val loadedChunk = chunkLoader.fetchChunk(chunkLoc)
      val chunk = loadedChunk.chunk
      return@write if (chunk == null) {
        // If we failed to load the old chunk assume the loaded chunk (if any) is corrupt, out of
        // date, and the loading should be re-tried
        null
      } else {
        updateChunk(chunk, loadedChunk.isNewlyGenerated)
        chunk
      }
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
  fun setBlock(
    worldX: WorldCoord,
    worldY: WorldCoord,
    material: Material,
    updateTexture: Boolean = true,
    prioritize: Boolean = false,
    loadChunk: Boolean = true
  ): Block? =
    actionOnBlock(worldX, worldY, loadChunk) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlock null
      chunk.setBlock(localX, localY, material, updateTexture, prioritize)
    }

  /**
   * Set a block at a given location
   *
   * @param block  The block at the given location
   * @param updateTexture If the texture of the corresponding chunk should be updated
   */
  fun setBlock(block: Block, updateTexture: Boolean = true, prioritize: Boolean = false, loadChunk: Boolean = true): Block? =
    actionOnBlock(block.worldX, block.worldY, loadChunk) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlock null
      chunk.setBlock(localX, localY, block, updateTexture, prioritize)
    }

  fun setBlock(
    worldX: WorldCoord,
    worldY: WorldCoord,
    protoBlock: ProtoWorld.Block?,
    updateTexture: Boolean = true,
    prioritize: Boolean = false,
    sendUpdatePacket: Boolean = true
  ): Block? =
    actionOnBlock(worldX, worldY, false) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlock null
      val block = Block.fromProto(this, chunk, localX, localY, protoBlock)
      chunk.setBlock(localX, localY, block, updateTexture, prioritize, sendUpdatePacket)
    }

  /**
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @param updateTexture If the texture of the corresponding chunk should be updated
   */
  fun removeBlock(
    worldX: WorldCoord,
    worldY: WorldCoord,
    loadChunk: Boolean = true,
    updateTexture: Boolean = true,
    prioritize: Boolean = false,
    sendUpdatePacket: Boolean = true
  ) = actionOnBlock(worldX, worldY, loadChunk) { localX, localY, chunk -> chunk?.removeBlock(localX, localY, updateTexture, prioritize, sendUpdatePacket) }

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
  fun isAirBlock(compactWorldLoc: Long, loadChunk: Boolean = true, markerIsAir: Boolean = true): Boolean =
    isAirBlock(compactWorldLoc.decompactLocX(), compactWorldLoc.decompactLocY(), loadChunk, markerIsAir)

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
  fun isAirBlock(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean = true, markerIsAir: Boolean = true): Boolean =
    actionOnBlock(worldX, worldY, loadChunk) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlock false
      chunk.getRawBlock(localX, localY).isAir(markerIsAir)
    }

  /**
   * Check whether a block can be placed at the given location
   */
  fun canEntityPlaceBlock(blockX: WorldCoord, blockY: WorldCoord, entity: Entity): Boolean {
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

  private fun canPlaceBlock(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean = true): Boolean =
    actionOnBlock(worldX, worldY, loadChunk) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlock false
      val material = chunk.getRawBlock(localX, localY).materialOrAir()
      material.isCollidable && !isAnyEntityAt(worldX, worldY)
    }

  private inline fun <R> actionOnBlock(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean = true, action: (localX: LocalCoord, localY: LocalCoord, chunk: Chunk?) -> R): R {
    val chunkX: ChunkCoord = worldX.worldToChunk()
    val chunkY: ChunkCoord = worldY.worldToChunk()
    val chunk: Chunk? = getChunk(chunkX, chunkY, loadChunk)

    val localX: LocalCoord = worldX.chunkOffset()
    val localY: LocalCoord = worldY.chunkOffset()
    return action(localX, localY, chunk)
  }

  private inline fun actionOnBlocks(
    locations: Iterable<WorldCompactLoc>,
    loadChunk: Boolean = true,
    action: (localX: LocalCoord, localY: LocalCoord, chunk: Chunk?) -> Unit
  ): Iterable<Chunk> {
    val chunks = LongMap<Chunk>()
    for ((worldX, worldY) in locations) {
      val chunkLoc = worldXYtoChunkCompactLoc(worldX, worldY)
      val chunk: Chunk? = chunks.get(chunkLoc) ?: getChunk(chunkLoc, loadChunk)?.also { chunks.put(chunkLoc, it) }
      action(worldX.chunkOffset(), worldY.chunkOffset(), chunk)
    }
    return chunks.values()
  }

  fun getBlocks(locs: GdxLongArray, loadChunk: Boolean = true): Iterable<Block> = getBlocks(locs.toArray(), loadChunk)
  fun getBlocks(locs: LongArray, loadChunk: Boolean = true): Iterable<Block> = locs.map { (blockX, blockY) -> getBlock(blockX, blockY, loadChunk) }.filterNotNullTo(mutableSetOf())
  fun getBlocks(locs: Iterable<WorldCompactLoc>, loadChunk: Boolean = true): Iterable<Block> =
    locs.mapNotNullTo(mutableSetOf()) { (blockX, blockY) -> getBlock(blockX, blockY, loadChunk) }

  fun removeBlocks(blocks: Iterable<Block>, prioritize: Boolean = false) {
    val blockChunks = ObjectSet<Chunk>()
    for (block in blocks) {
      block.remove(updateTexture = false)
      blockChunks.add(block.chunk)
    }
    for (chunk in blockChunks) {
      chunk.updateTexture(prioritize)
    }
  }

  @JvmName("removeLocs")
  fun removeBlocks(blocks: Iterable<WorldCompactLoc>, prioritize: Boolean = false) {
    val blockChunks = actionOnBlocks(blocks) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlocks
      chunk.removeBlock(localX, localY, updateTexture = false)
    }
    for (chunk in blockChunks) {
      chunk.updateTexture(prioritize)
    }
  }

  fun getEntities(worldX: WorldCoord, worldY: WorldCoord): Array<Entity> {
    val foundEntities = Array<Entity>(false, 4)
    for (entity in standaloneEntities) {
      val (x, y) = entity.getComponent(PositionComponent::class.java)
      val size = entity.getComponent(Box2DBodyComponent::class.java)
      if (worldX in MathUtils.floor(x - size.halfBox2dWidth) until MathUtils.ceil(x + size.halfBox2dWidth) &&
        worldY in MathUtils.floor(y - size.halfBox2dHeight) until MathUtils.ceil(y + size.halfBox2dHeight)
      ) {
        foundEntities.add(entity)
      }
    }
    return foundEntities
  }

  fun isAnyEntityAt(worldX: WorldCoord, worldY: WorldCoord): Boolean {
    for (entity in standaloneEntities) {
      val (x, y) = entity.positionComponent
      val size = entity.box2d
      if (worldX in MathUtils.floor(x - size.halfBox2dWidth) until MathUtils.ceil(x + size.halfBox2dWidth) &&
        worldY in MathUtils.floor(y - size.halfBox2dHeight) until MathUtils.ceil(y + size.halfBox2dHeight)
      ) {
        return true
      }
    }
    return false
  }

  /**
   * @param compactWorldLoc The coordinates from world view in a compact form
   * @param load            Load the chunk at the coordinates
   * @return The block at the given x and y
   */
  fun getRawBlock(compactWorldLoc: Long, load: Boolean): Block? = getRawBlock(compactWorldLoc.decompactLocX(), compactWorldLoc.decompactLocY(), load)

  /**
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @param loadChunk   Load the chunk at the coordinates
   * @return The block at the given x and y (or null if air block)
   */
  fun getRawBlock(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean): Block? =
    actionOnBlock(worldX, worldY, loadChunk) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return null
      return chunk.getRawBlock(localX, localY)
    }

  fun getBlockLight(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean = true): BlockLight? {
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
  fun getBlock(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean = true): Block? =
    actionOnBlock(worldX, worldY, loadChunk) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return null
      return chunk.getBlock(localX, localY)
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
  fun getBlock(worldX: WorldCompactLoc, loadChunk: Boolean = true): Block? = getBlock(worldX.decompactLocX(), worldX.decompactLocY(), loadChunk)

  /**
   * @return If the given chunk is loaded in memory
   */
  fun isChunkLoaded(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = isChunkLoaded(compactLoc(chunkX, chunkY))

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

      chunksLock.write {
        for (chunk in chunks.values()) {
          if (chunk != null && !unloadChunk(chunk, force = true, save = false)) {
            Main.logger().warn("Failed to unload chunk ${stringifyCompactLoc(chunk)}")
          }
        }
        val loadedChunks = chunks.size
        if (loadedChunks != 0) {
          Main.logger().warn("Failed to clear chunks during reload, there are $loadedChunks loaded chunks")
        }
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

  /**
   * @return All currently loaded chunks
   */
  val loadedChunks: Array<Chunk>
    get() {
      return chunksLock.read {
        val loadedChunks = Array<Chunk>(true, chunks.size, Chunk::class.java)
        for (chunk in chunks.values()) {
          if (chunk != null && chunk.isNotDisposed) {
            loadedChunks.add(chunk)
          }
        }
        loadedChunks
      }
    }

  fun isChunkLoaded(compactedChunkLoc: ChunkCompactLoc): Boolean {
    val chunk: Chunk? = getChunk(compactedChunkLoc, load = false)
    return chunk != null && chunk.isNotDisposed
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

      val removedChunk: Chunk? = chunksLock.write {
        if (save && chunkLoader is FullChunkLoader) {
          chunkLoader.save(chunk)
        }
        chunks.remove(chunk.compactLocation)
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

  fun containsEntity(uuid: String): Boolean = getEntity(uuid) != null

  fun getEntity(uuid: String): Entity? {
    for (entity in standaloneEntities) {
      if (entity.getComponent(IdComponent::class.java).id == uuid) {
        return entity
      }
    }
    return null
  }

  /**
   * Remove and disposes the given entity.
   *
   * Even if the given entity is not a part of this world, it will be disposed
   *
   * @param entity The entity to remove
   * @throws IllegalArgumentException if the given entity is not part of this world
   */
  @JvmOverloads
  fun removeEntity(entity: Entity, reason: DespawnReason = DespawnReason.UNKNOWN_REASON) {
    despawnEntity(entity, reason)
    engine.removeEntityAsync(entity)
  }

  fun getPlayer(uuid: String): Entity? {
    for (entity in playersEntities) {
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
  fun getBlocksWithin(worldX: WorldCoord, worldY: WorldCoord, radius: Float): ObjectSet<Block> = getBlocksWithin(worldX + HALF_BLOCK_SIZE, worldY + HALF_BLOCK_SIZE, radius)
  fun getBlocksWithin(worldX: Float, worldY: Float, radius: Float): ObjectSet<Block> {
    Preconditions.checkArgument(radius >= 0, "Radius should be a non-negative number")
    val blocks = ObjectSet<Block>()
    for (compact in getLocationsAABBFromCenter(worldX, worldY, radius, radius)) {
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
    val effectiveRaw: Boolean = if (!raw && !includeAir) {
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
        if ((includeAir || b.isMarkerBlock() || b.isNotAir()) && filter.invoke(b)) {
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
  fun getMaterial(worldX: WorldCoord, worldY: WorldCoord, load: Boolean = true): Material = getRawBlock(worldX, worldY, load).materialOrAir()
  fun getMaterial(compactLoc: Long, load: Boolean = true): Material = getRawBlock(compactLoc, load).materialOrAir()

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
    saveTask?.cancel(false)
    saveTask = null
    worldTicker.dispose()
    synchronized(BOX2D_LOCK) { worldBody.dispose() }

    chunkColumnListeners.dispose()

    chunksLock.write {
      for (chunk in chunks.values()) {
        chunk.dispose()
      }
      chunks.clear()
    }
    if (Main.isAuthoritative && !transientWorld) {
      val worldFolder = worldFolder
      if (worldFolder != null && worldFolder.isDirectory) {
        if (!deleteLockFile(uuid)) {
          Main.logger().error("Failed to delete world lock file!")
        }
      }
    }
    engine.dispose()
  }

  companion object {
    const val BLOCK_SIZE = 1f
    const val HALF_BLOCK_SIZE = BLOCK_SIZE / 2f
    const val LIGHT_SOURCE_LOOK_BLOCKS = 10f
    const val LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA = LIGHT_SOURCE_LOOK_BLOCKS + 2f
    const val TRY_LOCK_CHUNKS_DURATION_MS = 20L

    fun getLocationsAABBFromCorner(cornerWorldX: WorldCoordNumber, cornerWorldY: WorldCoordNumber, offsetX: Float, offsetY: Float): WorldCompactLocArray =
      getLocationsAABBFromCenter(
        centerWorldX = cornerWorldX.toFloat() - offsetX / 2f,
        centerWorldY = cornerWorldY.toFloat() - offsetY / 2f,
        offsetX = offsetX / 2f,
        offsetY = offsetY / 2f
      )

    /**
     * @return A square of locations within the given offset from the center
     */
    fun getLocationsAABBFromCenter(centerWorldX: WorldCoordNumber, centerWorldY: WorldCoordNumber, offsetX: Float, offsetY: Float): WorldCompactLocArray {
      val capacity = MathUtils.floorPositive(abs(offsetX)) * MathUtils.floorPositive(abs(offsetY))
      val blocks = GdxLongArray(true, capacity)
      val centerWorldXF = centerWorldX.toFloat()
      val centerWorldYF = centerWorldY.toFloat()
      var x = MathUtils.floor(centerWorldXF - offsetX)
      val maxX = centerWorldXF + offsetX
      val maxY = centerWorldYF + offsetY
      while (x <= maxX) {
        var y = MathUtils.floor(centerWorldYF - offsetY)
        while (y <= maxY) {
          blocks.add(compactLoc(x, y))
          y++
        }
        x++
      }
      return blocks.toArray()
    }

    /**
     * @param worldX X center
     * @param worldY Y center
     * @param radius Radius to be equal or less from center
     * @return Set of blocks within the given radius
     */
    fun getLocationsWithin(worldX: WorldCoord, worldY: WorldCoord, radius: Float): LongArray = getLocationsWithin(worldX + HALF_BLOCK_SIZE, worldY + HALF_BLOCK_SIZE, radius)

    /**
     * @param worldX X center
     * @param worldY Y center
     * @param radius Radius to be equal or less from center
     * @return Set of blocks within the given radius
     */
    fun getLocationsWithin(worldX: Float, worldY: Float, radius: Float): LongArray {
      Preconditions.checkArgument(radius >= 0, "Radius should be a non-negative number")
      val locs = GdxLongArray(false, (radius * radius * Math.PI).toInt() + 1)
      for (compact in getLocationsAABBFromCenter(worldX, worldY, radius, radius)) {
        if (isBlockInsideRadius(worldX, worldY, compact.decompactLocX(), compact.decompactLocY(), radius)) {
          locs.add(compact)
        }
      }
      return locs.toArray()
    }
  }
}
