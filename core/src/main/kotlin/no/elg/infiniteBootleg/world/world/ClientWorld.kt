package no.elg.infiniteBootleg.world.world

import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.input.ECSInputListener
import no.elg.infiniteBootleg.input.WorldInputHandler
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.render.ClientWorldRender

/**
 * @author Elg
 */
abstract class ClientWorld : World {
  final override val render: ClientWorldRender
  val input: WorldInputHandler
  val ecsInput: ECSInputListener

  init {
    ClientMain.inst().updateStatus(this)
    render = ClientWorldRender(this)
    ecsInput = ECSInputListener(this)
    input = WorldInputHandler(render)
  }

  constructor(protoWorld: ProtoWorld.World) : super(protoWorld)
  constructor(generator: ChunkGenerator, seed: Long, worldName: String) : super(generator, seed, worldName)

  override fun resize(width: Int, height: Int) {
    render.resize(width, height)
  }

  override fun dispose() {
    super.dispose()
    input.dispose()
    ecsInput.dispose()
    // Must be done on GL thread
    Gdx.app.postRunnable { render.dispose() }
  }
}
