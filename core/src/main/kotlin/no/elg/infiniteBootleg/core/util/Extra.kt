@file:Suppress("NOTHING_TO_INLINE")

package no.elg.infiniteBootleg.core.util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.fasterxml.uuid.Generators
import ktx.assets.dispose
import ktx.graphics.begin
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.EntityMarkerBlock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun fromUUIDOrNull(string: String?): UUID? =
  try {
    UUID.fromString(string)
  } catch (e: Exception) {
    null
  }

private val namespace = UUID.fromString("1aeeb167-a72e-45d3-8b75-8a144e56ca54")
private val uuidv5Generator = Generators.nameBasedGenerator(namespace)

/**
 * Uniform handling of string to seed (i.e., long) conversion.
 */
fun String.asWorldSeed(): Long = hashCode().toLong()

fun generateUUIDFromString(string: String): UUID = uuidv5Generator.generate(string)

fun generateUUIDFromLong(long: Long): UUID = uuidv5Generator.generate(long.toString())

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

inline fun Block?.isAir(markerIsAir: Boolean = true): Boolean {
  contract { returns(false) implies (this@isAir != null) }
  return this == null || (markerIsAir && this.isMarkerBlock()) || this.material == Material.Air
}

inline fun Block?.isNotAir(markerIsAir: Boolean = true): Boolean {
  contract { returns(true) implies (this@isNotAir != null) }
  return !this.isAir(markerIsAir)
}

inline fun Block?.isMarkerBlock(): Boolean {
  contract { returns(true) implies (this@isMarkerBlock is EntityMarkerBlock) }
  return this is EntityMarkerBlock
}

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

inline fun Batch.withColor(
  r: Float = this.color.r,
  g: Float = this.color.g,
  b: Float = this.color.b,
  a: Float = this.color.a,
  crossinline action: (Batch) -> Unit
) {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  val oldColor = this.color.cpy()
  this.setColor(r, g, b, a)
  action(this)
  this.color = oldColor
}

inline fun BitmapFont.withColor(
  r: Float = this.color.r,
  g: Float = this.color.g,
  b: Float = this.color.b,
  a: Float = this.color.a,
  crossinline action: (BitmapFont) -> Unit
) {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  val oldColor = this.color.cpy()
  this.setColor(r, g, b, a)
  action(this)
  this.color = oldColor
}

typealias ChunkColumnFeatureFlag = Int

/**
 * Automatically calls [Batch.begin] and [Batch.end]. If the batch is already drawing, it is ended before the action.
 * @param projectionMatrix A projection matrix to set on the batch before [Batch.begin]. If null, the batch's matrix
 * remains unchanged.
 * @param action inlined. Executed after [Batch.begin] and before [Batch.end].
 */
inline fun <B : Batch> B.safeUse(projectionMatrix: Matrix4? = null, action: (B) -> Unit) {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  if (projectionMatrix != null) {
    this.projectionMatrix = projectionMatrix
  }
  if (!isDrawing) {
    begin()
  }
  try {
    action(this)
  } finally {
    if (isDrawing) {
      end()
    }
  }
}

/**
 * Automatically calls [ShapeRenderer.begin] and [ShapeRenderer.end].
 * @param type specified shape type used to draw the shapes in the [action] block. Can be changed during the rendering
 * with [ShapeRenderer.set].
 * @param projectionMatrix A projection matrix to set on the ShapeRenderer before [ShapeRenderer.begin]. If null, the ShapeRenderer's matrix
 * remains unchanged.
 * @param action inlined. Executed after [ShapeRenderer.begin] and before [ShapeRenderer.end].
 */
inline fun <SR : ShapeRenderer> SR.safeUse(type: ShapeRenderer.ShapeType, projectionMatrix: Matrix4? = null, action: (SR) -> Unit) {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  if (projectionMatrix != null) {
    this.projectionMatrix = projectionMatrix
  }
  if (isDrawing) {
    end()
  }
  begin(type)
  try {
    action(this)
  } finally {
    end()
  }
}

/**
 * Blend two colors together
 *
 * @param from the color to blend from, i.e. the color at progress 0
 * @param to the color to blend to, i.e. the color at progress 1
 * @param progress the progress between the two colors, expected to be between 0 and 1
 *
 * @return the same instance as this
 */
fun Color.blend(from: Color, to: Color, progress: Float): Color =
  this.set(
    from.r * progress + to.r * (1f - progress),
    from.g * progress + to.g * (1f - progress),
    from.b * progress + to.b * (1f - progress),
    from.a * progress + to.a * (1f - progress)
  )

fun <K, T> Iterable<T>.partitionMap(toKey: (T) -> K): Map<K, List<T>> =
  buildMap<K, MutableList<T>> {
    for (item: T in this@partitionMap) {
      val list = this@buildMap.getOrPut(toKey(item)) { mutableListOf() }
      list += item
    }
  }

fun <K, T> Iterable<T>.partitionCount(toKey: (T) -> K): Map<K, Int> =
  buildMap<K, Int> {
    for (item: T in this@partitionCount) {
      val key = toKey(item)
      val current = this@buildMap.getOrDefault(key, 0)
      this@buildMap.put(key, current + 1)
    }
  }

/**
 * @return A string of the time between [start] and [end] in the format `<duration in seconds>.<millis part of duration>s`, e.g. `1.234s`
 */
fun diffTimePretty(start: Instant, end: Instant = Instant.now()): String = Duration.between(start, end).run { "${toSeconds()}.${toMillisPart()}s" }
