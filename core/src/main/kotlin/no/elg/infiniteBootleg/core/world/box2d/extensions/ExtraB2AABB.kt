package no.elg.infiniteBootleg.core.world.box2d.extensions

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2AABB
import com.badlogic.gdx.box2d.structs.b2Vec2

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

fun makeB2AABB(lowerX: Number, lowerY: Number, upperX: Number, upperY: Number): b2AABB =
  b2AABB().also {
    val vec2 = makeB2Vec2(lowerX, lowerY)
    it.lowerBound = vec2
    vec2.set(upperX, upperY)
    it.upperBound = vec2
  }
