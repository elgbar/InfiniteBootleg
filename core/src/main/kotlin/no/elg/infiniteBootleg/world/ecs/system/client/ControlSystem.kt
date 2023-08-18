package no.elg.infiniteBootleg.world.ecs.system.client

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.util.FLY_VEL
import no.elg.infiniteBootleg.util.MAX_X_VEL
import no.elg.infiniteBootleg.util.WorldEntity
import no.elg.infiniteBootleg.util.breakBlocks
import no.elg.infiniteBootleg.util.inputMouseLocator
import no.elg.infiniteBootleg.util.interpolate
import no.elg.infiniteBootleg.util.jump
import no.elg.infiniteBootleg.util.placeBlocks
import no.elg.infiniteBootleg.util.setVel
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_EARLY
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.groundedComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.world.ecs.controlledEntityFamily
import no.elg.infiniteBootleg.world.world.ClientWorld
import kotlin.math.abs
import kotlin.math.min

object ControlSystem : IteratingSystem(controlledEntityFamily, UPDATE_PRIORITY_EARLY) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    if (ClientMain.inst().shouldIgnoreWorldInput()) {
      return
    }
    val entityWorld = entity.world as? ClientWorld ?: return
    inputMouseLocator.update(entityWorld)

    with(WorldEntity(entityWorld, entity)) {
      when {
        Gdx.input.isButtonPressed(Input.Buttons.LEFT) -> interpolate(false, ::breakBlocks)
        Gdx.input.isButtonPressed(Input.Buttons.RIGHT) -> interpolate(false, ::placeBlocks)
        Gdx.input.isKeyJustPressed(Input.Keys.Q) -> interpolate(true, ::placeBlocks)
      }

      if (entity.flying) {
        fly()
      } else {
        jump()
        walk()
      }
    }
  }

  private val tmpVec = Vector2()

  private fun WorldEntity.fly() {
    fun fly(dx: Float = 0f, dy: Float = 0f) {
      setVel { oldX, oldY -> oldX + dx to oldY + dy }
    }

    when {
      Gdx.input.isKeyPressed(Input.Keys.W) -> fly(dy = FLY_VEL)
      Gdx.input.isKeyPressed(Input.Keys.S) -> fly(dy = -FLY_VEL)
      Gdx.input.isKeyPressed(Input.Keys.A) -> fly(dx = -FLY_VEL)
      Gdx.input.isKeyPressed(Input.Keys.D) -> fly(dx = FLY_VEL)
    }
  }

  private fun WorldEntity.walk() {
    fun moveHorz(dir: Float) {
      world.postBox2dRunnable {
        val groundedComponent = entity.groundedComponent
        if (groundedComponent.canMove(dir)) {
          val body = entity.box2dBody
          val currSpeed = body.linearVelocity.x
          val wantedSpeed = dir * if (groundedComponent.onGround) {
            MAX_X_VEL
          } else {
            MAX_X_VEL * (2f / 3f)
          }
          val impulse = body.mass * (wantedSpeed - (dir * min(abs(currSpeed), abs(wantedSpeed))))

          tmpVec.set(impulse, entity.velocityComponent.dy)

          body.applyLinearImpulse(tmpVec, body.worldCenter, true)
        }
      }
    }

    if (Gdx.input.isKeyPressed(Input.Keys.A)) {
      moveHorz(-1f)
    }
    if (Gdx.input.isKeyPressed(Input.Keys.D)) {
      moveHorz(1f)
    }
  }
}
