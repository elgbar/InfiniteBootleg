package no.elg.infiniteBootleg.screens.stage

import ktx.actors.onClickEvent
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.visTextButton

@Scene2dDsl
fun KVisWindow.toggleableDebugButton(
  name: String,
  onToggle: () -> Unit
) = visTextButton(name, style = "debug-menu-button") {
  pad(5f)
  onClickEvent { event ->
    event.cancel()
    isDisabled = !isDisabled
    onToggle()
  }
  it.fillX()
}
