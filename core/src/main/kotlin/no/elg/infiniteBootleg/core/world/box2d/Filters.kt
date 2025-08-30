package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2Filter

object Filters {
  private const val GROUND_CATEGORY: Long = 0x1
  private const val FALLING_BLOCK_CATEGORY: Long = 0x2
  private const val ENTITY_CATEGORY: Long = 0x4

  val NON_INTERACTIVE__GROUND_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(GROUND_CATEGORY)
    maskBits(0)
  }

  val EN__GROUND_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(GROUND_CATEGORY)
    maskBits(ENTITY_CATEGORY)
  }
  val GR_FB__GROUND_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(GROUND_CATEGORY)
    maskBits(GROUND_CATEGORY or FALLING_BLOCK_CATEGORY)
  }
  val GR_FB_EN__GROUND_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(GROUND_CATEGORY)
    maskBits(GROUND_CATEGORY or FALLING_BLOCK_CATEGORY or ENTITY_CATEGORY)
  }
  val GR__ENTITY_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(ENTITY_CATEGORY)
    maskBits(GROUND_CATEGORY)
  }
  val GR_EN__ENTITY_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(ENTITY_CATEGORY)
    maskBits(GROUND_CATEGORY or ENTITY_CATEGORY)
  }
  val GR_FB__FALLING_BLOCK_FILTER: b2Filter = Box2d.b2DefaultFilter().apply {
    categoryBits(FALLING_BLOCK_CATEGORY)
    maskBits(GROUND_CATEGORY or FALLING_BLOCK_CATEGORY)
  }
}
