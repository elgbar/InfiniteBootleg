package no.elg.infiniteBootleg.client.assets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.kotcrab.vis.ui.VisUI
import ktx.style.imageTextButton
import ktx.style.label
import ktx.style.menu
import ktx.style.menuItem
import ktx.style.selectBox
import ktx.style.set
import ktx.style.textButton
import ktx.style.textField
import ktx.style.visCheckBox
import ktx.style.visImageTextButton
import ktx.style.visTextButton
import ktx.style.visTextField
import ktx.style.window
import no.elg.infiniteBootleg.core.assets.InfAssets
import no.elg.infiniteBootleg.core.main.Main

fun InfAssets.loadInfBootSkin() {
  if (Main.Companion.isClient) {
    // Only load vis ui on clients
    with(VisUI.getSkin() as Skin) {
      val notFlippedFont = font20pt
      val boldNotFlippedFont = font20pt

      set("default-font", notFlippedFont)

      label(extend = "default") { font = notFlippedFont }
      label(extend = "link-label") { font = notFlippedFont }
      label(extend = "small") { font = notFlippedFont }
      label(extend = "menuitem-shortcut") { font = notFlippedFont }

      visTextField(extend = "default") { font = notFlippedFont }
      textField(extend = "default") { font = notFlippedFont }

      visTextButton(extend = "default") { font = notFlippedFont }
      visTextButton(extend = "menu-bar") { font = notFlippedFont }
      visTextButton(extend = "toggle") { font = notFlippedFont }
      visTextButton(extend = "blue") { font = notFlippedFont }

      visCheckBox(extend = "default") { font = notFlippedFont }

      textButton(extend = "default") { font = notFlippedFont }

      val newOpenButtonStyle = visImageTextButton(extend = "default") { font = notFlippedFont }
      visImageTextButton(extend = "menu-bar") { font = notFlippedFont }
      imageTextButton(extend = "default") { font = notFlippedFont }

      window(extend = "default") { titleFont = boldNotFlippedFont }
      window(extend = "resizable") { titleFont = boldNotFlippedFont }
      window(extend = "noborder") { titleFont = boldNotFlippedFont }
      window(extend = "dialog") { titleFont = boldNotFlippedFont }

      selectBox(extend = "default") { font = notFlippedFont }

      menuItem(extend = "default") { font = notFlippedFont }
      menu { openButtonStyle = newOpenButtonStyle }

      visTextButton(name = "debug-menu-button", extend = "default") {
        font = notFlippedFont
        fontColor = Color.WHITE
        disabledFontColor = Color.WHITE
        down = newDrawable("white", Color.valueOf("#FF4136"))
        up = down
        over = newDrawable("white", Color.FIREBRICK)
        checkedOver = over
        disabled = newDrawable("white", Color.FOREST)
      }

      visTextButton(name = "toggle-menu-button", extend = "debug-menu-button") {
        down = newDrawable("white", Color.PURPLE)
        up = down
        over = newDrawable("white", Color.PURPLE.cpy().mul(0.9f))
        checkedOver = over
      }
    }
  }
}
