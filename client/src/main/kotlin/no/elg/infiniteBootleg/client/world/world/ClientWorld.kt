package no.elg.infiniteBootleg.client.world.world

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Gdx
import ktx.assets.dispose
import no.elg.infiniteBootleg.client.input.ECSInputListener
import no.elg.infiniteBootleg.client.input.WorldInputHandler
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.box2d.service.DoorService
import no.elg.infiniteBootleg.client.world.ecs.system.FollowEntitySystem
import no.elg.infiniteBootleg.client.world.ecs.system.MagicSystem
import no.elg.infiniteBootleg.client.world.ecs.system.MineBlockSystem
import no.elg.infiniteBootleg.client.world.ecs.system.event.ContinuousInputSystem
import no.elg.infiniteBootleg.client.world.ecs.system.event.InputEventSystem
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.ecs.ThreadSafeEntitySet
import no.elg.infiniteBootleg.core.world.ecs.drawableMaterialEntitiesFamily
import no.elg.infiniteBootleg.core.world.ecs.drawableNonMaterialEntitiesFamily
import no.elg.infiniteBootleg.core.world.ecs.localPlayerFamily
import no.elg.infiniteBootleg.core.world.ecs.selectedMaterialComponentFamily
import no.elg.infiniteBootleg.core.world.ecs.system.event.PhysicsEventSystem
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld

/**
 * @author Elg
 */
abstract class ClientWorld : World {
  final override val render: ClientWorldRender
  val input: WorldInputHandler

  init {
    ClientMain.inst().updateStatus(this)
    render = ClientWorldRender(this)
    input = WorldInputHandler(render)
  }

  constructor(protoWorld: ProtoWorld.World, forceTransient: Boolean) : super(protoWorld, forceTransient)
  constructor(generator: ChunkGenerator, seed: Long, worldName: String, forceTransient: Boolean) : super(generator, seed, worldName, forceTransient)

  abstract fun isAuthorizedToChange(entity: Entity): Boolean

  override fun resize(width: Int, height: Int) {
    render.resize(width, height)
  }

  private lateinit var controlledPlayerEntitySet: ThreadSafeEntitySet
  val controlledPlayerEntities: Set<Entity> get() = controlledPlayerEntitySet.entities

  private lateinit var selectedMaterialEntitySet: ThreadSafeEntitySet
  val selectedMaterialEntities: Set<Entity> get() = selectedMaterialEntitySet.entities

  private lateinit var drawableNonMaterialEntitySet: ThreadSafeEntitySet
  val drawableNonMaterialEntities: Set<Entity> get() = drawableNonMaterialEntitySet.entities

  private lateinit var drawableMaterialEntitySet: ThreadSafeEntitySet
  val drawableMaterialEntities: Set<Entity> get() = drawableMaterialEntitySet.entities

  override fun addEntityListeners(engine: Engine) {
    controlledPlayerEntitySet = ThreadSafeEntitySet(engine, localPlayerFamily)
    selectedMaterialEntitySet = ThreadSafeEntitySet(engine, selectedMaterialComponentFamily)
    drawableNonMaterialEntitySet = ThreadSafeEntitySet(engine, drawableNonMaterialEntitiesFamily)
    drawableMaterialEntitySet = ThreadSafeEntitySet(engine, drawableMaterialEntitiesFamily)
  }

  override fun additionalSystems() =
    setOf(
      ContinuousInputSystem(ECSInputListener(this)),
      MineBlockSystem,
      FollowEntitySystem,
      InputEventSystem,
      MagicSystem
    )

  override fun configureSystem(system: EntitySystem) {
    if (system is PhysicsEventSystem) {
      system.handlers += DoorService
    }
  }

  override fun dispose() {
    super.dispose()
    input.dispose()
    // Must be done on GL thread
    Gdx.app.postRunnable { render.dispose { it.printStackTrace() } }
  }

  fun recalculateLights() {
    readChunks { readableChunks ->
      readableChunks.values().forEach(Chunk::updateAllBlockLights)
    }
  }
}
