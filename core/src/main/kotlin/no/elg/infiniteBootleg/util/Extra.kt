package no.elg.infiniteBootleg.util

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.fasterxml.uuid.Generators
import ktx.assets.dispose
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.world.world.World
import java.util.UUID
import kotlin.contracts.contract

fun fromUUIDOrNull(string: String?): UUID? {
  return try {
    UUID.fromString(string)
  } catch (e: Exception) {
    null
  }
}

private val namespace = UUID.fromString("1aeeb167-a72e-45d3-8b75-8a144e56ca54")
private val uuidv5Generator = Generators.nameBasedGenerator(namespace)

fun generateUUIDFromString(string: String): UUID {
  return uuidv5Generator.generate(string)
}

fun generateUUIDFromLong(long: Long): UUID {
  return uuidv5Generator.generate(long.toString())
}

fun StringBuilder.fastIntFormat(int: Int, d: Byte): StringBuilder {
  val len = int.stringSize()
  this.ensureCapacity(d.toInt())
  if (len < d) {
    for (i in 1..(d - len)) {
      this.append(' ')
    }
  }
  append(int)
  return this
}

/**
 * Returns the string representation size for a given int value.
 *
 * @param x int value
 * @return string size
 *
 * @implNote There are other ways to compute this: e.g. binary search,
 * but values are biased heavily towards zero, and therefore linear search
 * wins. The iteration results are also routinely inlined in the generated
 * code after loop unrolling.
 */
fun Int.stringSize(): Int {
  var x = this
  var d = 1
  if (x >= 0) {
    d = 0
    x = -x
  }
  var p = -10
  for (i in 1..9) {
    if (x > p) return i + d
    p *= 10
  }
  return 10 + d
}

fun Block?.isAir(markerIsAir: Boolean = true): Boolean {
  contract { returns(false) implies (this@isAir != null) }
  return this == null || (markerIsAir && this.isMarkerBlock()) || this.material == Material.AIR
}

fun Block?.isNotAir(markerIsAir: Boolean = true): Boolean {
  contract { returns(true) implies (this@isNotAir != null) }
  return !this.isAir(markerIsAir)
}

fun Block?.isMarkerBlock(): Boolean = this is EntityMarkerBlock

/**
 * Do an action on a disposable respire then call [Disposable.dispose] on the resource.
 * @param action The action to do on the resource
 * @param onError The action to perform if we failed to dispose the resource
 */
inline fun <T : Disposable, R> T.useDispose(onError: (Exception) -> Unit = {}, action: (T) -> R): R {
  try {
    return action(this)
  } finally {
    this.dispose(onError)
  }
}

inline fun SpriteBatch.withColor(r: Float = this.color.r, g: Float = this.color.g, b: Float = this.color.b, a: Float = this.color.a, action: (SpriteBatch) -> Unit) {
  val oldColor = this.color.cpy()
  this.setColor(r, g, b, a)
  action(this)
  this.color = oldColor
}

fun Entity.interactableBlocks(
  world: World,
  centerBlockX: WorldCoord,
  centerBlockY: WorldCoord,
  radius: Float,
  interactionRadius: Float
): Sequence<Long> {
  val pos = this.position
  return World.getLocationsWithin(centerBlockX, centerBlockY, radius).asSequence()
    .filter { worldLoc: WorldCompactLoc -> world.isChunkLoaded(worldLoc.worldToChunk()) }
    .filter { (worldX, worldY) ->
      ignorePlaceableCheck || (
        isBlockInsideRadius(pos.x, pos.y, worldX, worldY, interactionRadius) &&
          (!Settings.renderLight || world.getBlockLight(worldX, worldY, false)?.isLit ?: true)
        )
    }
}

fun Entity.breakableBlocks(
  world: World,
  centerBlockX: WorldCoord,
  centerBlockY: WorldCoord,
  radius: Float,
  interactionRadius: Float
): Sequence<Long> {
  return interactableBlocks(world, centerBlockX, centerBlockY, radius, interactionRadius).filterNot { world.isAirBlock(it, false) }
}

fun Entity.placeableBlocks(
  world: World,
  centerBlockX: WorldCoord,
  centerBlockY: WorldCoord,
  radius: Float,
  interactionRadius: Float
): Sequence<Long> {
  return interactableBlocks(world, centerBlockX, centerBlockY, radius, interactionRadius)
    .filter { world.isAirBlock(it) }
    .let {
      if (it.any { (worldX, worldY) -> world.canEntityPlaceBlock(worldX, worldY, this) }) {
        it
      } else {
        emptySequence()
      }
    }
}
typealias ChunkColumnFeatureFlag = Int
