package no.elg.infiniteBootleg.client.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.util.inputMouseLocator
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.net.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.core.net.serverBoundSpellSpawn
import no.elg.infiniteBootleg.core.world.Staff
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityOrZero
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.transients.LastSpellCastComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.LastSpellCastComponent.Companion.lastSpellCastOrNull
import no.elg.infiniteBootleg.core.world.ecs.creation.createSpellEntity
import no.elg.infiniteBootleg.core.world.ecs.localPlayerFamily
import no.elg.infiniteBootleg.core.world.magic.SpellState.Companion.canCastAgain
import kotlin.time.TimeSource

object MagicSystem : IteratingSystem(localPlayerFamily, UPDATE_PRIORITY_DEFAULT) {

  private val posVector = Vector2()
  private val velVector = Vector2()

  override fun processEntity(entity: Entity, deltaTime: Float) {
    if (ClientMain.inst().shouldIgnoreWorldInput()) {
      return
    }
    val world = entity.world as? ClientWorld ?: return
    val selectedItem = entity.selectedItem
    val heldStaff = selectedItem?.element as? Staff ?: return
    val existingSpellState = entity.lastSpellCastOrNull
    val doCastNow = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
    if (existingSpellState.canCastAgain() && doCastNow) {
      // TODO Increase (distance/speed) by holding right click?

      val position = entity.position
      val newSpellState = heldStaff.createSpellState(entity)
      entity.add(LastSpellCastComponent(newSpellState))
      val velocityOrZero = entity.velocityOrZero(velVector)

      inputMouseLocator.update(world)
      posVector
        .set(inputMouseLocator.mouseWorldX, inputMouseLocator.mouseWorldY)
        .sub(position.x, position.y)
        .nor()

      entity.containerOrNull?.remove(selectedItem, 1u)
      world.engine.createSpellEntity(
        world,
        position.x,
        position.y,
        posVector.x * newSpellState.spellVelocity.toFloat() + velocityOrZero.x,
        posVector.y * newSpellState.spellVelocity.toFloat() + velocityOrZero.y,
        newSpellState
      ) {
        newSpellState.castMark = TimeSource.Monotonic.markNow() + newSpellState.castDelay
        newSpellState.staff.onSpellCast(newSpellState, it)

        ClientMain.inst().serverClient.sendServerBoundPacket { serverBoundSpellSpawn() }
      }
    }
  }
}
