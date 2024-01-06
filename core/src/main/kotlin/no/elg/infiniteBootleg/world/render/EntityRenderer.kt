package no.elg.infiniteBootleg.world.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Box2D
import no.elg.infiniteBootleg.util.toDegrees
import no.elg.infiniteBootleg.util.worldToScreen
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.groundedComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedInventoryItemComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent.Companion.textureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.FollowedByCameraTag.Companion.followedByCamera
import no.elg.infiniteBootleg.world.ecs.drawableEntitiesFamily
import no.elg.infiniteBootleg.world.ecs.system.client.FollowEntitySystem
import no.elg.infiniteBootleg.world.world.ClientWorld
import kotlin.math.abs
import kotlin.math.roundToInt

class EntityRenderer(private val worldRender: ClientWorldRender) : Renderer {

  private val entities: ImmutableArray<Entity> = worldRender.world.engine.getEntitiesFor(drawableEntitiesFamily)

  private val layout = GlyphLayout()

  private val batch: Batch
    get() = worldRender.batch
  private val world: ClientWorld
    get() = worldRender.world

  private fun Entity.currentTexture(): TextureRegion {
    val lookDirectionOrNull = lookDirectionComponentOrNull
    val velocityOrNull = velocityOrNull
    val texture: TextureRegion =
      if (box2d.type == Box2D.BodyType.PLAYER && velocityOrNull != null) {
        if (velocityOrNull.isZero(EFFECTIVE_ZERO)) {
          Main.inst().assets.playerIdleTextures.getKeyFrame(globalAnimationTimer).textureRegion
        } else if (abs(velocityOrNull.x) > EFFECTIVE_ZERO && groundedComponentOrNull?.onGround == true) {
          Main.inst().assets.playerWalkingTextures.getKeyFrame(globalAnimationTimer).textureRegion
        } else {
          textureRegionComponent.texture.textureRegion
        }
      } else {
        textureRegionComponent.texture.textureRegion
      }
    val shouldFlipX = lookDirectionOrNull != null && ((lookDirectionOrNull.direction.dx < 0 && texture.isFlipX) || (lookDirectionOrNull.direction.dx > 0 && !texture.isFlipX))
    texture.flip(shouldFlipX, false)
    return texture
  }

  private fun Entity.holdingTexture(): TextureRegion? {
    return selectedInventoryItemComponentOrNull?.element?.textureRegion?.textureRegionOrNull
  }


  fun Batch.drawBox2d(box2d: Box2DBodyComponent, texture: TextureRegion, screenX: Float, screenY: Float) {
    if (box2d.body.isFixedRotation) {
      draw(texture, screenX, screenY, box2d.worldWidth, box2d.worldHeight)
    } else {
      draw(
        texture,
        screenX,
        screenY,
        box2d.worldWidth / 2f,
        box2d.worldHeight / 2f,
        box2d.worldWidth,
        box2d.worldHeight,
        1f,
        1f,
        box2d.body.angle.toDegrees()
      )
    }
  }

  override fun render() {
    globalAnimationTimer += Gdx.graphics.deltaTime
    for (entity in entities) {
      val box2d: Box2DBodyComponent = entity.box2d

      @Suppress("GDXKotlinFlushInsideLoop")
      val centerPos: Vector2 = if (entity.followedByCamera) {
        // We must render the entity we're following at the same position as the last calculated position, otherwise we'll get a weird jitter effect
        // Also to make camera is correct we must update the projection matrix again
        batch.projectionMatrix = worldRender.camera.combined
        FollowEntitySystem.processedPosition
      } else {
        entity.box2dBody.position
      }
      val worldX = centerPos.x - box2d.halfBox2dWidth
      val worldY = centerPos.y - box2d.halfBox2dHeight
      var lightX = 0f
      var lightY = 0f
      if (Settings.renderLight) {
        val blockX = (centerPos.x - box2d.halfBox2dWidth / 2).roundToInt()
        val blockY = centerPos.y.roundToInt()
        val topX = world.getTopBlockWorldY(blockX, BLOCKS_LIGHT_FLAG)
        if (blockY > topX) {
          lightX = worldToScreen(blockX.toFloat())
          lightY = worldToScreen((topX + 1).toFloat())
          batch.color = Color.WHITE
        } else {
          val blockLight = world.getBlockLight(blockX, blockY, false)
          if (blockLight != null) {
            if (blockLight.isSkylight) {
              batch.color = Color.WHITE
            } else if (blockLight.isLit) {
              val v = blockLight.averageBrightness
              batch.setColor(v, v, v, 1f)
            } else {
              batch.color = Color.BLACK
            }
            lightX = worldToScreen(blockX.toFloat())
            lightY = worldToScreen(blockY.toFloat())
          }
        }
      }

      val screenX = worldToScreen(worldX)
      val screenY = worldToScreen(worldY)

      batch.drawBox2d(box2d, entity.currentTexture(), screenX, screenY)

      entity.holdingTexture()?.also { holding ->
        val size = Block.BLOCK_SIZE / 2f
        val ratio = holding.regionWidth.toFloat() / holding.regionHeight.toFloat()
        batch.draw(holding, screenX, screenY, size, size * ratio)
      }
      batch.color = Color.WHITE
      if (Settings.debugEntityLight) {
        val size = Block.BLOCK_SIZE / 4f // The size of the debug cube
        val offset = Block.BLOCK_SIZE / 2f - size / 2f
        batch.draw(Main.inst().assets.whiteTexture.textureRegion, lightX + offset, lightY + offset, size, size)
      }

      entity.nameOrNull?.let { name ->
        val font = Main.inst().assets.font10pt
        layout.setText(font, name, Color.WHITE, 0f, Align.center, false)
        font.draw(batch, layout, screenX + box2d.worldWidth / 2f, screenY + box2d.worldHeight + font.capHeight + font.lineHeight / 2f)
      }
    }
    batch.color = Color.WHITE
  }

  companion object {
    const val EFFECTIVE_ZERO = 0.01f
    var globalAnimationTimer = 0f
  }
}
