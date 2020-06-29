package no.elg.infiniteBootleg.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.render.WorldRender
import no.elg.infiniteBootleg.world.subgrid.Entity
import no.elg.infiniteBootleg.world.subgrid.LivingEntity

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
      if (entity.isFlying) {
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
          moveHorz(0f)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
          moveHorz(0f)
        }
      } else {
        if (entity.isOnGround && Gdx.input.isKeyPressed(Input.Keys.W)) {
          jump()
        }
      }
      if (Gdx.input.isKeyPressed(Input.Keys.A)) {
        moveHorz(-HORIZONTAL_IMPULSE)
      }
      if (Gdx.input.isKeyPressed(Input.Keys.D)) {
        moveHorz(HORIZONTAL_IMPULSE)
      }
    }
    if (update) {
      worldRender.update()
    }
  }

  private fun setVel(x: Float, y: Float) {
    synchronized(WorldRender.BOX2D_LOCK) {
      controlled.updatePos()
      val body = controlled.body
      val vel = body.linearVelocity
      body.setLinearVelocity(vel.x, vel.y + JUMP_VERTICAL_VEL)
      body.isAwake = true
    }
  }

  private fun jump() {}
  private fun moveHorz(velX: Float) {
    synchronized(WorldRender.BOX2D_LOCK) {
      controlled.updatePos()
      val body = controlled.body
      val vel = body.linearVelocity
      var multiplier = 1f

      //Stop faster when switching direction
      val currSig = Math.signum(vel.x)
      if (currSig != 0f && Math.signum(velX) != currSig) {
        multiplier++
      }
      //Shift to run!
      if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
        multiplier++
      }
      body.setLinearVelocity(velX * multiplier, vel.y)
      body.isAwake = true
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
    const val HORIZONTAL_IMPULSE = .15f
    const val EDIT_TICK_DELAY = 1 //delay in ticks between allowing to place/break blocks
  }

  init {
    selected = Material.STONE
  }
}
