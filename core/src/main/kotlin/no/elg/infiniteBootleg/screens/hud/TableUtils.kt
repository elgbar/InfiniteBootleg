package no.elg.infiniteBootleg.screens.hud

import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.spinner.FloatSpinnerModel
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.collections.GdxArray
import ktx.collections.isNotEmpty
import ktx.collections.toGdxArray
import ktx.scene2d.KTable
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.KVisSelectBox
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.separator
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visSelectBox
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
import no.elg.infiniteBootleg.util.sealedSubclassObjectInstances
import no.elg.infiniteBootleg.util.toAbled
import no.elg.infiniteBootleg.util.toTitleCase
import java.math.BigDecimal

fun updateAllValues(onAnyElementChanged: MutableList<() -> Unit>) {
  for (onChange in onAnyElementChanged) {
    onChange()
  }
}

@Scene2dDsl
fun KTable.toggleableDebugButton(
  name: String,
  description: String? = null,
  style: String = "debug-menu-button",
  onAnyElementChanged: MutableList<() -> Unit>,
  booleanGetter: () -> Boolean,
  onToggle: () -> Unit
): VisTextButton =
  visTextButton(name, style) {
    pad(5f)
    isDisabled = booleanGetter()

    val tooltipLabel: VisLabel
    fun tooltipText() = description ?: "$name is ${isDisabled.toAbled()}"
    visTextTooltip(tooltipText()) {
      tooltipLabel = this.content as VisLabel
    }

    onClick {
      onToggle()
      updateAllValues(onAnyElementChanged)
    }
    it.fillX()
    onAnyElementChanged += {
      isDisabled = booleanGetter()
      tooltipLabel.setText(tooltipText())
    }
  }

@Scene2dDsl
fun KTable.floatSpinner(
  name: String,
  srcValueGetter: () -> Number,
  min: Number,
  max: Number,
  step: Number,
  decimals: Int,
  onAnyElementChanged: MutableList<() -> Unit>,
  onChange: (Float) -> Unit
) {
  val model = FloatSpinnerModel(srcValueGetter().toString(), min.toString(), max.toString(), step.toString(), decimals)
  spinner(name, model) {
    it.fillX()

    onChange {
      onChange(model.value.toFloat())
      updateAllValues(onAnyElementChanged)
    }
    onAnyElementChanged += {
      model.setValue(BigDecimal.valueOf(srcValueGetter().toDouble()), false)
    }
  }
}

@Scene2dDsl
inline fun <reified T : Enum<T>> KTable.enumSelector(
  onAnyElementChanged: MutableList<() -> Unit>,
  initialElement: T,
  name: String = T::class.java.simpleName.toTitleCase(),
  noinline onChange: (T) -> Unit = {}
): KVisSelectBox<T> {
  val model = enumValues<T>().toGdxArray()
  return genericSelector(onAnyElementChanged, name, model, initialElement, onChange)
}

@Scene2dDsl
inline fun <reified T : Any> KTable.sealedSelector(
  onAnyElementChanged: MutableList<() -> Unit>,
  initialElement: T,
  name: String = T::class.java.simpleName.toTitleCase(),
  noinline onChange: (T) -> Unit = {}
): KVisSelectBox<T> {
  val model = sealedSubclassObjectInstances<T>().toGdxArray()
  return genericSelector(onAnyElementChanged, name, model, initialElement, onChange)
}

@Scene2dDsl
fun <T> KTable.genericSelector(
  onAnyElementChanged: MutableList<() -> Unit>,
  name: String,
  model: GdxArray<T>,
  initialElement: T,
  onChange: (T) -> Unit = {}
): KVisSelectBox<T> {
  require(model.isNotEmpty()) { "Model must have at least one element" }
  horizontalGroup {
    visLabel(name)
    return visSelectBox {
      items = model
      selected = initialElement
      onChange {
        onChange(this.selected)
        updateAllValues(onAnyElementChanged)
      }
    }
  }
}

@Scene2dDsl
fun KVisWindow.section(theRow: KTable.() -> Unit) {
  theRow()
  row()
}

@Scene2dDsl
fun KVisWindow.aSeparator(cols: Int = 1) {
  section {
    separator {
      it.fillX()
      it.colspan(cols)
    }
  }
}
