package no.elg.infiniteBootleg.client.util

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.client.input.MouseLocator
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.util.JUMP_VERTICAL_VEL
import no.elg.infiniteBootleg.core.util.breakableLocs
import no.elg.infiniteBootleg.core.util.compactInt
import no.elg.infiniteBootleg.core.util.dstd
import no.elg.infiniteBootleg.core.util.placeableBlocks
import no.elg.infiniteBootleg.core.util.worldToBlock
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.Tool
import no.elg.infiniteBootleg.core.world.box2d.extensions.velocity
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent.Companion.groundedComponent
import no.elg.infiniteBootleg.core.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponent
import no.elg.infiniteBootleg.core.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ticker.WorldBox2DTicker
import no.elg.infiniteBootleg.core.world.world.World
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

private val logger = KotlinLogging.logger {}
fun isControlPressed(): Boolean = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)
fun isShiftPressed(): Boolean = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
fun isAltPressed(): Boolean = Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT)

sealed interface WorldEntity {
  val world: World
  val entity: Entity

  data class GenericWorldEntity(override val world: World, override val entity: Entity) : WorldEntity
  data class ClientWorldEntity(override val world: ClientWorld, override val entity: Entity) : WorldEntity
//  data class ServerWorldEntity(override val world: ServerWorld, override val entity: Entity) : WorldEntity
}

private val tmpVec = Vector2()
private val tmpVec2 = Vector2()

val inputMouseLocator = MouseLocator()
private var lastCreateBlockLoc: Long = 0
private var lastCreateBlockTick: Long = 0

fun breakBlocks(worldEntity: WorldEntity, blockX: Int, blockY: Int): Boolean =
  with(worldEntity) {
    val element = (entity.selectedItem ?: return false).element
    if (element is Tool) {
      if (canNotInteract(worldEntity, blockX, blockY) || entity.locallyControlledComponentOrNull?.instantBreak == false) return false
      val locallyControlledComponent = entity.locallyControlledComponent
      val breakableBlocks = entity.breakableLocs(world, blockX, blockY, locallyControlledComponent.brushSize, locallyControlledComponent.interactRadius).asIterable()
      world.removeBlocks(breakableBlocks, entity)
    }
    return true
  }

fun placeBlocks(worldEntity: WorldEntity, blockX: Int, blockY: Int): Boolean =
  with(worldEntity) {
    if (canNotInteract(worldEntity, blockX, blockY)) return false
    val item = entity.selectedItem ?: return false
    val element = item.element

    if (element is Material) {
      val world = entity.world
      val locallyControlledComponent = entity.locallyControlledComponent
      val placeableBlock = entity.placeableBlocks(world, blockX, blockY, locallyControlledComponent.interactRadius).toSet()
      val usages = placeableBlock.size.toUInt()
      val container = entity.containerOrNull ?: return false
      val notRemoved = container.remove(element, usages)
      if (notRemoved > 0u) {
        // Make sure we don't use more items than in the inventory
        placeableBlock.drop(notRemoved.toInt())
      }
      element.createBlocks(world, placeableBlock)
    }
    return true
  }

fun canNotInteract(worldEntity: WorldEntity, blockX: Int, blockY: Int): Boolean =
  with(worldEntity) {
    val compactLoc = compactInt(blockX, blockY)
    val tick = world.tick
    if (lastCreateBlockLoc == compactLoc && world.tick - lastCreateBlockTick < WorldBox2DTicker.BOX2D_TPS / 10f) {
      return true
    }
    lastCreateBlockLoc = compactLoc
    lastCreateBlockTick = tick
    return false
  }

fun WorldEntity.jump() {
  if (entity.groundedComponent.onGround) {
    setVel { oldX, _ -> oldX to JUMP_VERTICAL_VEL }
  }
}

fun WorldEntity.setVel(modify: (oldX: Float, oldY: Float) -> (Pair<Float, Float>)) {
  val body = entity.box2dBody
  val vel = body.velocity
  val (nx, ny) = modify(vel.x(), vel.y())
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
      logger.info { "---START SMOOTH PLACEMENT---" }
      logger.info { "(pos) prev: $prevPos, curr $currPos" }
      logger.info { "(distance) $distance" }
      logger.info { "Doing $iterations iterations of interpolation" }
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
      val pBx = pWx.worldToBlock()
      val pBy = pWy.worldToBlock()

      if (logging) {
        logger.info { "--inter $i mltX: $multiplierX | mltY: $multiplierY, pBx:$pBx | pBy:$pBy | pWx:$pWx | pWy:$pWy" }
      }
      update = update or action(this, pBx, pBy)
    }

    if (logging) {
      logger.info { "---END SMOOTH PLACEMENT (update? $update)---" }
    }
  }

  return update or action(this, blockX, blockY)
}
