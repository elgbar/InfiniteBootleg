package no.elg.infiniteBootleg.world.box2d

import com.badlogic.gdx.physics.box2d.Filter
import kotlin.experimental.or

object Filters {
  private const val GROUND_CATEGORY: Short = 0x1
  private const val FALLING_BLOCK_CATEGORY: Short = 0x2
  private const val ENTITY_CATEGORY: Short = 0x4

  val NON_INTERACTIVE__GROUND_FILTER = Filter().apply {
    categoryBits = GROUND_CATEGORY
    maskBits = 0
  }

  val EN__GROUND_FILTER = Filter().apply {
    categoryBits = GROUND_CATEGORY
    maskBits = ENTITY_CATEGORY
  }
  val GR_FB__GROUND_FILTER = Filter().apply {
    categoryBits = GROUND_CATEGORY
    maskBits = (GROUND_CATEGORY or FALLING_BLOCK_CATEGORY)
  }
  val GR_FB_EN__GROUND_FILTER = Filter().apply {
    categoryBits = GROUND_CATEGORY
    maskBits = GROUND_CATEGORY or FALLING_BLOCK_CATEGORY or ENTITY_CATEGORY
  }
  val GR__ENTITY_FILTER = Filter().apply {
    categoryBits = ENTITY_CATEGORY
    maskBits = GROUND_CATEGORY
  }
  val GR_EN_ENTITY_FILTER = Filter().apply {
    categoryBits = ENTITY_CATEGORY
    maskBits = GROUND_CATEGORY or ENTITY_CATEGORY
  }
  val GR_FB__FALLING_BLOCK_FILTER = Filter().apply {
    categoryBits = FALLING_BLOCK_CATEGORY
    maskBits = GROUND_CATEGORY or FALLING_BLOCK_CATEGORY
  }
}
