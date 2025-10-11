package no.elg.infiniteBootleg.client.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2Vec2
import no.elg.infiniteBootleg.client.inventory.container.open
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.util.WorldEntity
import no.elg.infiniteBootleg.client.util.breakBlocks
import no.elg.infiniteBootleg.client.util.inputMouseLocator
import no.elg.infiniteBootleg.client.util.interpolate
import no.elg.infiniteBootleg.client.util.isAltPressed
import no.elg.infiniteBootleg.client.util.placeBlocks
import no.elg.infiniteBootleg.client.util.setVel
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.core.net.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.core.net.serverBoundUpdateSelectedSlot
import no.elg.infiniteBootleg.core.util.FLY_VEL
import no.elg.infiniteBootleg.core.util.JUMP_VERTICAL_VEL
import no.elg.infiniteBootleg.core.util.MAX_X_VEL
import no.elg.infiniteBootleg.core.world.HorizontalDirection
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.Tool
import no.elg.infiniteBootleg.core.world.box2d.extensions.mass
import no.elg.infiniteBootleg.core.world.box2d.extensions.set
import no.elg.infiniteBootleg.core.world.box2d.extensions.velocity
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent.Companion.groundedComponent
import no.elg.infiniteBootleg.core.world.ecs.components.InputEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.HotbarSlot
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.hotbarComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.core.world.ecs.controlledEntityWithInputEventFamily
import no.elg.infiniteBootleg.core.world.ecs.system.api.EventSystem
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.sign

object InputSystem : EventSystem<InputEvent, InputEventQueueComponent>(
  controlledEntityWithInputEventFamily,
  InputEvent::class,
  InputEventQueueComponent.mapper
) {

  override fun beforeAllHandle() {
    ClientMain.inst().world?.let(inputMouseLocator::update)
  }

  override fun handleEvent(entity: Entity, event: InputEvent) {
    val entityWorld = entity.world as? ClientWorld ?: return
    val worldEntity = WorldEntity.ClientWorldEntity(entityWorld, entity)
    when (event) {
      is InputEvent.KeyDownEvent -> worldEntity.keyDown(event.keycode)
      is InputEvent.TouchDownEvent -> worldEntity.openChest(event.button)
      is InputEvent.KeyIsDownEvent -> worldEntity.move(event.keycode)
      is InputEvent.ScrolledEvent -> worldEntity.scrolled(event.amountY)
      is InputEvent.TouchDraggedEvent -> worldEntity.mouseDragged(event.buttons, event.justPressed)
      is InputEvent.KeyTypedEvent -> Unit
      is InputEvent.KeyUpEvent -> Unit
      is InputEvent.MouseMovedEvent -> Unit
      is InputEvent.TouchUpEvent -> Unit
      is InputEvent.SpellCastEvent -> Unit
    }
  }

  private fun WorldEntity.ClientWorldEntity.openChest(button: Int) {
    when (button) {
      Input.Buttons.RIGHT -> {
        val block = world.getBlock(inputMouseLocator.mouseBlockX, inputMouseLocator.mouseBlockY) ?: return
        if (block.material == Material.Container) {
          val owner = ContainerOwner.from(inputMouseLocator.mouseBlockX, inputMouseLocator.mouseBlockY)
          world.worldContainerManager.find(owner).thenApply { container ->
            container?.open()
          }
        }
      }
    }
  }

  private fun WorldEntity.mouseDragged(set: Set<Int>, justPressed: Boolean) {
    val update = if (Input.Buttons.LEFT in set) {
      interpolate(justPressed, ::breakBlocks)
    } else if (Input.Buttons.RIGHT in set) {
      interpolate(justPressed, ::placeBlocks)
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
        Input.Keys.W -> jump()
        Input.Keys.A -> moveHorz(HorizontalDirection.WESTWARD)
        Input.Keys.D -> moveHorz(HorizontalDirection.EASTWARD)
      }
    }
  }

  private fun WorldEntity.scrolled(amountY: Float) {
    val hotbarComponent = entity.hotbarComponentOrNull ?: return
    val direction = sign(amountY).toInt()
    if (isAltPressed()) {
      val locallyControlledComponent = entity.locallyControlledComponentOrNull ?: return
      val selectedItem = hotbarComponent.selectedItem(entity) ?: return
      if (selectedItem.element is Tool) {
        val newBrushSize = locallyControlledComponent.brushSize + direction * 0.25f
        locallyControlledComponent.brushSize = newBrushSize.coerceAtLeast(1f)
      }
    } else {
      val newOrdinal = (HotbarSlot.entries.size + hotbarComponent.selected.ordinal + direction) % HotbarSlot.entries.size
      updateSelectedItem(hotbarComponent, HotbarSlot.fromOrdinal(newOrdinal))
    }
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
    updateSelectedItem(hotbarComponent, hotbarSlot)
    return true
  }

  private fun WorldEntity.updateSelectedItem(hotbarComponent: HotbarComponent, slot: HotbarSlot) {
    hotbarComponent.selected = slot
    ClientMain.inst().serverClient.sendServerBoundPacket { serverBoundUpdateSelectedSlot(slot) }
  }

  private val tmpVec = b2Vec2()

  private fun WorldEntity.fly(dx: Float = 0f, dy: Float = 0f) {
    setVel { oldX, oldY -> oldX + dx to oldY + dy }
  }

  fun WorldEntity.jump() {
    if (entity.groundedComponent.canJump && entity.velocityComponentOrNull?.isStill() ?: true) {
      setVel { oldX, _ -> oldX to JUMP_VERTICAL_VEL }
    }
  }

  private fun WorldEntity.moveHorz(dir: HorizontalDirection) {
    world.postBox2dRunnable {
      val groundedComponent = entity.groundedComponent
      if (groundedComponent.canMove(dir)) {
        val bodyId = entity.box2dBody
        val currSpeed = bodyId.velocity.x().absoluteValue
        val wantedSpeed = if (groundedComponent.onGround) {
          MAX_X_VEL
        } else {
          MAX_X_VEL * (2f / 3f)
        }
        val impulse = bodyId.mass * dir.value * (wantedSpeed - min(currSpeed, wantedSpeed))

        tmpVec.set(impulse, 0)

        Box2d.b2Body_ApplyLinearImpulseToCenter(bodyId, tmpVec, true)
      }
    }
  }
}
