package no.elg.infiniteBootleg.util

import com.badlogic.gdx.math.Vector2
import com.google.protobuf.TextFormat
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2f
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2i
import no.elg.infiniteBootleg.protobuf.vector2i

fun Vector2.toVector2f(): Vector2f = Vector2f.newBuilder().setX(x).setY(y).build()
fun vector2iOf(x: Int, y: Int): Vector2i = Vector2i.newBuilder().setX(x).setY(y).build()

fun Vector2f.toVector2(): Vector2 = Vector2(x, y)
fun Vector2f.toCompact(): Long = compactLoc(x.toInt(), y.toInt())

fun Vector2i.toCompact(): Long = compactLoc(x, y)

fun Long.toVector2i(): Vector2i =
  vector2i {
    x = this@toVector2i.decompactLocX()
    y = this@toVector2i.decompactLocY()
  }

val singleLinePrinter: TextFormat.Printer by lazy { TextFormat.printer().emittingSingleLine(true) }
