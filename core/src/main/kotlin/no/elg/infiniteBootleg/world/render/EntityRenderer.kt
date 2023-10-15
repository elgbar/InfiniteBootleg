package no.elg.infiniteBootleg.world.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.worldToScreen
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent.Companion.textureRegionComponent
import no.elg.infiniteBootleg.world.ecs.drawableEntitiesFamily
import no.elg.infiniteBootleg.world.world.ClientWorld
import kotlin.math.roundToInt

class EntityRenderer(private val worldRender: ClientWorldRender) : Renderer {

  private val entities: ImmutableArray<Entity> = worldRender.world.engine.getEntitiesFor(drawableEntitiesFamily)

  private val layout = GlyphLayout()

  override fun render() {
    val batch: Batch = worldRender.batch
    val world: ClientWorld = worldRender.world
    val worldBody = world.worldBody
    val worldOffsetX = worldBody.worldOffsetX
    val worldOffsetY = worldBody.worldOffsetY
    for (entity in entities) {
      val textureRegion = entity.textureRegionComponent.texture
      val box2d = entity.box2d
      val centerPos = entity.box2dBody.position
      val worldX = centerPos.x - box2d.halfBox2dWidth
      val worldY = centerPos.y - box2d.halfBox2dHeight
      var lightX = 0f
      var lightY = 0f
      if (Settings.renderLight) {
        val blockX = (centerPos.x - box2d.halfBox2dWidth / 2).roundToInt()
        val blockY = centerPos.y.roundToInt()
        val topX = world.getTopBlockWorldY(blockX, BLOCKS_LIGHT_FLAG)
        if (blockY > topX) {
          lightX = worldToScreen(blockX.toFloat(), worldOffsetX)
          lightY = worldToScreen((topX + 1).toFloat(), worldOffsetY)
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
            lightX = worldToScreen(blockX.toFloat(), worldOffsetX)
            lightY = worldToScreen(blockY.toFloat(), worldOffsetY)
          }
        }
      }
      val screenX = worldToScreen(worldX, worldOffsetX)
      val screenY = worldToScreen(worldY, worldOffsetY)

      val lookDirectionOrNull = entity.lookDirectionComponentOrNull
      val texture = textureRegion.textureRegion
      val shouldFlipX = lookDirectionOrNull != null && ((lookDirectionOrNull.direction.dx < 0 && texture.isFlipX) || (lookDirectionOrNull.direction.dx > 0 && !texture.isFlipX))

      texture.flip(shouldFlipX, false)
      batch.draw(texture, screenX, screenY, box2d.worldWidth, box2d.worldHeight)
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
}
