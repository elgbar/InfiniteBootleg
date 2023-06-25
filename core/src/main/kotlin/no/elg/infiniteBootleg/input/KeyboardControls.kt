package no.elg.infiniteBootleg.input

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.math.Vector2
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.MouseLocator
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.util.placeableBlock
import no.elg.infiniteBootleg.util.worldToBlock
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.grounded
import no.elg.infiniteBootleg.world.ecs.components.InventoryComponent.Companion.inventoryOrNull
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocity
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.world.ecs.components.tags.UpdateBox2DVelocityTag.Companion.updateBox2DVelocity
import no.elg.infiniteBootleg.world.world.ClientWorld
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
class KeyboardControls(val world: ClientWorld) {

  var breakBrushSize = 2f
  var placeBrushSize = 1f

  private val tmpVec = Vector2()
  private val tmpVec2 = Vector2()

  private val mouseLocator = MouseLocator()

  private fun breakBlocks(entity: Entity, blockX: Int, blockY: Int, worldX: Float, worldY: Float): Boolean {
    Main.inst().scheduler.executeAsync {
      val world = entity.world.world
      if (breakBrushSize <= 1) {
        world.removeBlock(blockX, blockY)
      } else {
        val blocksWithin = world.getBlocksWithin(blockX, blockY, breakBrushSize)
        blocksWithin.removeAll { it.material == Material.AIR }
        world.removeBlocks(blocksWithin)
      }
    }
    return true
  }

  private fun placeBlocks(entity: Entity, blockX: Int, blockY: Int, worldX: Float, worldY: Float): Boolean {
    val world = entity.world.world
    if (!world.getEntities(worldX, worldY).isEmpty) {
      // cannot place on an entity
      return false
    }
    val material = (entity.selectedOrNull ?: return false).material
    val inventory = entity.inventoryOrNull ?: return false
    val placeableBlock = entity.placeableBlock(world, blockX, blockY, placeBrushSize)
    if (inventory.use(material, placeableBlock.size.toUInt())) {
      material.create(world, placeableBlock)
    }
    return true
  }

  private fun Entity.fly() {
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

  private fun Entity.walk() {
    fun moveHorz(dir: Float) {
      val world = world.world
      world.postBox2dRunnable {
        if (grounded.canMove(dir)) {
          val body = box2d.body

          val currSpeed = body.linearVelocity.x
          val wantedSpeed = dir * if (grounded.onGround) {
            MAX_X_VEL
          } else {
            MAX_X_VEL * (2f / 3f)
          }
          val impulse = body.mass * (wantedSpeed - (dir * min(abs(currSpeed), abs(wantedSpeed))))

          tmpVec.set(impulse, velocity.dy)

          body.applyLinearImpulse(tmpVec, body.worldCenter, true)
        }
      }
    }

    if (Gdx.input.isKeyPressed(Keys.A)) {
      moveHorz(-1f)
    }
    if (Gdx.input.isKeyPressed(Keys.D)) {
      moveHorz(1f)
    }
  }

  private fun Entity.jump() {
    if (this.grounded.onGround && Gdx.input.isKeyPressed(Keys.W)) {
      setVel { oldX, _ -> oldX to JUMP_VERTICAL_VEL }
    }
  }

  fun update(entity: Entity) {
    val entityWorld = entity.world.world
    if (entityWorld is ClientWorld) {
      mouseLocator.update(entityWorld)
    }
    if (Main.inst().console.isVisible) {
      return
    }

    when {
      Gdx.input.isButtonPressed(Buttons.LEFT) -> entity.interpolate(false, this::breakBlocks)
      Gdx.input.isButtonPressed(Buttons.RIGHT) -> entity.interpolate(false, this::placeBlocks)
      Gdx.input.isKeyJustPressed(Keys.Q) -> entity.interpolate(true, this::placeBlocks)
    }

    if (entity.flying) {
      entity.fly()
    } else {
      entity.jump()
      entity.walk()
    }
  }

  private fun Entity.setVel(modify: (oldX: Float, oldY: Float) -> (Pair<Float, Float>)) {
    val body = box2d.body
    val vel = body.linearVelocity
    val (nx, ny) = modify(vel.x, vel.y)
    val cap = { z: Float, max: Float -> sign(z) * min(max, abs(z)) }

    val velocityComponent = this.velocity
    velocityComponent.dx = cap(nx, MAX_X_VEL)
    velocityComponent.dy = cap(ny, MAX_Y_VEL)
    this.updateBox2DVelocity = true
  }

  fun touchDown(entity: Entity, button: Int) {
    val update =
      when (button) {
        Buttons.LEFT -> entity.interpolate(true, this::breakBlocks)
        Buttons.RIGHT -> entity.interpolate(true, this::placeBlocks)
        else -> false
      }

    if (update) {
      entity.world.world.render.update()
    }
  }

  fun keyDown(entity: Entity, keycode: Int): Boolean {
    when (keycode) {
      Keys.T -> entity.teleport(mouseLocator.mouseWorldX, mouseLocator.mouseWorldY)
      Keys.Q -> entity.interpolate(true, this::placeBlocks)
    }

    val selectedMaterial = entity.selectedOrNull ?: return true

    selectedMaterial.material = when (keycode) {
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
      else -> selectedMaterial.material
    }
    return true
  }

  /**
   * @param justPressed Whether to interpolate between placements
   */
  private fun Entity.interpolate(justPressed: Boolean, action: (entity: Entity, blockX: Int, blockY: Int, worldX: Float, worldY: Float) -> Boolean): Boolean {
    val blockX = mouseLocator.mouseBlockX
    val blockY = mouseLocator.mouseBlockY
    val worldX = mouseLocator.mouseWorldX
    val worldY = mouseLocator.mouseWorldY

    val inSameBlock = mouseLocator.previousMouseBlockX == blockX && mouseLocator.previousMouseBlockY == blockY
    if (justPressed || inSameBlock) {
      return action(this, blockX, blockY, worldX, worldY)
    }

    val currPos = tmpVec
    val prevPos = tmpVec2

    currPos.x = worldX
    currPos.y = worldY

    prevPos.x = mouseLocator.previousMouseWorldX
    prevPos.y = mouseLocator.previousMouseWorldY

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

      val logging = Settings.debug && Gdx.graphics.frameId % 100 == 0L && distance > Int.MAX_VALUE
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

        val pWx = halfpoint(worldX, mouseLocator.previousMouseWorldX, multiplierX)
        val pWy = halfpoint(worldY, mouseLocator.previousMouseWorldY, multiplierY)
        val pBx = worldToBlock(pWx)
        val pBy = worldToBlock(pWy)

        if (logging) {
          Main.logger().log("--inter $i mltX: $multiplierX | mltY: $multiplierY, pBx:$pBx | pBy:$pBy | pWx:$pWx | pWy:$pWy")
        }
        update = update or action(this, pBx, pBy, pWx, pWy)
      }

      if (logging) {
        Main.logger().log("---END SMOOTH PLACEMENT (update? $update)---")
      }
    }

    return update or action(this, blockX, blockY, worldX, worldY)
  }

  companion object {

    private const val JUMP_VERTICAL_VEL = 7.5f
    private const val FLY_VEL = 1f

    const val MAX_X_VEL = 7.5f // ie target velocity
    const val MAX_Y_VEL = 100f
  }
}
