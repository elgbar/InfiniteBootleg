package no.elg.infiniteBootleg.client.world.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.ecs.components.transients.RotatableTextureRegionComponent.Companion.rotatableTextureRegion
import no.elg.infiniteBootleg.client.world.ecs.system.FollowEntitySystem
import no.elg.infiniteBootleg.client.world.textureRegion
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.Settings.renderBox2dEntityDifference
import no.elg.infiniteBootleg.core.api.Renderer
import no.elg.infiniteBootleg.core.util.safeUse
import no.elg.infiniteBootleg.core.util.worldToScreen
import no.elg.infiniteBootleg.core.world.ContainerElement
import no.elg.infiniteBootleg.core.world.Staff
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.box2d.degrees
import no.elg.infiniteBootleg.core.world.box2d.extensions.position
import no.elg.infiniteBootleg.core.world.box2d.extensions.rotation
import no.elg.infiniteBootleg.core.world.box2d.extensions.x
import no.elg.infiniteBootleg.core.world.box2d.extensions.y
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent.Companion.groundedComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.MaterialComponent.Companion.materialComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.TintedComponent.Companion.tintedComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.EFFECTIVE_ZERO
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.selectedElement
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FollowedByCameraTag.Companion.followedByCamera
import no.elg.infiniteBootleg.core.world.ecs.components.transients.LastSpellCastComponent.Companion.lastSpellCastOrNull
import no.elg.infiniteBootleg.core.world.ecs.drawableEntitiesFamily
import no.elg.infiniteBootleg.core.world.magic.SpellState.Companion.canNotCastAgain
import no.elg.infiniteBootleg.core.world.magic.SpellState.Companion.timeToCast
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import kotlin.math.abs
import kotlin.math.roundToInt

class EntityRenderer(private val worldRender: ClientWorldRender) : Renderer {

  private val entities: ImmutableArray<Entity> = worldRender.world.engine.getEntitiesFor(drawableEntitiesFamily)

  private val nameLayout = GlyphLayout()
  private val lightVector: Vector2 = Vector2()

  private val shapeRenderer: ShapeRenderer = ShapeRenderer().also {
    it.color = Color.GREEN
    it.setAutoShapeType(true)
  }

  private val tmpColor = Color()

  private val batch: Batch
    get() = worldRender.batch
  private val world: ClientWorld
    get() = worldRender.world

  private fun Entity.currentTexture(): TextureRegion {
    val lookDirectionOrNull = lookDirectionComponentOrNull
    val velocityOrNull = velocityOrNull
    val rotatableTextureRegion: RotatableTextureRegion = if (box2d.type == ProtoWorld.Entity.Box2D.BodyType.PLAYER && velocityOrNull != null) {
      if (velocityOrNull.isZero(EFFECTIVE_ZERO)) {
        ClientMain.inst().assets.playerIdleTextures.getKeyFrame(globalAnimationTimer)
      } else if (abs(velocityOrNull.x) > EFFECTIVE_ZERO && groundedComponentOrNull?.onGround == true) {
        ClientMain.inst().assets.playerWalkingTextures.getKeyFrame(globalAnimationTimer)
      } else {
        ClientMain.inst().assets.playerTexture
      }
    } else {
      this.rotatableTextureRegion
    }
    val textureRegion: TextureRegion = rotatableTextureRegion.textureRegion
    val shouldFlipX =
      lookDirectionOrNull != null && ((lookDirectionOrNull.direction.dx < 0 && textureRegion.isFlipX) || (lookDirectionOrNull.direction.dx > 0 && !textureRegion.isFlipX))
    textureRegion.flip(shouldFlipX, false)
    return textureRegion
  }

  private fun setupEntityLight(centerPos: Vector2, box2d: Box2DBodyComponent) {
    if (Settings.renderLight) {
      val blockX = (centerPos.x - box2d.halfBox2dWidth / 2).roundToInt()
      val blockY = centerPos.y.roundToInt()
      val topX = world.getTopBlockWorldY(blockX, ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG)
      if (blockY > topX) {
        lightVector.set(blockX.worldToScreen(), (topX + 1).worldToScreen())
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
          lightVector.set(blockX.worldToScreen(), blockY.worldToScreen())
        } else {
          lightVector.setZero()
          batch.color = Color.WHITE
        }
      }
    } else {
      lightVector.setZero()
      batch.color = Color.WHITE
    }
  }

  fun drawBox2d(box2d: Box2DBodyComponent, texture: TextureRegion, screenX: Float, screenY: Float) {
    if (box2d.fixedRotation) {
      batch.draw(texture, screenX, screenY, box2d.worldWidth, box2d.worldHeight)
    } else {
      batch.draw(
        texture, screenX, screenY, box2d.worldWidth / 2f, box2d.worldHeight / 2f, box2d.worldWidth, box2d.worldHeight, 1f, 1f, box2d.body.rotation.degrees
      )
    }
  }

  fun drawHolding(entity: Entity, holding: ContainerElement?, screenX: Float, screenY: Float) {
    if (holding == null) return
    val size = Block.BLOCK_TEXTURE_SIZE / 2f
    val holdingTexture = holding.textureRegion?.textureRegionOrNull
    if (holdingTexture != null) {
      val ratio = holdingTexture.regionWidth.toFloat() / holdingTexture.regionHeight.toFloat()
      batch.draw(holdingTexture, screenX, screenY, size, size * ratio)
    }

    if (holding is Staff) {
      val lastSpellCaste = entity.lastSpellCastOrNull
      if (lastSpellCaste.canNotCastAgain()) {
        batch.end() // End the batch to draw the shape renderer
        shapeRenderer.safeUse(ShapeRenderer.ShapeType.Line, batch.projectionMatrix) {
          val width = size
          val height = size / 2f
          val x = size + screenX + 1 // place to the right of the held item, +1 to avoid overlap
          shapeRenderer.rect(x, screenY, width, height)
          shapeRenderer.set(ShapeRenderer.ShapeType.Filled)
          val percent = 1 - lastSpellCaste.timeToCast() / lastSpellCaste.castDelay
          shapeRenderer.rect(x, screenY, width * percent.toFloat(), height)
        }
        batch.begin()
      }
    }
  }

  fun drawName(entity: Entity, box2d: Box2DBodyComponent, screenX: Float, screenY: Float) {
    entity.nameOrNull?.let { name ->
      val font = ClientMain.inst().assets.font10pt
      nameLayout.setText(font, name, batch.color, 0f, Align.center, false)
      font.draw(batch, nameLayout, screenX + box2d.worldWidth / 2f, screenY + box2d.worldHeight + font.capHeight + font.lineHeight / 2f)
    }
  }

  fun debugEntityLight() {
    if (Settings.debugEntityLight) {
      batch.color = Color.WHITE // Make sure we can see the debug light
      val size = Block.BLOCK_TEXTURE_SIZE / 4f // The size of the debug cube
      val offset = Block.BLOCK_TEXTURE_SIZE / 2f - size / 2f
      batch.draw(ClientMain.inst().assets.whiteTexture.textureRegion, lightVector.x + offset, lightVector.y + offset, size, size)
    }
  }

  @Suppress("GDXKotlinFlushInsideLoop")
  override fun render() {
    if (!world.worldTicker.isPaused) {
      globalAnimationTimer += Gdx.graphics.deltaTime
    }
    for (entity in entities) {
      val box2d: Box2DBodyComponent = entity.box2d

      @Suppress("GDXKotlinFlushInsideLoop")
      val centerPos: Vector2 = if (entity.followedByCamera) {
        // We must render the entity we're following at the same position as the last calculated position, otherwise we'll get a weird jitter effect
        // Also to make camera is correct we must update the projection matrix again
        batch.projectionMatrix = worldRender.camera.combined
        FollowEntitySystem.processedPosition
      } else {
        entity.position
      }

      val texture = entity.currentTexture()

      if (renderBox2dEntityDifference) {
        batch.color = ASHLEY_COLOR
        val position = box2d.body.position
        val ashleyScreenX = (position.x - box2d.halfBox2dWidth).worldToScreen()
        val ashleyScreenY = (position.y - box2d.halfBox2dHeight).worldToScreen()
        drawBox2d(box2d, texture, ashleyScreenX, ashleyScreenY)

        batch.color = BOX2D_COLOR
        val screenX = (centerPos.x - box2d.halfBox2dWidth).worldToScreen()
        val screenY = (centerPos.y - box2d.halfBox2dHeight).worldToScreen()
        drawBox2d(box2d, texture, screenX, screenY)

        batch.color = Color.WHITE
      }

      val activeScreenX: Float
      val activeScreenY: Float
      if (entity.materialComponentOrNull != null) {
        val position = box2d.body.position
        activeScreenX = (position.x - box2d.halfBox2dWidth).worldToScreen()
        activeScreenY = (position.y - box2d.halfBox2dHeight).worldToScreen()
      } else {
        activeScreenX = (centerPos.x - box2d.halfBox2dWidth).worldToScreen()
        activeScreenY = (centerPos.y - box2d.halfBox2dHeight).worldToScreen()
      }

      setupEntityLight(centerPos, box2d)

      // Draw with a tint if there is one
      entity.tintedComponentOrNull?.also {
        batch.color = tmpColor.set(batch.color).mul(it.tint)
      }
      drawBox2d(box2d, texture, activeScreenX, activeScreenY)
      drawHolding(entity, entity.selectedElement, activeScreenX, activeScreenY)
      drawName(entity, box2d, activeScreenX, activeScreenY)
      debugEntityLight()
    }
  }

  companion object {
    var globalAnimationTimer = 0f
    val ASHLEY_COLOR = Color(1f, 0f, 0f, 0.5f)
    val BOX2D_COLOR = Color(0f, 0f, 1f, 0.5f)
  }
}
