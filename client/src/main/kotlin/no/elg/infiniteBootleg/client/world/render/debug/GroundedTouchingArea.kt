package no.elg.infiniteBootleg.client.world.render.debug

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Batch
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent.Companion.groundedComponent
import no.elg.infiniteBootleg.core.world.ecs.controlledEntityFamily
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion

class GroundedTouchingArea(worldRender: ClientWorldRender) : SingleBlockDebugRenderer<List<GroundedComponent>>(worldRender) {

  override val alpha: Float get() = 1f
  override val texture: RotatableTextureRegion
    get() = ClientMain.inst().assets.whiteTexture

  override fun beforeAllRender(batch: Batch): List<GroundedComponent>? {
    val entities: Array<Entity> = ClientMain.inst().engine?.getEntitiesFor(controlledEntityFamily)?.toArray<Entity>(Entity::class.java) ?: return null
    return if (entities.isEmpty()) null else entities.map { it.groundedComponent }
  }

  override fun shouldRender(block: Block, data: List<GroundedComponent>): Boolean {
    val blockPos = block.compactWorldLoc
    val any = data.any { groundedComponent -> blockPos in groundedComponent }
    return any
  }

  override fun beforeRender(block: Block, batch: Batch, data: List<GroundedComponent>) {
  }

  override val isActive: Boolean
    get() = Settings.renderGroundedTouchingBlocks
}
