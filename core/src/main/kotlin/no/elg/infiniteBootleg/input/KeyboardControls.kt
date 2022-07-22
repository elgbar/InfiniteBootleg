package no.elg.infiniteBootleg.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.math.Vector2
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.subgrid.Entity.GROUND_CHECK_OFFSET
import no.elg.infiniteBootleg.world.subgrid.LivingEntity
import no.elg.infiniteBootleg.world.subgrid.enitites.Player
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Control scheme where the user moves the player around with a keyboard
 *
 * @author Elg
 */
class KeyboardControls(worldRender: ClientWorldRender, entity: LivingEntity) : AbstractEntityControls(worldRender, entity) {
  private var selected: Material = Material.BRICK

  private var breakBrushSize = 2f
  private var placeBrushSize = 1f

  private val tmpVec = Vector2()
  private val tmpVec2 = Vector2()

  private fun breakBlocks(blockX: Int, blockY: Int, worldX: Float, worldY: Float): Boolean {
    if (breakBrushSize <= 1) {
      world.remove(blockX, blockY, true)
    } else {
      val blocksWithin = world.getBlocksWithin(worldX, worldY, breakBrushSize)
      if (blocksWithin.isEmpty) {
        world.remove(blockX, blockY, true)
      } else {
        world.removeBlocks(blocksWithin, true)
      }
    }
    return true
  }

  private fun placeBlocks(blockX: Int, blockY: Int, worldX: Float, worldY: Float): Boolean {
    var update = false
    if (!world.getEntities(worldX, worldY).isEmpty) {
      // cannot place on an entity
      return false
    }

    if (placeBrushSize <= 1) {
      update = selected.create(world, blockX, blockY, true)
    } else {
      val blocksWithin = world.getBlocksWithin(worldX, worldY, placeBrushSize)
      if (blocksWithin.isEmpty) {
        update = selected.create(world, blockX, blockY, true)
      } else {
        for (block in blocksWithin) {
          update = update or selected.create(world, block.worldX, block.worldY, true)
        }
      }
    }

    return update
  }

  private fun teleport() {
    // teleport the player to the (last) location of the mouse
    controlled.teleport(ClientMain.inst().mouseWorldX, ClientMain.inst().mouseWorldY, true)
    world.input.following = controlled
  }

  private fun fly() {
    fun fly(dx: Float = 0f, dy: Float = 0f) {
      setVel { oldX, oldY -> oldX + dx to oldY + dy }
    }

    when {
      Gdx.input.isKeyPressed(Keys.W) -> fly(dy = FLY_VEL)
      Gdx.input.isKeyPressed(Keys.S) -> fly(dy = -FLY_VEL)
      Gdx.input.isKeyPressed(Keys.A) -> fly(dx = -FLY_VEL)
      Gdx.input.isKeyPressed(Keys.D) -> fly(dx = FLY_VEL)
    }
  }

  private fun walk() {
    fun moveHorz(dir: Float) {
      if (!controlled.validLocation(controlled.position.x + GROUND_CHECK_OFFSET * dir, controlled.position.y)) {
        return
      }
      world.postBox2dRunnable {
        val body = controlled.body

        val currSpeed = body.linearVelocity.x
        val wantedSpeed = dir * if (controlled.isOnGround) {
          MAX_X_VEL
        } else {
          MAX_X_VEL * (2f / 3f)
        }
        val impulse = body.mass * (wantedSpeed - (dir * min(abs(currSpeed), abs(wantedSpeed))))

        tmpVec.set(impulse, controlled.velocity.y)

        body.applyLinearImpulse(tmpVec, body.worldCenter, true)
      }
    }

    if (Gdx.input.isKeyPressed(Keys.A)) {
      moveHorz(-1f)
    }
    if (Gdx.input.isKeyPressed(Keys.D)) {
      moveHorz(1f)
    }
  }

  private fun jump() {
    if (controlled.isOnGround && Gdx.input.isKeyPressed(Keys.W)) {
      setVel { oldX, _ -> oldX to JUMP_VERTICAL_VEL }
    }
  }

  private fun updateLookDirection(player: Player) {
    val angle: Float = tmpVec.set(ClientMain.inst().mouse).sub(player.position).angleDeg()
    player.lookDeg = angle
  }

  override fun update() {
    if (Main.inst().console.isVisible) {
      return
    }
    val update =
      when {
        Gdx.input.isButtonPressed(Buttons.LEFT) -> interpolate(Gdx.input.isButtonJustPressed(Buttons.RIGHT), this::breakBlocks)
        Gdx.input.isButtonPressed(Buttons.RIGHT) -> interpolate(Gdx.input.isButtonJustPressed(Buttons.RIGHT), this::placeBlocks)
        Gdx.input.isKeyJustPressed(Keys.Q) -> interpolate(true, this::placeBlocks)
        else -> false
      }

    if (Gdx.input.isKeyJustPressed(Keys.T)) {
      teleport()
    } else {
      if (controlled.isFlying) {
        fly()
      } else {
        jump()
        walk()
      }
    }

    if (controlled is Player) {
      val player = controlled as Player

      if (Gdx.input.isKeyJustPressed(Keys.P)) {
        player.toggleTorch()
      }
      updateLookDirection(player)
    }

    if (update) {
      worldRender.update()
    }
  }

  private fun setVel(modify: (oldX: Float, oldY: Float) -> (Pair<Float, Float>)) {
    world.postBox2dRunnable {
      val body = controlled.body
      val vel = body.linearVelocity
      val (nx, ny) = modify(vel.x, vel.y)
      val cap = { z: Float, max: Float -> sign(z) * min(max, abs(z)) }
      body.setLinearVelocity(cap(nx, MAX_X_VEL), cap(ny, MAX_Y_VEL))
      body.isAwake = true
    }
  }

  override fun keyDown(keycode: Int): Boolean {
    if (Main.inst().console.isVisible) {
      return false
    }
    selected = when (keycode) {
      Keys.NUM_0, Keys.NUMPAD_0 -> Material.values()[0]
      Keys.NUM_1, Keys.NUMPAD_1 -> Material.values()[1]
      Keys.NUM_2, Keys.NUMPAD_2 -> Material.values()[2]
      Keys.NUM_3, Keys.NUMPAD_3 -> Material.values()[3]
      Keys.NUM_4, Keys.NUMPAD_4 -> Material.values()[4]
      Keys.NUM_5, Keys.NUMPAD_5 -> Material.values()[5]
      Keys.NUM_6, Keys.NUMPAD_6 -> Material.values()[6]
      Keys.NUM_7, Keys.NUMPAD_7 -> Material.values()[7]
      Keys.NUM_8, Keys.NUMPAD_8 -> Material.values()[8]
      Keys.NUM_9, Keys.NUMPAD_9 -> Material.values()[9]
      else -> return false
    }
    return true
  }

  /**
   * @param justPressed Whether to interpolate between placements
   */
  private fun interpolate(justPressed: Boolean, action: (blockX: Int, blockY: Int, worldX: Float, worldY: Float) -> Boolean): Boolean {
    val main = ClientMain.inst()
    val blockX = main.mouseBlockX
    val blockY = main.mouseBlockY
    val worldX = main.mouseWorldX
    val worldY = main.mouseWorldY

    val inSameBlock = main.previousMouseBlockX == blockX && main.previousMouseBlockY == blockY
    if (justPressed || inSameBlock) {
      return action(blockX, blockY, worldX, worldY)
    }

    val currPos = tmpVec
    val prevPos = tmpVec2

    currPos.x = worldX
    currPos.y = worldY

    prevPos.x = main.previousMouseWorldX
    prevPos.y = main.previousMouseWorldY

    fun Vector2.dstd(v: Vector2): Double {
      val dx: Float = v.x - x
      val dy: Float = v.y - y
      return sqrt((dx * dx + dy * dy).toDouble())
    }

    val distance = currPos.dstd(prevPos)
    // Limit max distance to draw
    val maxDistance = 20.0

    var update = false
    if (distance in 0.0..maxDistance) {
      val iterations = ceil(distance).toInt()

      val logging = Settings.debug && Gdx.graphics.frameId % 100 == 0L && distance > 1
      if (logging) {
        Main.logger().log("---START SMOOTH PLACEMENT---")
        Main.logger().log("(pos) prev: $prevPos, curr $currPos")
        Main.logger().log("(distance) $distance")
        Main.logger().log("Doing $iterations iterations of interpolation")
      }
      for (i in 1 until iterations + 1) {
        val multiplierX = (distance - i) / distance

        // When drawing from (lower right to upper left) and (upper right to lower right) the Y multiplication must be the inverse
        //
        val multiplierY = when {
          prevPos.x < currPos.x && prevPos.y > currPos.y -> 1 - multiplierX
          prevPos.x > currPos.x && prevPos.y < currPos.y -> 1 - multiplierX
          else -> multiplierX
        }

        if (multiplierX.isInfinite() || multiplierX !in 0.0..1.0) {
          // If the multiplier is weird, don't place a block
          continue
        }

        fun halfpoint(i1: Float, i2: Float, multiplier: Double): Float = (min(i1, i2) + abs(i1 - i2) * multiplier).toFloat()

        val pWx = halfpoint(worldX, main.previousMouseWorldX, multiplierX)
        val pWy = halfpoint(worldY, main.previousMouseWorldY, multiplierY)
        val pBx = CoordUtil.worldToBlock(pWx)
        val pBy = CoordUtil.worldToBlock(pWy)

        if (logging) {
          Main.logger().log("--inter $i mltX: $multiplierX | mltY: $multiplierY, pBx:$pBx | pBy:$pBy | pWx:$pWx | pWy:$pWy")
        }
        update = update or action(pBx, pBy, pWx, pWy)
      }

      if (logging) {
        Main.logger().log("---END SMOOTH PLACEMENT (update? $update)---")
      }
    }

    return update or action(blockX, blockY, worldX, worldY)
  }

  override fun getSelected(): Material {
    return selected
  }

  override fun setSelected(selected: Material) {
    this.selected = selected
  }

  override fun getBreakBrushSize(): Float {
    return breakBrushSize
  }

  override fun setBreakBrushSize(breakBrushSize: Float) {
    this.breakBrushSize = breakBrushSize
  }

  override fun getPlaceBrushSize(): Float {
    return placeBrushSize
  }

  override fun setPlaceBrushSize(placeBrushSize: Float) {
    this.placeBrushSize = placeBrushSize
  }

  companion object {
    const val JUMP_VERTICAL_VEL = 20f
    const val FLY_VEL = .075f

    const val MAX_X_VEL = 15f // ie target velocity
    const val MAX_Y_VEL = 100f
  }
}
