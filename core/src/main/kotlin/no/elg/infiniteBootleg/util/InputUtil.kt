package no.elg.infiniteBootleg.util

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.input.MouseLocator
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.world.ecs.components.InventoryComponent.Companion.inventoryComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedInventoryItemComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.world.ecs.components.additional.GroundedComponent.Companion.groundedComponent
import no.elg.infiniteBootleg.world.ecs.components.additional.LocallyControlledComponent.Companion.locallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.additional.LocallyControlledComponent.Companion.locallyControlledComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ticker.WorldBox2DTicker
import no.elg.infiniteBootleg.world.world.World
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

class WorldEntity(val world: World, val entity: Entity)

const val JUMP_VERTICAL_VEL = 9f
const val FLY_VEL = 1f

const val MAX_X_VEL = 7.5f // ie target velocity
const val MAX_Y_VEL = 20f

const val INITIAL_BRUSH_SIZE = 1f
const val INITIAL_INTERACT_RADIUS = 32f
const val INITIAL_INSTANT_BREAK = false

private val tmpVec = Vector2()
private val tmpVec2 = Vector2()

val inputMouseLocator = MouseLocator()
private var lastCreateBlockLoc: Long = 0
private var lastCreateBlockTick: Long = 0

fun breakBlocks(worldEntity: WorldEntity, blockX: Int, blockY: Int): Boolean = with(worldEntity) {
  if (canNotInteract(worldEntity, blockX, blockY) || worldEntity.entity.locallyControlledComponentOrNull?.instantBreak == false) return false
  val locallyControlledComponent = entity.locallyControlledComponent
  val breakableBlocks = entity.breakableBlocks(world, blockX, blockY, locallyControlledComponent.brushSize, locallyControlledComponent.interactRadius).asIterable()
  world.removeBlocks(world.getBlocks(breakableBlocks))
  return true
}

fun placeBlocks(worldEntity: WorldEntity, blockX: Int, blockY: Int): Boolean = with(worldEntity) {
  if (canNotInteract(worldEntity, blockX, blockY)) return false
  val world = entity.world
  val material = (entity.selectedInventoryItemComponentOrNull ?: return false).material
  val inventory = entity.inventoryComponentOrNull ?: return false
  val locallyControlledComponent = entity.locallyControlledComponent
  val placeableBlock = entity.placeableBlocks(world, blockX, blockY, locallyControlledComponent.brushSize, locallyControlledComponent.interactRadius).toSet()
  if (inventory.use(material, placeableBlock.size.toUInt())) {
    material.createBlocks(world, placeableBlock)
  }
  return true
}

fun canNotInteract(worldEntity: WorldEntity, blockX: Int, blockY: Int): Boolean = with(worldEntity) {
  val compactLoc = compactLoc(blockX, blockY)
  val tick = world.tick
  if (lastCreateBlockLoc == compactLoc && world.tick - lastCreateBlockTick < WorldBox2DTicker.BOX2D_TPS / 10f) {
    return true
  }
  lastCreateBlockLoc = compactLoc
  lastCreateBlockTick = tick
  return false
}

fun WorldEntity.jump() {
  if (entity.groundedComponent.onGround && Gdx.input.isKeyPressed(Input.Keys.W)) {
    setVel { oldX, _ -> oldX to JUMP_VERTICAL_VEL }
  }
}

fun WorldEntity.setVel(modify: (oldX: Float, oldY: Float) -> (Pair<Float, Float>)) {
  val body = entity.box2dBody
  val vel = body.linearVelocity
  val (nx, ny) = modify(vel.x, vel.y)
  entity.setVelocity(nx, ny)
}

/**
 * @param justPressed Whether to interpolate between placements
 */
fun WorldEntity.interpolate(justPressed: Boolean, action: (WorldEntity, blockX: Int, blockY: Int) -> Boolean): Boolean {
  val blockX = inputMouseLocator.mouseBlockX
  val blockY = inputMouseLocator.mouseBlockY
  val worldX = inputMouseLocator.mouseWorldX
  val worldY = inputMouseLocator.mouseWorldY

  val inSameBlock = inputMouseLocator.previousMouseBlockX == blockX && inputMouseLocator.previousMouseBlockY == blockY
  if (justPressed || inSameBlock) {
    return action(this, blockX, blockY)
  }

  val currPos = tmpVec
  val prevPos = tmpVec2

  currPos.x = worldX
  currPos.y = worldY

  prevPos.x = inputMouseLocator.previousMouseWorldX
  prevPos.y = inputMouseLocator.previousMouseWorldY

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

      val pWx = halfpoint(worldX, inputMouseLocator.previousMouseWorldX, multiplierX)
      val pWy = halfpoint(worldY, inputMouseLocator.previousMouseWorldY, multiplierY)
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
