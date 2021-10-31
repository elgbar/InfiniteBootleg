package no.elg.infiniteBootleg.util

import com.badlogic.gdx.math.Vector2
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2f

fun Vector2.toVector2f(): Vector2f = Vector2f.newBuilder().setX(x).setY(y).build()

fun Vector2f.toVector2(): Vector2 = Vector2(x, y)
