package no.elg.infiniteBootleg.core.world.box2d.extensions

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2AABB
import com.badlogic.gdx.box2d.structs.b2Vec2

val b2AABB.isValid: Boolean get() = Box2d.b2IsValidAABB(this)

fun b2AABB.contains(other: b2AABB): Boolean = Box2d.b2AABB_Contains(this, other)

fun b2AABB.center(): b2Vec2 = Box2d.b2AABB_Center(this)

fun b2AABB.center(out: b2Vec2) {
  Box2d.b2AABB_Center(this, out)
}

fun b2AABB.extents(): b2Vec2 = Box2d.b2AABB_Extents(this)

fun b2AABB.extents(out: b2Vec2) {
  Box2d.b2AABB_Extents(this, out)
}

fun b2AABB.union(other: b2AABB): b2AABB = Box2d.b2AABB_Union(this, other)

fun b2AABB.union(other: b2AABB, out: b2AABB) {
  Box2d.b2AABB_Union(this, other, out)
}

/**
 * Creates an AABB from the given [lowerX] and [lowerY] coordinates and extends it by [offsetX] and [offsetY].
 *
 * @param offsetX How much to offset the upper X coordinate from the lower X coordinate. Must be non-negative
 * @param offsetY How much to offset the upper Y coordinate from the lower Y coordinate. Must be non-negative
 *
 * @throws IllegalArgumentException If the AABB is invalid. It must not contain NaN or infinity. Upper bound must be greater than or equal to lower bound.
 */
fun makeAABBOffset(lowerX: Float, lowerY: Float, offsetX: Float, offsetY: Float): b2AABB = makeAABBDirect(lowerX, lowerY, lowerX + offsetX, lowerY + offsetY)

/**
 * Creates an AABB from the given coordinates. Will throw if the AABB is invalid.
 *
 * @throws IllegalArgumentException If the AABB is invalid. It must not contain NaN or infinity. Upper bound must be greater than or equal to lower bound.
 */
fun makeAABBDirect(lowerX: Number, lowerY: Number, upperX: Number, upperY: Number): b2AABB =
  b2AABB().also {
    val vec2 = makeB2Vec2(lowerX, lowerY)
    it.lowerBound = vec2
    vec2.set(upperX, upperY)
    it.upperBound = vec2
    require(it.isValid) {
      """Invalid AABB! Must not contain NaN or infinity. Upper bound must be greater than or equal to lower bound.
      | lower: ${it.lowerBound.x()}, ${it.lowerBound.y()} upper: ${it.upperBound.x()}, ${it.upperBound.y()}
      | Is X bound valid? ${it.lowerBound.x() <= it.upperBound.x()}
      | Is Y bound valid? ${it.lowerBound.y() <= it.upperBound.y()}
      | lower x: NaN? ${it.lowerBound.x().isNaN()} | infinite? ${it.lowerBound.x().isInfinite()}
      | lower y: NaN? ${it.lowerBound.y().isNaN()} | infinite? ${it.lowerBound.y().isInfinite()}
      | upper x: NaN? ${it.upperBound.x().isNaN()} | infinite? ${it.upperBound.x().isInfinite()}
      | upper y: NaN? ${it.upperBound.y().isNaN()} | infinite? ${it.upperBound.y().isInfinite()}
      """.trimMargin()
    }
  }
