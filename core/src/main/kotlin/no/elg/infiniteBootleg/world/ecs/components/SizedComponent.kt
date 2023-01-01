package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import ktx.ashley.Mapper
import no.elg.infiniteBootleg.world.Block

/**
 * One unit is [Block.BLOCK_SIZE]
 *
 * @param width  The height of this entity in world view
 * @param height The width of this entity in world view
 */
data class SizedComponent(val width: Float, val height: Float) : Component {

  val halfBox2dWidth: Float get() = width / (Block.BLOCK_SIZE * 2f)

  val halfBox2dHeight: Float get() = height / (Block.BLOCK_SIZE * 2f)

  companion object : Mapper<SizedComponent>()
}
