package no.elg.infiniteBootleg.screens.stage

import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.spinner.FloatSpinnerModel
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
import no.elg.infiniteBootleg.util.toAbled

@Scene2dDsl
fun KVisWindow.toggleableDebugButton(
  name: String,
  initiallyDisabled: Boolean,
  onToggle: () -> Unit
) = visTextButton(name, style = "debug-menu-button") {
  pad(5f)
  isDisabled = initiallyDisabled

  val tooltipLabel: VisLabel
  fun tooltipText() = "$name is ${isDisabled.toAbled()}"
  visTextTooltip(tooltipText()) {
    tooltipLabel = this.content as VisLabel
  }

  onClick {
    isDisabled = !isDisabled
    tooltipLabel.setText(tooltipText())
    onToggle()
  }
  it.fillX()
}

@Scene2dDsl
fun KVisWindow.floatSpinner(name: String, initialValue: Float, min: Float, max: Float, step: Float, onChange: (Float) -> Unit) {
  val model = FloatSpinnerModel(initialValue.toString(), min.toString(), max.toString(), step.toString())
  spinner(name, model) {
    it.fillX()
    onChange {
      onChange(model.value.toFloat())
    }
  }
}
