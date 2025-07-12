package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.box2d.structs.b2Vec2
import no.elg.infiniteBootleg.core.util.Compacted2Float
import no.elg.infiniteBootleg.core.util.Compacted2Int
import no.elg.infiniteBootleg.core.util.compactFloat
import no.elg.infiniteBootleg.core.util.compactInt

fun b2Vec2.set(x: Float, y: Float): b2Vec2 =
  apply {
    x(x)
    y(y)
  }

fun b2Vec2.compactToFloat(): Compacted2Float = compactFloat(x(), y())

fun b2Vec2.compactToInt(): Compacted2Int = compactInt(x().toInt(), y().toInt())
