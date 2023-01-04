package no.elg.infiniteBootleg.world.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
import no.elg.infiniteBootleg.world.ClientWorld
import no.elg.infiniteBootleg.world.blocks.TntBlock.Companion.whiteTexture
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent.Companion.textureRegion
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.drawableEntitiesFamily
import kotlin.math.roundToInt

class EntityRenderer(private val worldRender: ClientWorldRender) : Renderer {

  override fun render() {
    val batch: Batch = worldRender.batch
    val world: ClientWorld = worldRender.world
    val worldBody = world.worldBody
    val worldOffsetX = worldBody.worldOffsetX
    val worldOffsetY = worldBody.worldOffsetY
    for (entity in world.engine.getEntitiesFor(drawableEntitiesFamily)) {
      val textureRegion = entity.textureRegion.texture
      val centerPos = entity.position
      val box2d = entity.box2d
      val worldX = centerPos.x - box2d.halfBox2dWidth
      val worldY = centerPos.y - box2d.halfBox2dHeight
      var lightX = 0f
      var lightY = 0f
      if (Settings.renderLight) {
        val blockX = (centerPos.x - box2d.halfBox2dWidth / 2).roundToInt()
        val blockY = centerPos.y.roundToInt()
        val topX = world.getTopBlockWorldY(blockX, BLOCKS_LIGHT_FLAG)
        if (blockY > topX) {
          lightX = CoordUtil.worldToScreen(blockX.toFloat(), worldOffsetX)
          lightY = CoordUtil.worldToScreen((topX + 1).toFloat(), worldOffsetY)
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
            lightX = CoordUtil.worldToScreen(blockX.toFloat(), worldOffsetX)
            lightY = CoordUtil.worldToScreen(blockY.toFloat(), worldOffsetY)
          }
        }
      }
      val screenX = CoordUtil.worldToScreen(worldX, worldOffsetX)
      val screenY = CoordUtil.worldToScreen(worldY, worldOffsetY)
      batch.draw(textureRegion, screenX, screenY, box2d.width, box2d.height)
      batch.color = Color.WHITE
      if (Settings.debugEntityLight) {
        batch.draw(whiteTexture, lightX, lightY, Block.BLOCK_SIZE.toFloat(), Block.BLOCK_SIZE.toFloat())
      }
    }
    batch.color = Color.WHITE
  }
}
