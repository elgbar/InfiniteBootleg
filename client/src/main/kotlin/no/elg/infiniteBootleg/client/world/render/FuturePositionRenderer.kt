package no.elg.infiniteBootleg.client.world.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.core.api.Renderer
import no.elg.infiniteBootleg.core.util.isNotAir
import no.elg.infiniteBootleg.core.util.withColor
import no.elg.infiniteBootleg.core.util.worldToScreen
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.core.world.ecs.components.transients.SpellStateComponent.Companion.spellStateComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.spellEntityFamily
import no.elg.infiniteBootleg.core.world.ticker.WorldBox2DTicker

class FuturePositionRenderer(private val worldRender: ClientWorldRender) : Renderer {

  private val entities: ImmutableArray<Entity> = worldRender.world.engine.getEntitiesFor(spellEntityFamily)

  @Suppress("NOTHING_TO_INLINE")
  private inline fun futurePoint(startingPos: Double, startingVel: Double, gravity: Double, n: Double): Double {
    val stepVelocity = startingVel * WorldBox2DTicker.Companion.BOX2D_TIME_STEP
    val stepGravity = WorldBox2DTicker.Companion.BOX2D_TIME_STEP * WorldBox2DTicker.Companion.BOX2D_TIME_STEP * gravity

    return startingPos + n * stepVelocity + 0.5 * (n * n + n) * stepGravity
  }

  override fun render() {
    if (enabled) {
      worldRender.batch.withColor(a = .75f) {
        for (entity: Entity in entities) {
//          val texture = entity.textureRegionComponent.texture.textureRegion
          val box2dComp = entity.box2d
          val body = box2dComp.body
          val spawn = entity.spellStateComponentOrNull ?: continue

          val gravityX = body.world.gravity.x.toDouble() * body.gravityScale
          val gravityY = body.world.gravity.y.toDouble() * body.gravityScale

          val worldX = spawn.spawnX.toDouble()
          val worldY = spawn.spawnY.toDouble()

          for (step in 1..numberOfStepsToSee) {
            val time = step.toDouble()
            val futureWorldX = futurePoint(worldX, spawn.spawnDx.toDouble(), gravityX, time)
            val futureWorldY = futurePoint(worldY, spawn.spawnDy.toDouble(), gravityY, time)
            if (collisionCheck && worldRender.world.getRawBlock(futureWorldX.toInt(), futureWorldY.toInt(), loadChunk = false).isNotAir(markerIsAir = false)) {
              break
            }
            worldRender.batch.draw(
              ClientMain.inst().assets.whiteTexture.textureRegion,
              worldToScreen(futureWorldX),
              worldToScreen(futureWorldY),
              1f,
              1f
            )
          }
        }
      }
    }
  }

  companion object {
    var enabled = false
    var collisionCheck = true
    var numberOfStepsToSee: Int = 128
  }
}
