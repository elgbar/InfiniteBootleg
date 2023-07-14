package no.elg.infiniteBootleg.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import no.elg.infiniteBootleg.util.worldToBlock
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.world.ClientWorld

class MouseLocator {
  var mouseBlockX = 0
    private set
  var mouseBlockY = 0
    private set
  var mouseWorldX = 0f
    private set
  var mouseWorldY = 0f
    private set
  var previousMouseBlockX = 0
    private set
  var previousMouseBlockY = 0
    private set
  var previousMouseWorldX = 0f
    private set
  var previousMouseWorldY = 0f
    private set

  private val mouseWorldInput = Vector2()
  val screenInputVec = Vector3()

  fun update(world: ClientWorld) {
    screenInputVec.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
    world.render.camera.unproject(screenInputVec)
    // Whenever z is not zero unproject returns a very low number
    // I don't know why this is the case, but checking for z to be zero seems to fix the bug
    if (screenInputVec.z == 0f) {
      previousMouseWorldX = mouseWorldX
      previousMouseWorldY = mouseWorldY
      previousMouseBlockX = mouseBlockX
      previousMouseBlockY = mouseBlockY

      val worldBody = world.worldBody
      mouseWorldX = screenInputVec.x / Block.BLOCK_SIZE - worldBody.worldOffsetX
      mouseWorldY = screenInputVec.y / Block.BLOCK_SIZE - worldBody.worldOffsetY
      mouseWorldInput.set(mouseWorldX, mouseWorldY)
      mouseBlockX = worldToBlock(mouseWorldX)
      mouseBlockY = worldToBlock(mouseWorldY)
    }
  }
}
