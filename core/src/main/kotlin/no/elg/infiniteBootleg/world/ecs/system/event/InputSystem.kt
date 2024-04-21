package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import no.elg.infiniteBootleg.inventory.container.Container.Companion.open
import no.elg.infiniteBootleg.util.FLY_VEL
import no.elg.infiniteBootleg.util.JUMP_VERTICAL_VEL
import no.elg.infiniteBootleg.util.MAX_X_VEL
import no.elg.infiniteBootleg.util.WorldEntity
import no.elg.infiniteBootleg.util.WorldEntity.ClientWorldEntity
import no.elg.infiniteBootleg.util.breakBlocks
import no.elg.infiniteBootleg.util.inputMouseLocator
import no.elg.infiniteBootleg.util.interpolate
import no.elg.infiniteBootleg.util.placeBlocks
import no.elg.infiniteBootleg.util.setVel
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.api.restriction.ClientSystem
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.groundedComponent
import no.elg.infiniteBootleg.world.ecs.components.InputEventQueueComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.world.ecs.components.inventory.ContainerComponent.Companion.ownedContainerOrNull
import no.elg.infiniteBootleg.world.ecs.components.inventory.HotbarComponent.Companion.HotbarSlot
import no.elg.infiniteBootleg.world.ecs.components.inventory.HotbarComponent.Companion.hotbarComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.world.ecs.components.transients.CurrentlyBreakingComponent.Companion.currentlyBreakingComponentOrNull
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithInputEventFamily
import no.elg.infiniteBootleg.world.world.ClientWorld
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

object InputSystem :
  EventSystem<InputEvent, InputEventQueueComponent>(controlledEntityWithInputEventFamily, InputEvent::class, InputEventQueueComponent.mapper),
  ClientSystem {

  override fun handleEvent(entity: Entity, deltaTime: Float, event: InputEvent) {
    val entityWorld = entity.world as? ClientWorld ?: return
    val worldEntity = ClientWorldEntity(entityWorld, entity)
    inputMouseLocator.update(entityWorld)
    when (event) {
      is InputEvent.KeyDownEvent -> worldEntity.keyDown(event.keycode)
      is InputEvent.TouchDownEvent -> worldEntity.openChest(event.button)
      is InputEvent.KeyIsDownEvent -> worldEntity.move(event.keycode)
      is InputEvent.KeyTypedEvent -> Unit
      is InputEvent.KeyUpEvent -> Unit
      is InputEvent.MouseMovedEvent -> Unit
      is InputEvent.ScrolledEvent -> worldEntity.scrolled(event.amountY)
      is InputEvent.TouchDraggedEvent -> worldEntity.mouseDragged(event.buttons)
      is InputEvent.TouchUpEvent -> Unit
      is InputEvent.SpellCastEvent -> Unit
    }
  }

  private fun ClientWorldEntity.openChest(button: Int) {
    when (button) {
      Input.Buttons.RIGHT -> {
        val block = world.getBlock(inputMouseLocator.mouseBlockX, inputMouseLocator.mouseBlockY) ?: return
        if (block.material == Material.CONTAINER) {
          block.entity?.ownedContainerOrNull?.open()
        }
      }
    }
  }

  private fun WorldEntity.mouseDragged(set: Set<Int>) {
    val update =
      if (Input.Buttons.LEFT in set) {
        entity.currentlyBreakingComponentOrNull?.reset()
        interpolate(true, ::breakBlocks)
      } else if (Input.Buttons.RIGHT in set) {
        interpolate(true, ::placeBlocks)
      } else {
        false
      }
    if (update) {
      world.render.update()
    }
  }

  private fun WorldEntity.move(keycode: Int) {
    if (entity.flying) {
      when (keycode) {
        Input.Keys.W -> fly(dy = FLY_VEL)
        Input.Keys.S -> fly(dy = -FLY_VEL)
        Input.Keys.A -> fly(dx = -FLY_VEL)
        Input.Keys.D -> fly(dx = FLY_VEL)
      }
    } else {
      when (keycode) {
        Input.Keys.W -> if (entity.groundedComponent.onGround) setVel { oldX, _ -> oldX to JUMP_VERTICAL_VEL }
        Input.Keys.A -> moveHorz(-1f)
        Input.Keys.D -> moveHorz(1f)
      }
    }
  }

  private fun WorldEntity.scrolled(amountY: Float) {
    val hotbarComponent = entity.hotbarComponentOrNull ?: return
    val direction = sign(amountY).toInt()

    val newOrdinal = (HotbarSlot.entries.size + hotbarComponent.selected.ordinal + direction) % HotbarSlot.entries.size
    hotbarComponent.selected = HotbarSlot.fromOrdinal(newOrdinal)
  }

  private fun WorldEntity.keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.T -> entity.teleport(inputMouseLocator.mouseWorldX, inputMouseLocator.mouseWorldY, killVelocity = true)
      Input.Keys.Q -> interpolate(true, ::placeBlocks)
    }

    val hotbarComponent = entity.hotbarComponentOrNull ?: return false
    val hotbarSlot = when (keycode) {
      Input.Keys.NUM_1, Input.Keys.NUMPAD_1 -> HotbarSlot.ONE
      Input.Keys.NUM_2, Input.Keys.NUMPAD_2 -> HotbarSlot.TWO
      Input.Keys.NUM_3, Input.Keys.NUMPAD_3 -> HotbarSlot.THREE
      Input.Keys.NUM_4, Input.Keys.NUMPAD_4 -> HotbarSlot.FOUR
      Input.Keys.NUM_5, Input.Keys.NUMPAD_5 -> HotbarSlot.FIVE
      Input.Keys.NUM_6, Input.Keys.NUMPAD_6 -> HotbarSlot.SIX
      Input.Keys.NUM_7, Input.Keys.NUMPAD_7 -> HotbarSlot.SEVEN
      Input.Keys.NUM_8, Input.Keys.NUMPAD_8 -> HotbarSlot.EIGHT
      Input.Keys.NUM_9, Input.Keys.NUMPAD_9 -> HotbarSlot.NINE
      else -> return false
    }
    hotbarComponent.selected = hotbarSlot
    return true
  }

  private val tmpVec = Vector2()

  private fun WorldEntity.fly(dx: Float = 0f, dy: Float = 0f) {
    setVel { oldX, oldY -> oldX + dx to oldY + dy }
  }

  private fun WorldEntity.moveHorz(dir: Float) {
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
}
