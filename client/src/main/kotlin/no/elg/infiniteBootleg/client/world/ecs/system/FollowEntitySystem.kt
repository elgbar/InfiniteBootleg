package no.elg.infiniteBootleg.client.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FollowedByCameraTag
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FollowedByCameraTag.Companion.followedByCamera
import no.elg.infiniteBootleg.core.world.ecs.followEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.system.api.FamilyEntitySystem
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

object FollowEntitySystem : FamilyEntitySystem(followEntityFamily, UPDATE_PRIORITY_DEFAULT) {

  private const val CAMERA_LERP = 10f
  private const val LERP_CUTOFF = 5f

  override fun processEntities(entities: ImmutableArray<Entity>, deltaTime: Float) {
    val entity = entities.firstOrNull() ?: return
    if (entities.size() == 1) {
      processEntity(entity, deltaTime)
    } else if (entities.size() > 1) {
      logger.warn { "There are multiple entities with ${FollowedByCameraTag::class.simpleName}. There can only be one at a time. Entities: ${entities.map { it.id }}" }
      entities.drop(1).forEach { it.followedByCamera = false }
    }
  }

  /**
   * The position of the entity was last time the camera was updated.
   *
   * Should be treated as read only outside the class
   *
   * OK that this is mutable as only one entity are allowed to be followed by the camera at a time
   */
  val processedPosition: Vector2 = Vector2()

  private fun processEntity(entity: Entity, deltaTime: Float) {
    val worldRender = entity.world.render
    if (worldRender is ClientWorldRender) {
      val camera: OrthographicCamera = worldRender.camera
      val position = entity.positionComponent
      processedPosition.set(position.x, position.y)
      val x = position.x * Block.Companion.BLOCK_TEXTURE_SIZE
      val y = position.y * Block.Companion.BLOCK_TEXTURE_SIZE
      val diffX = x - camera.position.x
      val diffY = y - camera.position.y
      val teleportCam = !Settings.enableCameraFollowLerp || abs(diffX) > Gdx.graphics.width || abs(diffY) > Gdx.graphics.height
      if (teleportCam) {
        camera.position.x = x
        camera.position.y = y
      } else {
        val dx = diffX * CAMERA_LERP
        val dy = diffY * CAMERA_LERP
        if (abs(dx) > LERP_CUTOFF || abs(dy) > LERP_CUTOFF) {
          camera.position.x += dx * deltaTime
          camera.position.y += dy * deltaTime
        }
      }
      worldRender.update()
    }
  }
}
