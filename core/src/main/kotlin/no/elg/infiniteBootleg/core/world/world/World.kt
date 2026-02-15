package no.elg.infiniteBootleg.core.world.world

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.LongMap
import com.badlogic.gdx.utils.ObjectSet
import com.google.errorprone.annotations.concurrent.GuardedBy
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.Hash.FAST_LOAD_FACTOR
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import ktx.async.interval
import ktx.collections.GdxArray
import ktx.collections.GdxLongArray
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.api.Resizable
import no.elg.infiniteBootleg.core.events.InitialChunksOfWorldLoadedEvent
import no.elg.infiniteBootleg.core.events.WorldLoadedEvent
import no.elg.infiniteBootleg.core.events.WorldSpawnUpdatedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.core.items.Item.Companion.fullName
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.CheckableDisposable
import no.elg.infiniteBootleg.core.util.ChunkColumnFeatureFlag
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.WorldCompactLoc
import no.elg.infiniteBootleg.core.util.WorldCompactLocArray
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.WorldCoordFloat
import no.elg.infiniteBootleg.core.util.WorldCoordNumber
import no.elg.infiniteBootleg.core.util.chunkOffset
import no.elg.infiniteBootleg.core.util.compactInt
import no.elg.infiniteBootleg.core.util.compactLong
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.decompactLocX
import no.elg.infiniteBootleg.core.util.decompactLocY
import no.elg.infiniteBootleg.core.util.isAir
import no.elg.infiniteBootleg.core.util.isBlockInsideRadius
import no.elg.infiniteBootleg.core.util.isMarkerBlock
import no.elg.infiniteBootleg.core.util.isNotAir
import no.elg.infiniteBootleg.core.util.launchOnAsyncSuspendable
import no.elg.infiniteBootleg.core.util.launchOnBox2d
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import no.elg.infiniteBootleg.core.util.partitionCount
import no.elg.infiniteBootleg.core.util.singleLinePrinter
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.util.toCompact
import no.elg.infiniteBootleg.core.util.toVector2i
import no.elg.infiniteBootleg.core.util.worldToBlock
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.util.worldXYtoChunkCompactLoc
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.WorldMetadata
import no.elg.infiniteBootleg.core.world.WorldTime
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.remove
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.core.world.blocks.BlockLight
import no.elg.infiniteBootleg.core.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.core.world.box2d.VoidPointerManager
import no.elg.infiniteBootleg.core.world.box2d.WorldBody
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.Chunk.Companion.invalid
import no.elg.infiniteBootleg.core.world.chunks.Chunk.Companion.valid
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumnsManager
import no.elg.infiniteBootleg.core.world.ecs.ThreadSafeEngine
import no.elg.infiniteBootleg.core.world.ecs.basicRequiredEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.basicRequiredEntityFamilyToSendToClient
import no.elg.infiniteBootleg.core.world.ecs.basicStandaloneEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.core.world.ecs.components.NameComponent.Companion.nameOrToString
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.core.world.ecs.disposeBox2dOnRemoval
import no.elg.infiniteBootleg.core.world.ecs.ensureUniquenessListener
import no.elg.infiniteBootleg.core.world.ecs.localPlayerFamily
import no.elg.infiniteBootleg.core.world.ecs.namedEntitiesFamily
import no.elg.infiniteBootleg.core.world.ecs.playerFamily
import no.elg.infiniteBootleg.core.world.ecs.save
import no.elg.infiniteBootleg.core.world.ecs.system.MaxVelocitySystem
import no.elg.infiniteBootleg.core.world.ecs.system.NoMovementInUnlockedChunksSystem
import no.elg.infiniteBootleg.core.world.ecs.system.OutOfBoundsSystem
import no.elg.infiniteBootleg.core.world.ecs.system.UpdateBox2DStateSystem
import no.elg.infiniteBootleg.core.world.ecs.system.ValidateGroundContactSystem
import no.elg.infiniteBootleg.core.world.ecs.system.block.BrokenBlockCleanupSystem
import no.elg.infiniteBootleg.core.world.ecs.system.block.ExplosiveBlockSystem
import no.elg.infiniteBootleg.core.world.ecs.system.block.FallingBlockSystem
import no.elg.infiniteBootleg.core.world.ecs.system.block.LeavesDecaySystem
import no.elg.infiniteBootleg.core.world.ecs.system.block.UpdateGridBlockSystem
import no.elg.infiniteBootleg.core.world.ecs.system.event.PhysicsEventSystem
import no.elg.infiniteBootleg.core.world.ecs.system.magic.SpellRemovalSystem
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.core.world.loader.WorldLoader
import no.elg.infiniteBootleg.core.world.loader.chunk.ChunkLoader
import no.elg.infiniteBootleg.core.world.loader.chunk.FullChunkLoader
import no.elg.infiniteBootleg.core.world.managers.container.AuthoritativeWorldContainerManager
import no.elg.infiniteBootleg.core.world.managers.container.WorldContainerManager
import no.elg.infiniteBootleg.core.world.render.WorldRender
import no.elg.infiniteBootleg.core.world.ticker.CommonWorldTicker
import no.elg.infiniteBootleg.core.world.ticker.WorldTicker
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.world
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.StampedLock
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

/**
 * Different kind of views
 *
 *  * Chunk view: One unit in chunk view is [Chunk.Companion.CHUNK_SIZE] times larger than a unit in
 * world view
 *  * World view: One unit in world view is [Block.Companion.BLOCK_TEXTURE_SIZE] times larger than a unit in
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
  seed: Long,
  /**
   * @return The name of the world
   */
  name: String,
  forceTransient: Boolean = false
) : CheckableDisposable,
  Resizable {

  constructor(protoWorld: ProtoWorld.World, forceTransient: Boolean = false) : this(
    WorldLoader.generatorFromProto(
      protoWorld
    ),
    protoWorld.seed,
    protoWorld.name,
    forceTransient
  )

  protected val metadata: WorldMetadata = WorldMetadata(
    name = name,
    seed = seed,
    spawn = compactInt(0, generator.getHeight(0)),
    isTransient = forceTransient || !Settings.loadWorldFromDisk || Main.isServerClient,
    box2dCoroutineDispatcher = object : CoroutineDispatcher() {
      override fun dispatch(context: CoroutineContext, block: Runnable) {
        postBox2dRunnable(block::run)
      }

      override fun isDispatchNeeded(context: CoroutineContext): Boolean = !ThreadType.PHYSICS.isThreadType()
    },
    worldTickCoroutineDispatcher = object : CoroutineDispatcher() {
      override fun dispatch(context: CoroutineContext, block: Runnable) {
        postWorldTickerRunnable(block::run)
      }

      override fun isDispatchNeeded(context: CoroutineContext): Boolean = !ThreadType.TICKER.isThreadType()
    }
  )

  abstract val render: WorldRender

  /**
   * must be accessed under [chunksLock]
   */
  @get:GuardedBy("chunksLock")
  @GuardedBy("chunksLock")
  private val chunks = LongMap<Chunk>()

  private val chunksLock: StampedLock = StampedLock()

  /**
   * The entity engine of this world
   *
   * Adding and removing entities to the engine must be used on [ThreadType.PHYSICS] thread. Either within a System or within the [postBox2dRunnable] method.
   */
  val engine: ThreadSafeEngine = initializeEngine()

  val worldBody: WorldBody = WorldBody(this)
  val worldTime: WorldTime = WorldTime(this)

  open val worldTicker: WorldTicker = CommonWorldTicker(this)
  open val worldContainerManager: WorldContainerManager = AuthoritativeWorldContainerManager(engine)
  abstract val chunkLoader: ChunkLoader

  val chunkColumnsManager: ChunkColumnsManager = ChunkColumnsManager(this)

  /**
   * Spawn in world coordinates
   */
  var spawn: WorldCompactLoc
    get() = metadata.spawn
    set(value) {
      assertNotDisposed()
      val old = metadata.spawn
      if (value != old) {
        EventManager.dispatchEvent(WorldSpawnUpdatedEvent(this, old, value))
        metadata.spawn = value
      }
    }

  /**
   * Whether this world is can be saved to disk
   */
  val isTransient: Boolean get() = metadata.isTransient

  /**
   * @return Unique identification of this world
   */
  val uuid: String get() = metadata.uuid
  val isLoaded: Boolean get() = metadata.isLoaded
  val name: String get() = metadata.name
  val seed: Long get() = metadata.seed
  val worldFolder: FileHandle? get() = metadata.worldFolder
  val chunkReads get() = metadata.chunkReads
  val chunkWrites get() = metadata.chunkWrites
  val box2dCoroutineDispatcher: CoroutineDispatcher get() = metadata.box2dCoroutineDispatcher
  val worldTickCoroutineDispatcher: CoroutineDispatcher get() = metadata.worldTickCoroutineDispatcher

  val hasDisposeBegun: Boolean get() = metadata.worldDisposeState > 0
  val isValid: Boolean get() = metadata.worldDisposeState == 0 && isLoaded
  final override val isDisposed: Boolean
    get() = metadata.worldDisposeState > 1

  val tick get() = worldTicker.tickId

  val playersEntities: ImmutableArray<Entity> get() = engine.getEntitiesFor(playerFamily)
  val controlledPlayerEntities: ImmutableArray<Entity> get() = engine.getEntitiesFor(localPlayerFamily)
  val standaloneEntities: ImmutableArray<Entity> get() = engine.getEntitiesFor(basicStandaloneEntityFamily)
  val validEntities: ImmutableArray<Entity> get() = engine.getEntitiesFor(basicRequiredEntityFamily)
  val validEntitiesToSendToClient: ImmutableArray<Entity>
    get() = engine.getEntitiesFor(basicRequiredEntityFamilyToSendToClient)
  val namedEntities: ImmutableArray<Entity> get() = engine.getEntitiesFor(namedEntitiesFamily)

  init {
    MathUtils.random.setSeed(seed)
    val world: World = this

    EventManager.oneShotListener<InitialChunksOfWorldLoadedEvent> {
      launchOnMainSuspendable {
        readChunks { readableChunks ->
          readableChunks.values().forEach(Chunk::updateAllBlockLights)
        }
        if (Main.isAuthoritative) {
          // Add a delay to make sure the light is calculated
          launchOnAsyncSuspendable {
            delay(200L)
            // World must be set to loaded before dispatching WorldLoadedEvent
            metadata.isLoaded = true
            EventManager.dispatchEvent(WorldLoadedEvent(world))
          }
        }
      }
    }
    EventManager.oneShotListener<WorldLoadedEvent> {
      logger.debug { "Handling WorldLoadedEvent, adding systems to the engine" }
      addSystems()
    }
  }

  protected open fun addEntityListeners(engine: Engine) {}
  protected open fun additionalSystems(): Set<EntitySystem> = setOf()

  /**
   * Allow for each system to be potentially configured after being added to the engine
   */
  protected open fun configureSystem(system: EntitySystem) {}

  private fun initializeEngine(): ThreadSafeEngine {
    val engine = ThreadSafeEngine()
    ensureUniquenessListener(engine)
    disposeBox2dOnRemoval(engine)
    addEntityListeners(engine)
    return engine
  }

  private fun addSystems() {
    engine.addSystem(MaxVelocitySystem)
    engine.addSystem(UpdateBox2DStateSystem)
    engine.addSystem(PhysicsEventSystem())
    engine.addSystem(UpdateGridBlockSystem)
    engine.addSystem(OutOfBoundsSystem)
    engine.addSystem(FallingBlockSystem())
    engine.addSystem(ExplosiveBlockSystem)
    engine.addSystem(LeavesDecaySystem())
    engine.addSystem(NoMovementInUnlockedChunksSystem)
    engine.addSystem(SpellRemovalSystem)
    engine.addSystem(ValidateGroundContactSystem)
    engine.addSystem(BrokenBlockCleanupSystem)
    additionalSystems().forEach(engine::addSystem)

    engine.systems.forEach(::configureSystem)
  }

  open fun initialize() {
    assertNotDisposed()
    val worldFolder = worldFolder
    if (!isTransient && worldFolder != null && worldFolder.isDirectory && !WorldLoader.canWriteToWorld(uuid)) {
      if (!Settings.ignoreWorldLock) {
        metadata.isTransient = true
        logger.warn { "World found is already in use. Initializing world as a transient." }
      } else {
        logger.warn { "World found is already in use. However, ignore world lock is enabled therefore the world will be loaded normally. Here be corrupt worlds!" }
      }
    }
    val worldInfoFile = worldFolder?.child(WorldLoader.WORLD_INFO_PATH)

    val willDispatchChunksLoadedEvent = if (worldFolder == null || worldInfoFile == null || !worldInfoFile.exists() || worldInfoFile.isDirectory) {
      logger.info { "No world save found" }
      loadNewWorld()
    } else if (isTransient) {
      logger.info { "World is transient, will not load from disk" }
      loadNewWorld()
    } else {
      logger.info { "Loading world from '${worldFolder.file().absolutePath}'" }
      if (WorldLoader.writeLockFile(uuid)) {
        try {
          val protoWorld = ProtoWorld.World.parseFrom(worldInfoFile.readBytes())
          loadFromProtoWorld(protoWorld)
        } catch (e: Exception) {
          logger.error(e) { "Failed to load world ${worldFolder.file().absolutePath}" }
          loadNewWorld()
        }
      } else {
        logger.error { "Failed to write world lock file! Setting world to transient to be safe" }
        metadata.isTransient = true
        loadNewWorld()
      }
    }

    worldTicker.start()

    if (!willDispatchChunksLoadedEvent) {
      render.update()
      launchOnBox2d {
        render.chunkLocationsInView.forEach(::loadChunk)
        EventManager.dispatchEvent(InitialChunksOfWorldLoadedEvent(this@World))
      }
    }
    updateSavePeriod()
  }

  fun createChunkIterator(): LongMap.Entries<Chunk> = LongMap.Entries(chunks)

  fun readChunks(onFailure: (() -> Unit)? = null, action: (readableChunks: LongMap<Chunk>) -> Unit): Unit = readChunks<Unit>(onFailure ?: { }, action)

  fun <R> readChunks(onFailure: () -> R, action: (readableChunks: LongMap<Chunk>) -> R): R {
    contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
    assertNotDisposed()
    chunkReads.incrementAndGet()
    val stamp = chunksLock.tryReadLock(LONG_WAIT_DURATION_CHUNK_LOCK_MS, TimeUnit.MILLISECONDS)
    return if (stamp != 0L) {
      try {
        action(chunks)
      } finally {
        chunksLock.unlock(stamp)
      }
    } else {
      logger.warn { "Failed to acquire chunks read lock" }
      onFailure.invoke()
    }
  }

  private fun <R> writeChunks(onFailure: (() -> R?)? = null, action: (writableChunks: LongMap<Chunk>) -> R): R? {
    contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
    assertNotDisposed()
    chunkWrites.incrementAndGet()
    val stamp = chunksLock.tryWriteLock(1, TimeUnit.SECONDS)
    return if (stamp != 0L) {
      try {
        action(chunks)
      } finally {
        chunksLock.unlock(stamp)
      }
    } else {
      logger.warn { "Failed to acquire chunks write lock" }
      onFailure?.invoke()
    }
  }

  fun <R> readChunks(timeoutMillis: Long, onFailure: (() -> R?)? = null, onSuccess: (readableChunks: LongMap<Chunk>) -> R?): R? {
    contract { callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE) }
    assertNotDisposed()
    chunkReads.incrementAndGet()
    // This is a long lock, it must appear to be an atomic operation though
    var result: R?
    var acquiredLock = 0L
    val acquireTime = measureTimeMillis {
      acquiredLock = chunksLock.tryReadLock(timeoutMillis, TimeUnit.MILLISECONDS)
    }
    if (acquiredLock != 0L) {
      try {
        result = onSuccess(chunks)
      } finally {
        chunksLock.unlock(acquiredLock)
      }
      if (acquireTime >= timeoutMillis) {
        logger.debug { "Acquired read lock in $acquireTime ms. Max lock time is $timeoutMillis ms" }
      } else if (acquireTime > timeoutMillis / 2L) {
        logger.trace { "Acquired chunk read lock in $acquireTime ms out of max $timeoutMillis ms" }
      }
    } else {
      logger.warn { "Failed to acquire chunks read lock in $acquireTime ms" }
      result = onFailure?.invoke()
    }
    return result
  }

  fun updateSavePeriod() {
    metadata.saveTask = interval(Settings.savePeriodSeconds, Settings.savePeriodSeconds, task = ::save)
  }

  /**
   * Called when loading a new world where a [ProtoWorld.World] is unavailable
   *
   * @return Whether this will call `dispatchEvent(InitialChunksOfWorldLoadedEvent(this))`
   *
   * @see loadFromProtoWorld
   */
  open fun loadNewWorld(): Boolean = false

  /**
   * Called when loading an existing world.
   *
   * @return Whether this will call `dispatchEvent(InitialChunksOfWorldLoadedEvent(this))`
   *
   * @see loadNewWorld
   */
  open fun loadFromProtoWorld(protoWorld: ProtoWorld.World): Boolean {
    assertNotDisposed()
    if (Settings.debug && Settings.logPersistence) {
      logger.debug { singleLinePrinter.printToString(protoWorld) }
    }
    spawn = protoWorld.spawn.toCompact()
    worldTime.timeScale = protoWorld.timeScale
    worldTime.time = protoWorld.time
    chunkColumnsManager.fromProtobuf(protoWorld.chunkColumnsList)

    return false
  }

  fun save() = save(false)

  /**
   * Allow for saving when disposeing
   */
  protected open fun save(overrideDispose: Boolean) {
    if ((!overrideDispose && hasDisposeBegun) || isTransient) {
      return
    }
    val worldFolder = worldFolder ?: return
    logger.debug { "Saving $this" }

    readChunks { readableChunks ->
      val chunkLoader = chunkLoader
      if (chunkLoader is FullChunkLoader) {
        for (chunk in readableChunks.values()) {
          chunkLoader.save(chunk)
        }
      }
    }

    val builder = toProtobuf()
    val worldInfoFile = worldFolder.child(WorldLoader.WORLD_INFO_PATH)
    if (worldInfoFile.exists()) {
      worldInfoFile.moveTo(worldFolder.child(WorldLoader.WORLD_INFO_PATH + ".old"))
    }
    worldInfoFile.writeBytes(builder.toByteArray(), false)
  }

  fun toProtobuf(): ProtoWorld.World =
    world {
      name = metadata.name
      seed = metadata.seed
      time = this@World.worldTime.time
      timeScale = this@World.worldTime.timeScale
      spawn = this@World.spawn.toVector2i()
      generator = ChunkGenerator.getGeneratorType(chunkLoader.generator)
      chunkColumns += chunkColumnsManager.toProtobuf()
      if (Main.isSingleplayer) {
        controlledPlayerEntities.firstOrNull()?.save(toAuthoritative = true, ignoreTransient = true)?.also {
          player = it
        }
      }
    }

  fun getChunkColumn(chunkX: ChunkCoord): ChunkColumn {
    assertNotDisposed()
    return chunkColumnsManager.getChunkColumn(chunkX)
  }

  /**
   * @param features What kind of top block to return
   * @return The block (including Air!) at the given local x, if `null` the chunk failed to load.
   * @see no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag
   */
  fun getTopBlock(worldX: WorldCoord, features: ChunkColumnFeatureFlag): Block? = getChunkColumn(worldX.worldToChunk()).topBlock(worldX.chunkOffset(), features)

  /**
   * @param worldX   The block column to query for the worldX for
   * @param features What kind of top block to return
   * @return The worldY coordinate of the top block of the given worldX
   * @see no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag
   */
  fun getTopBlockWorldY(worldX: WorldCoord, features: ChunkColumnFeatureFlag): WorldCoord = getChunkColumn(worldX.worldToChunk()).topBlockHeight(worldX.chunkOffset(), features)

  fun getChunkFromWorld(worldX: WorldCoord, worldY: WorldCoord, load: Boolean): Chunk? {
    val chunkX = worldX.worldToChunk()
    val chunkY = worldY.worldToChunk()
    return getChunk(chunkX, chunkY, load)
  }

  /**
   * @param chunk The chunk to update to
   * @param newlyGenerated Whether the chunk is newly generated, used for chunk dispatch events
   * @param expectedChunk The expected chunk to be updated, if the chunk is not the expected chunk it will not be updated. If the current chunk is `null` it will be updated regardless
   *
   * @return The currently active chunk, might be different from the argument [chunk]
   */
  fun updateChunk(chunk: Chunk, expectedChunk: Chunk? = null, newlyGenerated: Boolean): Chunk {
    assertNotDisposed()
    require(chunk.isValid) { "Chunk must be valid to be updated" }
    var chunkToDispose: Chunk? = null
    val toReturn: Chunk? = writeChunks { writableChunks ->
      val current: Chunk? = writableChunks[chunk.compactLocation]
      return@writeChunks if (current != null && expectedChunk != null && current !== expectedChunk) {
        logger.warn { "Unexpected chunk when updating chunk, will not update chunk. Given chunk will be disposed" }
        chunkToDispose = chunk
        current
      } else {
        val old = writableChunks.put(chunk.compactLocation, chunk)
        chunkToDispose = old
        chunk
      }
    }

    if (toReturn == null) {
      logger.warn { "Failed to write chunk" }
      throw IllegalStateException("Failed to write chunk")
    }
    chunkToDispose?.dispose()

    if (chunkToDispose === chunk) {
      logger.warn { "Unexpected chunk when updating chunk, will not write chunk. Given chunk will be disposed" }
    } else if (chunkToDispose != null) {
      logger.trace { "Swapping chunk at ${toReturn.compactLocation}" }
    } else {
      // No old chunk to dispose, so this is a new chunk
      EventManager.dispatchEventAsync(ChunkLoadedEvent(toReturn, newlyGenerated))
    }
    return toReturn
  }

  fun getChunk(chunkX: ChunkCoord, chunkY: ChunkCoord, load: Boolean): Chunk? = getChunk(compactInt(chunkX, chunkY), load)

  private class ThreadLocalChunkCache {
    var lastLoc: Long = Long.MIN_VALUE
    var lastChunk: Chunk? = null
    val chunks = Long2ObjectOpenHashMap<WeakReference<Chunk>>(32)
  }

  private val threadLocalChunkCache = ThreadLocal.withInitial {
    CHUNK_THREAD_LOCAL.incrementAndGet()
    ThreadLocalChunkCache()
  }

  /**
   * Find a valid chunk and optionally load it if it is not valid/not loaded
   *
   * @implSpec This method is heavily performance optimize. Check for performance regressions when modifying with 'sandtest'
   *
   * @return A valid chunk
   */
  fun getChunk(chunkLoc: Long, load: Boolean = true): Chunk? {
    assertNotDisposed()
    val cache = threadLocalChunkCache.get()

    // Fast path: check last accessed chunk (same-chunk spatial locality)
    if (cache.lastLoc == chunkLoc) {
      val lastChunk = cache.lastChunk
      if (lastChunk != null && lastChunk.isValid) {
        return lastChunk
      }
    }

    // Medium path: check thread-local map
    val localChunks = cache.chunks
    val localChunk: Chunk? = localChunks.get(chunkLoc)?.get()
    if (localChunk != null) {
      if (localChunk.isValid) {
        cache.lastLoc = chunkLoc
        cache.lastChunk = localChunk
        return localChunk
      } else {
        localChunks.remove(chunkLoc)
      }
    }

    // Slow path: read from main store
    // Give a larger timeout when disposing as it might take longer to acquire the lock as all chunks are being saved
    val timeoutMillis = if (hasDisposeBegun) LONG_WAIT_DURATION_CHUNK_LOCK_MS else TRY_LOCK_CHUNKS_DURATION_MS
    // This is a long lock, it must appear to be an atomic operation though
    val readChunk = readChunks(timeoutMillis) { readableChunks -> readableChunks[chunkLoc] }
    val finalChunk = if (readChunk == null || readChunk.isDisposed) {
      if (!load) {
        null
      } else {
        loadChunk(chunkLoc)
      }
    } else {
      readChunk
    }
    return if (finalChunk.valid()) {
      CHUNK_ADDED_THREAD_LOCAL.incrementAndGet()
      localChunks.put(chunkLoc, WeakReference(finalChunk))
      cache.lastLoc = chunkLoc
      cache.lastChunk = finalChunk
      finalChunk
    } else {
      null
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
  fun loadChunk(chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk? = loadChunk(compactInt(chunkX, chunkY))

  /**
   * Load a chunk into memory, either from disk or generate the chunk from its position
   *
   * @param chunkLoc       The location of the chunk (in Chunk coordinate-view)
   *
   * @return The loaded chunk
   */
  protected fun loadChunk(chunkLoc: ChunkCompactLoc): Chunk? {
    assertNotDisposed()
    if (worldTicker.isPaused) {
      return null
    }
    val current = readChunks<Chunk?>({ null }) { readableChunks -> readableChunks[chunkLoc] }
    if (current != null) {
      if (current.isValid) {
        return current
      } else if (current.isNotDisposed) {
        // If the current chunk is not valid, but not disposed either, so it should be loading
        // We don't want to load a new chunk when the current one is finishing its loading
        return null
      }
    }

    val loadedChunk = chunkLoader.fetchChunk(chunkLoc)
    val chunk = loadedChunk.chunk
    return if (chunk == null) {
      // If we failed to load the old chunk assume the loaded chunk (if any) is corrupt, out of
      // date, and the loading should be re-tried
      null
    } else {
      updateChunk(chunk, current, loadedChunk.isNewlyGenerated)
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
   * Check if a given location in the world is [Material.Air] (or internally, doesn't exists)
   * this is faster than a standard `getBlock(worldX, worldY).getMaterial == Material.Air` as
   * the [getRawBlock] method might createBlock and store a new air block
   * at the given location
   *
   * **note** this does not if there are entities at this location
   *
   * @param worldLoc The world location to check
   * @return If the block at the given location is air.
   */
  fun isAirBlock(compactWorldLoc: Long, loadChunk: Boolean = true, markerIsAir: Boolean? = null): Boolean =
    isAirBlock(compactWorldLoc.decompactLocX(), compactWorldLoc.decompactLocY(), loadChunk, markerIsAir)

  /**
   * Check if a given location in the world is [Material.Air] (or internally, does not exist)
   * this is faster than a standard `getBlock(worldX, worldY).getMaterial == Material.Air` as
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
  fun isAirBlock(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean = true, markerIsAir: Boolean? = null): Boolean =
    actionOnBlock(worldX, worldY, loadChunk) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlock false
      chunk.getRawBlock(localX, localY).isAir(markerIsAir)
    }

  /**
   * Check if a given location in the world is [Material.Air] (or internally, does not exist)
   * this is faster than a standard `getBlock(worldX, worldY).getMaterial == Material.Air` as
   * the [getBlock] method might create a Block and store a new air
   * block at the given location.
   *
   * If the chunk at the given coordinates isn't loaded yet this method return `true`
   *
   * **note** this does not if there are entities at this location
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @return If the block at the given location is not air.
   */
  fun isNotAirBlock(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean = true, markerIsAir: Boolean? = null): Boolean =
    actionOnBlock(worldX, worldY, loadChunk) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlock true
      chunk.getRawBlock(localX, localY).isNotAir(markerIsAir)
    }

  /**
   * Check whether a block can be placed at the given location
   */
  fun canEntityPlaceBlock(blockX: WorldCoord, blockY: WorldCoord, entity: Entity): Boolean {
    if (entity.ignorePlaceableCheck) {
      return true
    }
    if (canPlaceBlock(blockX, blockY)) {
      return true
    }
    for (direction in Direction.CARDINAL) {
      if (canPlaceBlock(blockX + direction.dx, blockY + direction.dy)) {
        return true
      }
    }
    return false
  }

  private fun canPlaceBlock(worldX: WorldCoord, worldY: WorldCoord): Boolean =
    actionOnBlock(worldX, worldY, false) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlock false
      val rawBlock = chunk.getRawBlock(localX, localY)
      rawBlock !is EntityMarkerBlock && rawBlock.materialOrAir().isCollidable && !isAnyEntityAt(worldX, worldY)
    }

  /**
   * Perform an action on a chunk, loading it if necessary
   *
   * @param maybeChunk A chunk that might be the correct chunk, used for optimization only
   * @param worldX     The world x coordinate to perform the action at
   * @param worldY     The world y coordinate to perform the action at
   * @param loadChunk  Whether to load the chunk if it is not already loaded
   * @param action     The action to perform on the chunk
   *
   * @return The result of the action
   */
  fun <R> actionOnChunk(
    maybeChunk: Chunk,
    worldX: WorldCoord,
    worldY: WorldCoord,
    loadChunk: Boolean = true,
    action: (actionChunk: Chunk?) -> R
  ): R {
    assertNotDisposed()
    val chunkX: ChunkCoord = worldX.worldToChunk()
    val chunkY: ChunkCoord = worldY.worldToChunk()
    val chunk: Chunk? = if (maybeChunk.isValid && compactInt(chunkX, chunkY) == maybeChunk.compactLocation) {
      maybeChunk
    } else {
      getChunk(chunkX, chunkY, loadChunk)
    }

    return action(chunk)
  }

  inline fun <R> actionOnBlock(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean = true, action: (localX: LocalCoord, localY: LocalCoord, chunk: Chunk?) -> R): R {
    assertNotDisposed()
    val chunkX: ChunkCoord = worldX.worldToChunk()
    val chunkY: ChunkCoord = worldY.worldToChunk()
    val chunk: Chunk? = getChunk(chunkX, chunkY, loadChunk)

    val localX: LocalCoord = worldX.chunkOffset()
    val localY: LocalCoord = worldY.chunkOffset()
    return action(localX, localY, chunk)
  }

  inline fun actionOnBlocks(
    locations: Iterable<WorldCompactLoc>,
    loadChunk: Boolean = true,
    action: (localX: LocalCoord, localY: LocalCoord, chunk: Chunk?) -> Unit
  ): Iterable<Chunk> {
    assertNotDisposed()
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

  fun removeBlocks(blocks: Iterable<Block>, prioritize: Boolean = false): Set<Block> {
    assertNotDisposed()
    val blockChunks = ObjectOpenHashSet<Chunk>()
    val removed = ObjectOpenHashSet<Block>()
    for (block in blocks) {
      val chunk = block.remove() ?: continue
      removed += block
      blockChunks += chunk
    }
    for (chunk in blockChunks) {
      chunk.dirty(prioritize)
    }
    return removed
  }

  /**
   * @return The blocks actually removed
   */
  @JvmName("removeLocs")
  fun removeBlocks(blocks: Iterable<WorldCompactLoc>, giveTo: Entity? = null, prioritize: Boolean = false): Set<Block> {
    assertNotDisposed()
    val toRemove = ObjectOpenHashSet<Block>()
    actionOnBlocks(blocks) { localX, localY, nullableChunk ->
      val chunk = nullableChunk ?: return@actionOnBlocks
      chunk.getRawBlock(localX, localY)?.also { oldBlock -> toRemove += oldBlock }
    }

    // Make sure we only remove references to each entity once (i.e. for multi-block entities)
    val seenEntity = ObjectOpenHashSet<String>()
    toRemove.removeIf {
      val entity = it.entity
      entity != null && !seenEntity.add(entity.id)
    }

    removeBlocks(toRemove, prioritize)

    giveBlocks(toRemove, giveTo)
    return toRemove
  }

  private fun giveBlocks(blocks: Collection<Block>, giveTo: Entity?) {
    assertNotDisposed()
    if (blocks.isNotEmpty()) {
      val container = giveTo?.containerOrNull
      if (container != null) {
        val items = blocks.partitionCount { it.material }.map { (mat, count) -> mat.toItem(stock = count.toUInt()) }
        logger.trace { "Will give ${items.map { it.fullName }} to ${giveTo.nameOrToString}" }
        val notAdded = container.add(items)
        if (notAdded.isNotEmpty()) {
          logger.debug { "Failed to add items when removing block, not enough space for $notAdded" }
        }
      } else if (giveTo != null) {
        logger.debug { "Cannot give items to ${giveTo.nameOrToString}, failed to find a container" }
      }
    }
  }

  fun getEntities(worldX: WorldCoord, worldY: WorldCoord): GdxArray<Entity> {
    assertNotDisposed()
    val foundEntities = GdxArray<Entity>(false, 0)
    for (entity in standaloneEntities) {
      val (x, y) = entity.positionComponent
      val size = entity.box2d
      if (worldX in MathUtils.floor(x - size.halfBox2dWidth) until MathUtils.ceil(x + size.halfBox2dWidth) &&
        worldY in MathUtils.floor(y - size.halfBox2dHeight) until MathUtils.ceil(y + size.halfBox2dHeight)
      ) {
        foundEntities.add(entity)
      }
    }
    return foundEntities
  }

  fun isAnyEntityAt(worldX: WorldCoord, worldY: WorldCoord): Boolean {
    assertNotDisposed()
    for (entity in standaloneEntities) {
      val (x, y) = entity.positionComponent
      val box2d = entity.box2d
      if (worldX in MathUtils.floor(x - box2d.halfBox2dWidth) until MathUtils.ceil(x + box2d.halfBox2dWidth) &&
        worldY in MathUtils.floor(y - box2d.halfBox2dHeight) until MathUtils.ceil(y + box2d.halfBox2dHeight)
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
  fun isChunkLoaded(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = isChunkLoaded(compactInt(chunkX, chunkY))

  /**
   * @return Copy of currently loaded chunks
   */
  val loadedChunks: GdxArray<Chunk>
    get() {
      return readChunks<GdxArray<Chunk>>({ GdxArray<Chunk>(0) }) { readableChunks ->
        val loadedChunks = GdxArray<Chunk>(true, readableChunks.size)
        for (chunk in readableChunks.values()) {
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
    if (isDisposed) {
      return false
    }
    if (chunk != null && (force || chunk.allowedToUnload)) {
      if (chunk.world !== this) {
        logger.warn { "Tried to unload chunk from different world" }
        return false
      }

      val removedChunk: Chunk? = writeChunks { writableChunks ->
        writableChunks.remove(chunk.compactLocation)
      }
      if (save) {
        chunkLoader.save(chunk)
      }
      chunk.dispose()
      logger.trace { "Unloaded chunk ${stringifyCompactLoc(chunk)}" }
      if (removedChunk != null && chunk !== removedChunk) {
        logger.warn {
          "Removed unloaded chunk ${stringifyCompactLoc(chunk)} was different from chunk in list of loaded chunks: ${stringifyCompactLoc(removedChunk)}"
        }
        removedChunk.dispose()
      }
      return true
    }
    return false
  }

  fun containsEntity(entityId: String): Boolean = getEntity(entityId) != null

  fun getEntity(entityId: String): Entity? {
    assertNotDisposed()
    for (entity in validEntities) {
      if (entity.id == entityId) {
        return entity
      }
    }
    return null
  }

  /**
   * Remove and disposes the given entity directly.
   *
   * Will not handle block entities correctly, set the tag [no.elg.infiniteBootleg.core.world.ecs.components.tags.BrokenBlockTag] to properly remove blocks.
   *
   * Even if the given entity is not a part of this world, it will be disposed
   *
   * @param entity The entity to remove
   * @throws IllegalArgumentException if the given entity is not part of this world
   */
  @JvmOverloads
  open fun removeEntity(entity: Entity, reason: DespawnReason = DespawnReason.UNKNOWN_REASON) {
    assertNotDisposed()
    engine.removeEntity(entity)
  }

  fun getPlayer(entityId: String): Entity? {
    assertNotDisposed()
    for (entity in playersEntities) {
      if (entity.id == entityId) {
        return entity
      }
    }
    return null
  }

  fun hasPlayer(entityId: String): Boolean = getPlayer(entityId) != null

  /**
   * @param worldX X center (center of each block
   * @param worldY Y center
   * @param radius Radius to be equal or less from center
   * @return Set of blocks within the given radius
   */
  fun getBlocksWithin(worldX: WorldCoord, worldY: WorldCoord, radius: Float): ObjectSet<Block> = getBlocksWithin(worldX + HALF_BLOCK_SIZE, worldY + HALF_BLOCK_SIZE, radius)

  fun getBlocksWithin(worldX: Float, worldY: Float, radius: Float): ObjectSet<Block> {
    require(radius >= 0) { "Radius should be a non-negative number" }
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
   * @param cancel    Allows for early termination of this method, argument are current blocks found
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
    cancel: (GdxArray<Block>?) -> Boolean = NEVER_CANCEL,
    filter: (Block) -> Boolean = ACCEPT_EVERY_BLOCK
  ): GdxArray<Block> {
    require(width >= 0) { "Width must be >= 0, was $width" }
    require(height >= 0) { "Height must be >= 0, was $height" }
    val worldX = centerWorldX - width
    val worldY = centerWorldY - height
    return getBlocksAABB(worldX, worldY, width * 2f, height * 2f, raw, loadChunk, includeAir, cancel, filter)
  }

  /**
   * @param worldX     Lower world X coordinate
   * @param worldY     Lower world Y coordinate
   * @param offsetX    Offset from the world X coordinate (inclusive)
   * @param offsetY    Offset from the world Y coordinate (inclusive)
   * @param raw        If non-existing blocks (i.e., air) should be created to be included
   * @param loadChunk  Whether to load the chunks the blocks exist in. If false no blocks from
   * unloaded chunks will be included
   * @param includeAir Whether air should be included
   * @param cancel     Allows for early termination of this method, argument are current blocks found
   * @return An Axis-Aligned Bounding Box of blocks
   */
  fun getBlocksAABB(
    worldX: WorldCoordFloat,
    worldY: WorldCoordFloat,
    offsetX: Float,
    offsetY: Float,
    raw: Boolean,
    loadChunk: Boolean,
    includeAir: Boolean,
    cancel: (GdxArray<Block>?) -> Boolean = NEVER_CANCEL,
    filter: (Block) -> Boolean = ACCEPT_EVERY_BLOCK
  ): GdxArray<Block> {
    assertNotDisposed()
    val effectiveRaw: Boolean = if (!raw && !includeAir) {
      logger.warn { "Will not include air AND air blocks will be created! (raw: false, includeAir: false)" }
      true
    } else {
      raw
    }
    require(offsetX >= 0) { "offsetX must be >= 0, was $offsetX" }
    require(offsetY >= 0) { "offsetY must be >= 0, was $offsetY" }
    var blocks: GdxArray<Block>? = null
    var x = MathUtils.floor(worldX)
    val startY = MathUtils.floor(worldY)
    val maxX = worldX + offsetX
    val maxY = worldY + offsetY
    var invalidChunks: Long2ObjectMap<Chunk>? = null
    while (x <= maxX) {
      var y = startY
      while (y <= maxY) {
        if (cancel !== NEVER_CANCEL && cancel(blocks)) {
          return blocks ?: EMPTY_BLOCKS_ARRAY
        }
        val chunkPos = compactInt(x.worldToChunk(), y.worldToChunk())
        if (invalidChunks != null && invalidChunks.containsKey(chunkPos)) {
          y++
          // No point in checking this chunk again
          continue
        }
        val chunk: Chunk? = getChunk(chunkPos, loadChunk)
        if (chunk.invalid()) {
          y++
          if (invalidChunks == null) {
            // Conservative estimate on how many chunks we may encounter, we don't expect them to be
            val estimatedChunks = (offsetX.worldToChunk() + 1) * (offsetY.worldToChunk() + 1)
            // When estimated chunks are very low it should be faster to use linear probing. A cutoff of 4 is conservative approximately when hash map may be faster
            invalidChunks = if (estimatedChunks <= 4) Long2ObjectArrayMap<Chunk>(estimatedChunks) else Long2ObjectOpenHashMap<Chunk>(1, FAST_LOAD_FACTOR)
          }
          invalidChunks.put(chunkPos, chunk)
          continue
        }
        val localX = x.chunkOffset()
        val localY = y.chunkOffset()
        val block = if (effectiveRaw) {
          chunk.getRawBlock(localX, localY)
        } else {
          chunk.getBlock(localX, localY)
        }
        if (block == null) {
          y++
          continue
        }
        if ((includeAir || block.isMarkerBlock() || block.isNotAir()) && (filter === ACCEPT_EVERY_BLOCK || filter(block))) {
          val nnBlocks = blocks ?: GdxArray<Block>(false, 1).also { array -> blocks = array }
          nnBlocks.add(block)
        }
        y++
      }
      x++
    }
    return blocks ?: EMPTY_BLOCKS_ARRAY
  }

  /**
   * @param worldX The x coordinate in world view
   * @param worldY The y coordinate in world view
   * @return The material at the given location
   */
  fun getMaterial(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean = true): Material = getRawBlock(worldX, worldY, loadChunk).materialOrAir()
  fun getMaterial(compactLoc: Long, loadChunk: Boolean = true): Material = getRawBlock(compactLoc, loadChunk).materialOrAir()

  /**
   * Alias to `WorldBody#postBox2dRunnable`
   */
  fun postBox2dRunnable(runnable: () -> Unit) {
    assertNotDisposed()
    worldBody.postBox2dRunnable(runnable)
  }

  fun postWorldTickerRunnable(runnable: () -> Unit) {
    assertNotDisposed()
    worldTicker.postRunnable(runnable)
  }

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

  override fun resize(width: Int, height: Int) = Unit

  override fun toString(): String = "World $name ($uuid)"

  fun assertNotDisposed() {
    check(!isDisposed) { "World $this is disposed" }
  }

  override fun dispose() {
    if (hasDisposeBegun) {
      return
    }
    metadata.worldDisposeState++
    save(overrideDispose = true)

    logger.debug { "Switching thread for disposal of $this to physics thread" }
    val job = ThreadType.PHYSICS.launchOrRunSuspended(this) {
      writeChunks { writableChunks ->
        writableChunks.values().forEach(Chunk::dispose)
        writableChunks.clear()
      }

      if (!isTransient) {
        WorldLoader.deleteLockFile(uuid)
      }
      metadata.dispose()
      VoidPointerManager.globalVPM.clear(checkMemLeak = false)
      EventManager.clear()
      // Some of these (e.g., engine) must be done from the physics thread
      engine.dispose()
      worldTicker.dispose()
      chunkColumnsManager.dispose()
      chunkLoader.dispose()
      worldBody.dispose()
      metadata.worldDisposeState++
      logger.debug { "$this have been fully disposed" }
    }
    // Make sure we are fully disposed before returning!
    runBlocking { job.join() }
  }

  companion object {
    /**
     * Size of block in world coordinates
     */
    const val BLOCK_SIZE = 1f
    const val HALF_BLOCK_SIZE = BLOCK_SIZE / 2f
    const val HALF_BLOCK_SIZE_D = BLOCK_SIZE / 2.0

    const val LIGHT_SOURCE_LOOK_BLOCKS = 10
    const val LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA = LIGHT_SOURCE_LOOK_BLOCKS + 2
    const val LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_F: Float = LIGHT_SOURCE_LOOK_BLOCKS + 2f
    const val LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_POW = LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA * LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA
    const val TRY_LOCK_CHUNKS_DURATION_MS = 20L
    const val LONG_WAIT_DURATION_CHUNK_LOCK_MS = 1000L

    val NEVER_CANCEL: (GdxArray<Block>?) -> Boolean = { false }
    val ACCEPT_EVERY_BLOCK: (Block) -> Boolean = { true }

    val CHUNK_THREAD_LOCAL = AtomicInteger(0)
    val CHUNK_REMOVED_THREAD_LOCAL = AtomicInteger(0)
    val CHUNK_ADDED_THREAD_LOCAL = AtomicInteger(0)

    val EMPTY_BLOCKS_ARRAY = GdxArray<Block>(false, 0)
      get() {
        require(field.items.isEmpty()) { "Expected EMPTY_BLOCKS_ARRAY to be an empty array, but got ${field.size} elements" }
        return field
      }

    fun getLocationsAABBFromCorner(cornerWorldX: WorldCoordNumber, cornerWorldY: WorldCoordNumber, offsetX: Float, offsetY: Float): WorldCompactLocArray =
      getLocationsAABBFromCenter(
        centerWorldX = cornerWorldX.toDouble() - offsetX / 2.0,
        centerWorldY = cornerWorldY.toDouble() - offsetY / 2.0,
        offsetX = offsetX / 2.0,
        offsetY = offsetY / 2.0
      )

    fun getLocationsAABBFromCorner(cornerWorldX: WorldCoordNumber, cornerWorldY: WorldCoordNumber, offsetX: Double, offsetY: Double): WorldCompactLocArray =
      getLocationsAABBFromCenter(
        centerWorldX = cornerWorldX.toFloat() - offsetX / 2.0,
        centerWorldY = cornerWorldY.toFloat() - offsetY / 2.0,
        offsetX = offsetX / 2.0,
        offsetY = offsetY / 2.0
      )

    /**
     * @return A square of locations within the given offset from the center
     */
    fun getLocationsAABBFromCenter(centerWorldX: Float, centerWorldY: Float, offsetX: Float, offsetY: Float): WorldCompactLocArray =
      getLocationsAABBFromCenter(centerWorldX.toDouble(), centerWorldY.toDouble(), offsetX.toDouble(), offsetY.toDouble())

    /**
     * @return A square of locations within the given offset from the center
     */
    fun getLocationsAABBFromCenter(centerWorldX: Double, centerWorldY: Double, offsetX: Double, offsetY: Double): WorldCompactLocArray {
      var x = floor(centerWorldX - offsetX).toLong()
      val maxX = (centerWorldX + offsetX).toLong()
      val maxY = (centerWorldY + offsetY).toLong()
      val startY = floor(centerWorldY - offsetY).toLong()
      val capacity = (maxX - x + 1) * (maxY - startY + 1)
      val blocks = LongArray(capacity.toInt())
      var i = 0
      while (x <= maxX) {
        var y = startY
        while (y <= maxY) {
          blocks[i++] = compactLong(x, y)
          y++
        }
        x++
      }
      require(i == blocks.size) { "Expected to fill all blocks, but only filled $i" }
      return blocks
    }

    /**
     * @param worldX X center
     * @param worldY Y center
     * @param radius Radius to be equal or less from center
     * @return Set of blocks within the given radius
     */
    fun getLocationsWithin(worldX: WorldCoordNumber, worldY: WorldCoordNumber, radius: Float): LongOpenHashSet =
      getLocationsWithin(worldX.toDouble(), worldY.toDouble(), radius.toDouble())

    fun getLocationsWithin(worldX: WorldCoordNumber, worldY: WorldCoordNumber, radius: Double): LongOpenHashSet {
      require(radius >= 0) { "Radius should be a non-negative number" }
      if (radius <= 1.0) {
        val coord = compactInt(worldX.worldToBlock(), worldY.worldToBlock())
        return LongOpenHashSet(1).apply { add(coord) }
      } else {
        val expected = (radius * radius * Math.PI).toInt()
        val locs = LongOpenHashSet(expected)
        val centerWorldX = worldX.toDouble() + HALF_BLOCK_SIZE_D
        val centerWorldY = worldY.toDouble() + HALF_BLOCK_SIZE_D
        for (compact in getLocationsAABBFromCenter(centerWorldX, centerWorldY, radius, radius)) {
          if (isBlockInsideRadius(centerWorldX, centerWorldY, compact.decompactLocX(), compact.decompactLocY(), radius)) {
            locs.add(compact)
          }
        }
        return locs
      }
    }
  }
}
