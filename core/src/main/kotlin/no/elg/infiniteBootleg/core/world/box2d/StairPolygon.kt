package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2Polygon
import com.badlogic.gdx.box2d.structs.b2Vec2
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.world.blocks.BlockShape
import no.elg.infiniteBootleg.core.world.box2d.extensions.makeB2Vec2

/**
 * Build a right-triangle [b2Polygon] for a [BlockShape] stair variant. The triangle is
 * positioned in the chunk's local coordinate space — the block's 1x1 cell occupies
 * `(localX..localX+1, localY..localY+1)`.
 *
 * Vertices are wound CCW (Box2D requirement for a positive-area polygon).
 */
fun makeStairTrianglePolygon(localX: LocalCoord, localY: LocalCoord, shape: BlockShape): b2Polygon {
  require(shape.isStair) { "Shape $shape is not a stair shape" }
  val ox = localX - 0.5f
  val oy = localY - 0.5f
  val (ax, ay, bx, by, cx, cy) = stairTriangleVerticesCCW(shape)
  val points = b2Vec2.b2Vec2Pointer(3, true)
  points.set(makeB2Vec2(ox + ax, oy + ay), 0)
  points.set(makeB2Vec2(ox + bx, oy + by), 1)
  points.set(makeB2Vec2(ox + cx, oy + cy), 2)
  val hull = Box2d.b2ComputeHull(points, 3)
  return Box2d.b2MakePolygon(hull.asPointer(), 0f)
}

private fun stairTriangleVerticesCCW(shape: BlockShape): FloatArray =
  when (shape) {
    BlockShape.STAIR_NE -> floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)
    BlockShape.STAIR_NW -> floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f)
    BlockShape.STAIR_SE -> floatArrayOf(0f, 0f, 1f, 1f, 0f, 1f)
    BlockShape.STAIR_SW -> floatArrayOf(1f, 0f, 1f, 1f, 0f, 1f)
    BlockShape.FULL -> error("FULL is not a stair")
  }

private operator fun FloatArray.component1(): Float = this[0]
private operator fun FloatArray.component2(): Float = this[1]
private operator fun FloatArray.component3(): Float = this[2]
private operator fun FloatArray.component4(): Float = this[3]
private operator fun FloatArray.component5(): Float = this[4]
private operator fun FloatArray.component6(): Float = this[5]
