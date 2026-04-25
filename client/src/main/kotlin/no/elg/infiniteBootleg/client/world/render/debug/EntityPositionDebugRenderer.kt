package no.elg.infiniteBootleg.client.world.render.debug

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import ktx.ashley.has
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.api.render.OverlayRenderer
import no.elg.infiniteBootleg.core.util.safeUse
import no.elg.infiniteBootleg.core.util.worldToScreen
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.DoorComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.creation.DOOR_X_OFFSET
import kotlin.math.round

class EntityPositionDebugRenderer(private val worldRender: ClientWorldRender) :
  OverlayRenderer,
  Disposable {

  private val shapeRenderer: ShapeRenderer = ShapeRenderer(1000)

  override val isActive: Boolean
    get() = Settings.debugEntityAtMouseInputEvent

  override fun render() {
    shapeRenderer.safeUse(ShapeRenderer.ShapeType.Line, worldRender.camera.combined) {
      val mouseLocator = ClientMain.inst().mouseLocator

      val mapEntitiesAt = worldRender.world.mapEntitiesAt(mouseLocator.mouseBlockX, mouseLocator.mouseBlockY)
      if (!mapEntitiesAt.hasNext()) {
        return
      }
      val hoveringEntities = mapEntitiesAt.asSequence().mapNotNull { e -> e.box2dOrNull?.let { b2d -> e to b2d } }.toList()

      shapeRenderer.color = Color.BLACK
      for ((entity, box2d) in hoveringEntities) {
        val (rawX, rawY) = entity.positionComponent
        val isDoor = entity.has(DoorComponent.mapper)
        val x = if (isDoor) {
          rawX + DOOR_X_OFFSET + box2d.halfBox2dWidth
        } else {
          rawX
        }
        val y = if (isDoor) {
          rawY + box2d.halfBox2dHeight
        } else {
          rawY
        }

        // Note sync these vars with World.mapEntitiesAt
        val lowerX = round(x - box2d.halfBox2dWidth).worldToScreen()
        val lowerY = round(y - box2d.halfBox2dHeight).worldToScreen()
        val upperY = round(y + box2d.halfBox2dHeight).worldToScreen()
        val upperX = round(x + box2d.halfBox2dWidth).worldToScreen()
        val vertices = floatArrayOf(
          // lower left
          lowerX,
          lowerY,
          // upper left
          lowerX,
          upperY,
          // upper right
          upperX,
          upperY,
          // lower right
          upperX,
          lowerY
        )
        shapeRenderer.polygon(vertices, 0, vertices.size)
      }
    }
  }

  override fun dispose() {
    shapeRenderer.dispose()
  }
}
