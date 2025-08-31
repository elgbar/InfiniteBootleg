package no.elg.infiniteBootleg.client.world.world

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.client.input.ECSInputListener
import no.elg.infiniteBootleg.client.input.WorldInputHandler
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.box2d.service.DoorService
import no.elg.infiniteBootleg.client.world.ecs.system.FollowEntitySystem
import no.elg.infiniteBootleg.client.world.ecs.system.MagicSystem
import no.elg.infiniteBootleg.client.world.ecs.system.MineBlockSystem
import no.elg.infiniteBootleg.client.world.ecs.system.event.ContinuousInputSystem
import no.elg.infiniteBootleg.client.world.ecs.system.event.InputSystem
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.world.ecs.system.event.PhysicsSystem
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

  override fun additionalSystems() =
    setOf(
      ContinuousInputSystem(ECSInputListener(this)),
      MineBlockSystem,
      FollowEntitySystem,
      InputSystem,
      MagicSystem
    )

  override fun configureSystem(system: EntitySystem) {
    if (system is PhysicsSystem) {
      system.handlers += DoorService
    }
  }

  override fun dispose() {
    super.dispose()
    input.dispose()
    // Must be done on GL thread
    Gdx.app.postRunnable { render.dispose() }
  }
}
