package no.elg.infiniteBootleg.inventory.ui

import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.main.ClientMain

/**
 * @author kheba
 */
class SlotActor internal constructor(private val skin: Skin, val slot: Slot, private val container: Container) :
  ImageButton(
    createStyle(
      skin,
      slot.stack
    )
  ) {
  private var oldStack: Item?

  init {
    oldStack = if (slot.stack == null) null else slot.stack

    // TODO tooltip
    //        final SlotTooltip tooltip = new SlotTooltip(slot, skin);
    //        tooltip.setTouchable(
    //            Touchable.disabled); // allows for mouse to hit tooltips in the top-right corner
    // of the screen without
    //        // flashing
    //        InventoryScreen.stage.addActor(tooltip);
    //        addListener(new TooltipListener(tooltip, true));
  }

  fun update() {
    slot.updateStack()

    if (slot.stack == oldStack) {
      return
    }

//          setStyle(createStyle(skin, slot.getStack()));
    oldStack = if (slot.stack == null) null else slot.stack
  }

  companion object {
    private const val SCALE = 4

    private fun createStyle(skin: Skin, item: Item?): ImageButtonStyle {
      //    if (item != null) {
      //
      //      //draw everything to this buffer to overlap the stacks texture with how many there are
      // in the stack
      //      FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA4444, (int) Block.BLOCK_SIZE
      // * SCALE,
      //        (int) Block.BLOCK_SIZE * SCALE, false);
      //
      //      //this is the texture that will store the combined texture.
      //      //it is a region so we can flip it
      //      tex = new TextureRegion(frameBuffer.getColorBufferTexture());
      //
      //      // the sprite _must_ be the size of the given graphics size
      //      //TODO find out why
      //      Sprite rawSprite = new Sprite(item.getElement().getTextureRegion());
      //
      //      rawSprite.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
      //
      //      //Start drawing everything to the framebuffer
      //      frameBuffer.begin();
      //
      //      //clear junk that might appear behind transparent textures
      //      Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
      //
      //      //start the batch (as you would normally)
      //      batch.begin();
      //
      //      //draw the tile texture
      //      rawSprite.draw(batch);
      //
      //      //scale the font so we can read it
      //      BitmapFont font = GameMain.map.font;
      //      //remember old font scale
      //      float oldScaleX = font.getData().scaleX;
      //      float oldScaleY = font.getData().scaleY;
      //      font.getData().setScale(20, 12);
      //
      //      //draw the amount in the lower left corner
      //      font.draw(batch, item.getStock() + "", 0, font.getCapHeight());
      //
      //      //reset font scale
      //      font.getData().setScale(oldScaleX, oldScaleY);
      //
      //      batch.end();
      //      frameBuffer.end();
      //
      //      //make sure the sprite will be displayed correctly
      //      tex.flip(false, true);
      //    } else {
      val tex = ClientMain.inst().assets.whiteTexture.textureRegionOrNull
      //    }
      //
      val style = ImageButtonStyle(skin.get(ButtonStyle::class.java))

      style.imageUp = TextureRegionDrawable(tex)
      style.imageDown = TextureRegionDrawable(tex)
      //
      return style
    }
  }
}
