package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2Filter

object Filters {
  private const val GROUND_CATEGORY: Long = 1 shl 0
  private const val FALLING_BLOCK_CATEGORY: Long = 1 shl 1 // sand and torch
  private const val ENTITY_CATEGORY: Long = 1 shl 2
  private const val BLOCK_ENTITY_CATEGORY: Long = 1 shl 3 // door

  val NON_INTERACTIVE__GROUND_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(GROUND_CATEGORY)
    maskBits(0)
  }

  val EN__BLOCK_ENTITY_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(BLOCK_ENTITY_CATEGORY)
    maskBits(ENTITY_CATEGORY)
  }
  val GR_FB_BE__GROUND_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(GROUND_CATEGORY)
    maskBits(GROUND_CATEGORY or FALLING_BLOCK_CATEGORY or BLOCK_ENTITY_CATEGORY)
  }
  val GR_FB_EN_BE__GROUND_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(GROUND_CATEGORY)
    maskBits(GROUND_CATEGORY or FALLING_BLOCK_CATEGORY or ENTITY_CATEGORY or BLOCK_ENTITY_CATEGORY)
  }
  val GR__ENTITY_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(ENTITY_CATEGORY)
    maskBits(GROUND_CATEGORY)
  }
  val GR_EN_BE__ENTITY_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(ENTITY_CATEGORY)
    maskBits(GROUND_CATEGORY or ENTITY_CATEGORY or BLOCK_ENTITY_CATEGORY)
  }
  val GR_FB_BE__FALLING_BLOCK_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(FALLING_BLOCK_CATEGORY)
    maskBits(GROUND_CATEGORY or FALLING_BLOCK_CATEGORY or BLOCK_ENTITY_CATEGORY)
  }
}
