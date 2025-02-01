package no.elg.infiniteBootleg.world.world

import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.input.ECSInputListener
import no.elg.infiniteBootleg.input.WorldInputHandler
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.system.FollowEntitySystem
import no.elg.infiniteBootleg.world.ecs.system.MagicSystem
import no.elg.infiniteBootleg.world.ecs.system.MineBlockSystem
import no.elg.infiniteBootleg.world.ecs.system.event.ContinuousInputSystem
import no.elg.infiniteBootleg.world.ecs.system.event.InputSystem
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.render.ClientWorldRender

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

  override fun dispose() {
    super.dispose()
    input.dispose()
    // Must be done on GL thread
    Gdx.app.postRunnable { render.dispose() }
  }
}
