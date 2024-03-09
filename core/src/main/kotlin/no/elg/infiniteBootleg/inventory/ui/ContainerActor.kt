package no.elg.infiniteBootleg.inventory.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.assets.disposeSafely
import ktx.graphics.use
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.vis.visImageButton
import ktx.scene2d.vis.visTextTooltip
import ktx.scene2d.vis.visWindow
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.events.ContainerEvent
import no.elg.infiniteBootleg.events.api.EventManager.registerListener
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.safeUse
import no.elg.infiniteBootleg.util.toTitleCase
import no.elg.infiniteBootleg.util.withColor

@Scene2dDsl
fun Stage.createContainerActor(container: Container, dragAndDrop: DragAndDrop, batch: Batch): VisWindow {
  actors {
    return visWindow(container.name) {
      isVisible = false
      isMovable = true
      isResizable = false

      defaults().maxWidth(object : Value() {
        override fun get(actor: Actor?): Float {
          return Gdx.graphics.width * 0.75f
        }
      })

      val updateFunctions: MutableList<() -> Unit> = mutableListOf()

      fun updateAllSlots() {
        updateFunctions.forEach { it() }
        invalidateHierarchy()
        pack()
      }

      val filter: (ContainerEvent) -> Boolean = { it.container == container }
      val listener: ContainerEvent.() -> Unit = { Main.inst().scheduler.executeSync(::updateAllSlots) }
      registerListener<ContainerEvent.Changed>(true, filter, listener)
      registerListener<ContainerEvent.Opening>(true, filter, listener)

      for (containerSlot in container) {
        visImageButton {
          val tooltip = visTextTooltip("")

          var fbo: FrameBuffer? = null
          fun updateSlot() {
            val item = container[containerSlot.index]

            val slotDrawable = createDrawable(batch, item, containerSlot.index, fbo)
            style.imageUp = if (slotDrawable != null) {
              val (newFbo, drawable) = slotDrawable
              fbo = newFbo
              drawable
            } else {
              fbo?.disposeSafely()
              fbo = null
              defaultDrawable
            }
            tooltip.setText(item?.run { element.name.lowercase().toTitleCase() } ?: "<Empty>")
          }

          it.pad(2f).space(2f)
          updateSlot()
          updateFunctions += ::updateSlot

          val slot = InventorySlot(container, containerSlot.index)
          userObject = slot
          dragAndDrop.addSource(SlotSource(this@visImageButton, slot))
          dragAndDrop.addTarget(SlotTarget(this@visImageButton))
          pack()
        }
        if ((containerSlot.index + 1) % 10 == 0) row()
      }
      pack()
      centerWindow()
      Main.inst().scheduler.executeSync {
        updateAllSlots()
        centerWindow()
      }
    }
  }
}

const val SLOT_SIZE: Float = 64f
const val FBO_SLOT_SIZE: Float = SLOT_SIZE
val drawIndex: Boolean get() = Settings.debug

val defaultDrawable: Drawable by lazy {
  val frameBuffer = createFBO()
  frameBuffer.use {
    Gdx.gl.glClearColor(1f, 0f, 1f, 0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
  }
  TextureRegionDrawable(TextureRegion(frameBuffer.colorBufferTexture).also { it.flip(false, true) })
}

private fun createFBO(): FrameBuffer =
  FrameBuffer(Pixmap.Format.RGBA8888, FBO_SLOT_SIZE.toInt(), FBO_SLOT_SIZE.toInt(), false).also {
    it.colorBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
  }

fun createDrawable(batch: Batch, maybeItem: Item?, index: Int, fbo: FrameBuffer?): Pair<FrameBuffer, Drawable>? {
  val item = maybeItem ?: return null
  val texture = item.element.textureRegion?.textureRegionOrNull ?: return null
  val frameBuffer = fbo ?: createFBO()
  frameBuffer.use {
    // clear junk that might appear behind transparent textures
    Gdx.gl.glClearColor(1f, 0f, 1f, 0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    batch.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, FBO_SLOT_SIZE, FBO_SLOT_SIZE)
    batch.safeUse {
      batch.draw(texture, 0f, 0f, FBO_SLOT_SIZE, FBO_SLOT_SIZE)
      if (drawIndex) {
        Main.inst().assets.font16pt.withColor(r = 0.75f, g = 0.75f, b = 0.75f, a = 1f) { font ->
          font.draw(batch, "i:$index", 0f, FBO_SLOT_SIZE - font.capHeight / 3f)
        }
      }
      Main.inst().assets.font20pt.also { font ->
        font.draw(batch, "${item.stock}", 0f, font.capHeight)
      }
    }
  }
  return frameBuffer to TextureRegionDrawable(TextureRegion(frameBuffer.colorBufferTexture).also { it.flip(false, true) })
}
