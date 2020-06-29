package no.elg.infiniteBootleg.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.render.WorldRender
import no.elg.infiniteBootleg.world.subgrid.Entity
import no.elg.infiniteBootleg.world.subgrid.LivingEntity
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

/**
 * Control scheme where the user moves the player around with a keyboard
 *
 * @author Elg
 */
class KeyboardControls(worldRender: WorldRender, entity: LivingEntity) : AbstractEntityControls(worldRender, entity) {
  private var selected: Material?

  //if objects can be placed on non-air blocks
  var replacePlacement = false
  private var breakBrushSize = 2f
  private var placeBrushSize = 1f
  private var lastEditTick: Long = 0

  override fun update() {
    if (Main.inst().console.isVisible) {
      return
    }
    var update = false
    val blockX = Main.inst().mouseBlockX
    val blockY = Main.inst().mouseBlockY
    val rawX = Main.inst().mouseX
    val rawY = Main.inst().mouseY
    val world = worldRender.getWorld()
    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
      if (breakBrushSize <= 1) {
        world.remove(blockX, blockY, true)
      } else {
        for (block in world.getBlocksWithin(rawX, rawY, breakBrushSize, true)) {
          world.remove(block.worldX, block.worldY, true)
        }
      }
      lastEditTick = world.tick
      update = true
    } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
      if (placeBrushSize <= 1) {
        update = selected!!.create(world, blockX, blockY)
      } else {
        for (block in world.getBlocksWithin(rawX, rawY, placeBrushSize, false)) {
          update = update or selected!!.create(world, block.worldX, block.worldY)
        }
      }
      if (update) {
        lastEditTick = world.tick
      }
    }
    val entity: Entity = controlled
    if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
      //teleport the player to the (last) location of the mouse
      entity.teleport(Main.inst().mouseX, Main.inst().mouseY, true)
      val input = world.input
      if (input != null) {
        input.following = entity
        input.isLockedOn = true
      }
    } else {

      fun setVel(modify: (oldX: Float, oldY: Float) -> (Pair<Float, Float>)) {
        synchronized(WorldRender.BOX2D_LOCK) {
          controlled.updatePos()
          val body = controlled.body
          val vel = body.linearVelocity
          val (nx, ny) = modify(vel.x, vel.y)
          val cap = { z: Float, max: Float -> sign(z) * min(max, abs(z)) }
          body.setLinearVelocity(cap(nx, MAX_X_VEL), cap(ny, MAX_Y_VEL))
          body.isAwake = true
        }
      }

      if (entity.isFlying) {

        fun fly(dx: Float = 0f, dy: Float = 0f) {
          setVel { oldX, oldY -> oldX + dx to oldY + dy }
        }

        when {
          Gdx.input.isKeyPressed(Input.Keys.W) -> fly(dy = FLY_VEL)
          Gdx.input.isKeyPressed(Input.Keys.S) -> fly(dy = -FLY_VEL)
          Gdx.input.isKeyPressed(Input.Keys.A) -> fly(dx = -FLY_VEL)
          Gdx.input.isKeyPressed(Input.Keys.D) -> fly(dx = FLY_VEL)
        }
      } else {
        if (entity.isOnGround && Gdx.input.isKeyPressed(Input.Keys.W)) {
          setVel { oldX, _ -> oldX to JUMP_VERTICAL_VEL }
        }

        fun moveHorz(velX: Float) {
          setVel { oldX, oldY ->
            val multiplier = {
              //Stop faster when switching direction
              val currSig = sign(oldX)
              val reverseDir = if (currSig != 0f && sign(velX) != currSig) 1f else 0f

              //hold shift to run
              val shift = if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) 1f else 0f

              1f + reverseDir + shift
            }

            val newVel = (oldX + velX * multiplier()).let {
              if (it > 20f) 20f else it
            }

            return@setVel oldX + velX * multiplier() to oldY
          }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
          moveHorz(-HORIZONTAL_IMPULSE)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
          moveHorz(HORIZONTAL_IMPULSE)
        }
      }
    }
    if (update) {
      worldRender.update()
    }
  }

  override fun keyDown(keycode: Int): Boolean {
    if (Main.inst().console.isVisible) {
      return false
    }
    selected = when (keycode) {
      Input.Keys.NUM_0, Input.Keys.NUMPAD_0 -> Material.values()[0]
      Input.Keys.NUM_1, Input.Keys.NUMPAD_1 -> Material.values()[1]
      Input.Keys.NUM_2, Input.Keys.NUMPAD_2 -> Material.values()[2]
      Input.Keys.NUM_3, Input.Keys.NUMPAD_3 -> Material.values()[3]
      Input.Keys.NUM_4, Input.Keys.NUMPAD_4 -> Material.values()[4]
      Input.Keys.NUM_5, Input.Keys.NUMPAD_5 -> Material.values()[5]
      Input.Keys.NUM_6, Input.Keys.NUMPAD_6 -> Material.values()[6]
      Input.Keys.NUM_7, Input.Keys.NUMPAD_7 -> Material.values()[7]
      Input.Keys.NUM_8, Input.Keys.NUMPAD_8 -> Material.values()[8]
      Input.Keys.NUM_9, Input.Keys.NUMPAD_9 -> Material.values()[9]
      else -> return false
    }
    return true
  }

  override fun getSelected(): Material? {
    return selected
  }

  override fun setSelected(selected: Material?) {
    this.selected = selected
  }

  override fun getBreakBrushSize(): Float {
    return breakBrushSize
  }

  override fun setBreakBrushSize(breakBrushSize: Float) {
    this.breakBrushSize = breakBrushSize
  }

  override fun getPlaceBrushSize(): Float {
    return placeBrushSize
  }

  override fun setPlaceBrushSize(placeBrushSize: Float) {
    this.placeBrushSize = placeBrushSize
  }

  companion object {
    const val JUMP_VERTICAL_VEL = 7.5f
    const val HORIZONTAL_IMPULSE = 0.15f
    const val FLY_VEL = .075f
    const val EDIT_TICK_DELAY = 1 //delay in ticks between allowing to place/break blocks


    const val MAX_X_VEL = 30f
    const val MAX_Y_VEL = JUMP_VERTICAL_VEL
  }

  init {
    selected = Material.STONE
  }
}
