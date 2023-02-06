package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.input.WorldInputHandler
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.tags.FollowedByCameraTag
import no.elg.infiniteBootleg.world.ecs.followEntityFamily
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import kotlin.math.abs

object FollowEntitySystem : FamilyEntitySystem(followEntityFamily, UPDATE_PRIORITY_DEFAULT) {
  override fun update(deltaTime: Float) {
    val entity = entities.firstOrNull() ?: return
    processEntity(entity)
    if (entities.size() > 1) {
      Main.logger().warn("There are multiple entities with ${FollowedByCameraTag::class.simpleName}. There can only be one at a time. Entities: $entities")
    }
  }

  private fun processEntity(entity: Entity) {
    val worldRender = entity.world.world.render
    if (worldRender is ClientWorldRender) {
      val camera: OrthographicCamera = worldRender.camera
      val position = entity.position
      val x = position.x * Block.BLOCK_SIZE
      val y = position.y * Block.BLOCK_SIZE
      val diffX = x - camera.position.x
      val diffY = y - camera.position.y
      val teleportCam = !Settings.enableCameraFollowLerp || abs(diffX) > Gdx.graphics.width || abs(diffY) > Gdx.graphics.height
      if (teleportCam) {
        camera.position.x = x * Block.BLOCK_SIZE
        camera.position.y = y * Block.BLOCK_SIZE
      } else {
        val dx = diffX * WorldInputHandler.CAMERA_LERP * Block.BLOCK_SIZE
        val dy = diffY * WorldInputHandler.CAMERA_LERP * Block.BLOCK_SIZE
        if (abs(dx) > WorldInputHandler.LERP_CUTOFF || abs(dy) > WorldInputHandler.LERP_CUTOFF) {
          camera.position.x += dx * Gdx.graphics.deltaTime
          camera.position.y += dy * Gdx.graphics.deltaTime
        }
      }
      worldRender.update()
    }
  }
}
