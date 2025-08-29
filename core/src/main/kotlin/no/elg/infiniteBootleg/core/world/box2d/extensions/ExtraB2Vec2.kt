package no.elg.infiniteBootleg.core.world.box2d.extensions

import com.badlogic.gdx.box2d.structs.b2Vec2
import no.elg.infiniteBootleg.core.util.Compacted2Float
import no.elg.infiniteBootleg.core.util.Compacted2Int
import no.elg.infiniteBootleg.core.util.compactFloat
import no.elg.infiniteBootleg.core.util.compactInt
import no.elg.infiniteBootleg.core.util.decompactLocXf
import no.elg.infiniteBootleg.core.util.decompactLocYf

fun makeB2Vec2(x: Number, y: Number): b2Vec2 = makeB2Vec2(x.toFloat(), y.toFloat())

fun makeB2Vec2(x: Float, y: Float): b2Vec2 =
  b2Vec2().apply {
    this.x(x)
    this.y(y)
  }

fun b2Vec2.set(x: Float, y: Float): b2Vec2 =
  apply {
    x(x)
    y(y)
  }

fun b2Vec2.compactToFloat(): Compacted2Float = compactFloat(x(), y())

fun b2Vec2.compactToInt(): Compacted2Int = compactInt(x().toInt(), y().toInt())

operator fun b2Vec2.component1(): Float = x()
operator fun b2Vec2.component2(): Float = y()

inline val b2Vec2.x get() = x()
inline val b2Vec2.y get() = y()

fun Compacted2Float.tob2Vec2(): b2Vec2 = b2Vec2().set(x = this.decompactLocXf(), y = this.decompactLocYf())
