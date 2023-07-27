package no.elg.infiniteBootleg.input

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.math.Vector2
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.breakableBlocks
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.dstd
import no.elg.infiniteBootleg.util.placeableBlocks
import no.elg.infiniteBootleg.util.worldToBlock
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.components.InventoryComponent.Companion.inventoryOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocity
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.world.ecs.components.transients.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.world.ecs.components.transients.GroundedComponent.Companion.grounded
import no.elg.infiniteBootleg.world.ecs.components.transients.SelectedInventoryItemComponent.Companion.selectedOrNull
import no.elg.infiniteBootleg.world.ecs.components.transients.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ticker.WorldBox2DTicker.Companion.BOX2D_TPS
import no.elg.infiniteBootleg.world.world.ClientWorld
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

/**
 * Control scheme where the user moves the player around with a keyboard
 *
 * @author Elg
 */
class KeyboardControls(val world: ClientWorld) {

  var brushSize = INITIAL_BRUSH_SIZE
  var interactRadius = INITIAL_INTERACT_RADIUS

  private val tmpVec = Vector2()
  private val tmpVec2 = Vector2()

  private val mouseLocator = MouseLocator()
  private var lastCreateBlockLoc: Long = 0
  private var lastCreateBlockTick: Long = 0

  private fun breakBlocks(entity: Entity, blockX: Int, blockY: Int): Boolean {
    if (canNotInteract(blockX, blockY)) return false
    val world = entity.world
    val breakableBlocks = entity.breakableBlocks(world, blockX, blockY, brushSize, interactRadius).asIterable()
    world.removeBlocks(world.getBlocks(breakableBlocks))
    return true
  }

  private fun placeBlocks(entity: Entity, blockX: Int, blockY: Int): Boolean {
    if (canNotInteract(blockX, blockY)) return false
    val world = entity.world
    val material = (entity.selectedOrNull ?: return false).material
    val inventory = entity.inventoryOrNull ?: return false
    val placeableBlock = entity.placeableBlocks(world, blockX, blockY, brushSize, interactRadius).toSet()
    if (inventory.use(material, placeableBlock.size.toUInt())) {
      material.createBlocks(world, placeableBlock)
    }
    return true
  }

  private fun canNotInteract(blockX: Int, blockY: Int): Boolean {
    val compactLoc = compactLoc(blockX, blockY)
    val tick = world.tick
    if (lastCreateBlockLoc == compactLoc && world.tick - lastCreateBlockTick < BOX2D_TPS / 10f) {
      return true
    }
    lastCreateBlockLoc = compactLoc
    lastCreateBlockTick = tick
    return false
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
      val world = world
      world.postBox2dRunnable {
        if (grounded.canMove(dir)) {
          val body = box2dBody

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
    val entityWorld = entity.world
    if (entityWorld is ClientWorld) {
      mouseLocator.update(entityWorld)
    }
    if (ClientMain.inst().shouldIgnoreWorldInput()) {
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
    val body = box2dBody
    val vel = body.linearVelocity
    val (nx, ny) = modify(vel.x, vel.y)
    this.setVelocity(nx, ny)
  }

  fun touchDown(entity: Entity, button: Int) {
    val update =
      when (button) {
        Buttons.LEFT -> entity.interpolate(true, this::breakBlocks)
        Buttons.RIGHT -> entity.interpolate(true, this::placeBlocks)
        else -> false
      }

    if (update) {
      entity.world.render.update()
    }
  }

  fun keyDown(entity: Entity, keycode: Int): Boolean {
    when (keycode) {
      Keys.T -> entity.teleport(mouseLocator.mouseWorldX, mouseLocator.mouseWorldY)
      Keys.Q -> entity.interpolate(true, this::placeBlocks)
    }

    val selectedMaterial = entity.selectedOrNull ?: return true

    val extra = if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT)) 10 else 0
    try {
      when (keycode) {
        Keys.NUM_0, Keys.NUMPAD_0 -> Material.entries[0 + extra]
        Keys.NUM_1, Keys.NUMPAD_1 -> Material.entries[1 + extra]
        Keys.NUM_2, Keys.NUMPAD_2 -> Material.entries[2 + extra]
        Keys.NUM_3, Keys.NUMPAD_3 -> Material.entries[3 + extra]
        Keys.NUM_4, Keys.NUMPAD_4 -> Material.entries[4 + extra]
        Keys.NUM_5, Keys.NUMPAD_5 -> Material.entries[5 + extra]
        Keys.NUM_6, Keys.NUMPAD_6 -> Material.entries[6 + extra]
        Keys.NUM_7, Keys.NUMPAD_7 -> Material.entries[7 + extra]
        Keys.NUM_8, Keys.NUMPAD_8 -> Material.entries[8 + extra]
        Keys.NUM_9, Keys.NUMPAD_9 -> Material.entries[9 + extra]
        else -> null
      }?.let {
        selectedMaterial.material = it
      }
    } catch (_: IndexOutOfBoundsException) {
      // Ignore out of bounds, for materials that don't exist
    }
    return true
  }

  /**
   * @param justPressed Whether to interpolate between placements
   */
  private fun Entity.interpolate(justPressed: Boolean, action: (entity: Entity, blockX: Int, blockY: Int) -> Boolean): Boolean {
    val blockX = mouseLocator.mouseBlockX
    val blockY = mouseLocator.mouseBlockY
    val worldX = mouseLocator.mouseWorldX
    val worldY = mouseLocator.mouseWorldY

    val inSameBlock = mouseLocator.previousMouseBlockX == blockX && mouseLocator.previousMouseBlockY == blockY
    if (justPressed || inSameBlock) {
      return action(this, blockX, blockY)
    }

    val currPos = tmpVec
    val prevPos = tmpVec2

    currPos.x = worldX
    currPos.y = worldY

    prevPos.x = mouseLocator.previousMouseWorldX
    prevPos.y = mouseLocator.previousMouseWorldY

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
        update = update or action(this, pBx, pBy)
      }

      if (logging) {
        Main.logger().log("---END SMOOTH PLACEMENT (update? $update)---")
      }
    }

    return update or action(this, blockX, blockY)
  }

  companion object {

    private const val JUMP_VERTICAL_VEL = 9f
    private const val FLY_VEL = 1f

    const val MAX_X_VEL = 7.5f // ie target velocity
    const val MAX_Y_VEL = 20f

    const val INITIAL_BRUSH_SIZE = 1f
    const val INITIAL_INTERACT_RADIUS = 32f
  }
}
