package no.elg.infiniteBootleg.client.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.WorldCoordFloat
import no.elg.infiniteBootleg.core.util.compactLoc
import no.elg.infiniteBootleg.core.util.worldToBlock
import no.elg.infiniteBootleg.core.world.blocks.Block

class MouseLocator {
  var mouseBlockX: WorldCoord = 0
    private set
  var mouseBlockY: WorldCoord = 0
    private set
  var mouseWorldX: WorldCoordFloat = 0f
    private set
  var mouseWorldY: WorldCoordFloat = 0f
    private set
  var previousMouseBlockX: WorldCoord = 0
    private set
  var previousMouseBlockY: WorldCoord = 0
    private set
  var previousMouseWorldX: WorldCoordFloat = 0f
    private set
  var previousMouseWorldY: WorldCoordFloat = 0f
    private set

  private val mouseWorldInput = Vector2()
  private val screenInputVec = Vector3()

  val mouseBlockCompactLoc: Long get() = compactLoc(mouseBlockX, mouseBlockY)

  /**
   * If the block the mouse is over has changed since the last update
   */
  val isOverNewBlock: Boolean get() = mouseBlockX != previousMouseBlockX || mouseBlockY != previousMouseBlockY

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

      mouseWorldX = screenInputVec.x / Block.Companion.BLOCK_TEXTURE_SIZE
      mouseWorldY = screenInputVec.y / Block.Companion.BLOCK_TEXTURE_SIZE
      mouseWorldInput.set(mouseWorldX, mouseWorldY)
      mouseBlockX = worldToBlock(mouseWorldX)
      mouseBlockY = worldToBlock(mouseWorldY)
    }
  }
}
