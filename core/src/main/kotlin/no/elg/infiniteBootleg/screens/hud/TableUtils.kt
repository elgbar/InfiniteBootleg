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
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.KVisTable
import ktx.scene2d.vis.separator
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visSlider
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
import no.elg.infiniteBootleg.util.IBVisSelectBox
import no.elg.infiniteBootleg.util.sealedSubclassObjectInstances
import no.elg.infiniteBootleg.util.toAbled
import no.elg.infiniteBootleg.util.toTitleCase
import no.elg.infiniteBootleg.util.visIBSelectBox
import java.math.BigDecimal
import kotlin.reflect.KMutableProperty0

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
  property: KMutableProperty0<Boolean>
): VisTextButton = toggleableDebugButton(name, description, style, onAnyElementChanged, property::get) { property.set(!property.get()) }

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
fun KTable.intSpinner(
  name: String,
  srcValueGetter: () -> Number,
  min: Number,
  max: Number,
  step: Number,
  decimals: Int,
  onAnyElementChanged: MutableList<() -> Unit>,
  onChange: (Int) -> Unit
) = floatSpinner(name, srcValueGetter, min, max, step, decimals, onAnyElementChanged) { onChange(it.toInt()) }

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
inline fun <reified T : Enum<T>> KWidget<*>.enumSelector(
  onAnyElementChanged: MutableList<() -> Unit>,
  initialElement: T,
  name: String = T::class.java.simpleName.toTitleCase(),
  noinline onChange: (T) -> Unit = {}
): IBVisSelectBox<T> {
  val items = enumValues<T>().toGdxArray()
  return genericSelector(onAnyElementChanged, name, items, initialElement, onChange)
}

@Scene2dDsl
inline fun <reified T : Any> KWidget<*>.sealedSelector(
  onAnyElementChanged: MutableList<() -> Unit>,
  initialElement: T,
  name: String = T::class.java.simpleName.toTitleCase(),
  noinline onChange: (T) -> Unit = {}
): IBVisSelectBox<T> {
  val items = sealedSubclassObjectInstances<T>().toGdxArray()
  return genericSelector(onAnyElementChanged, name, items, initialElement, onChange)
}

@Scene2dDsl
fun <T> KWidget<*>.genericSelector(
  onAnyElementChanged: MutableList<() -> Unit>,
  name: String,
  items: GdxArray<T>,
  initialElement: T,
  onChange: (T) -> Unit = {}
): IBVisSelectBox<T> {
  require(items.isNotEmpty()) { "Model must have at least one element" }
  visTable(true) {
    visLabel(name)
    return visIBSelectBox(items, initialElement) {
      onChange {
        onChange(this.selected)
        updateAllValues(onAnyElementChanged)
      }
    }
  }
}

@Scene2dDsl
fun KVisTable.section(theRow: KTable.() -> Unit) {
  theRow()
  row()
}

@Scene2dDsl
fun KVisTable.aSeparator(cols: Int = this.columns) {
  section {
    separator {
      it.fillX()
      it.colspan(cols)
    }
  }
}

@Scene2dDsl
fun KTable.floatSlider(
  name: String,
  srcValueGetter: () -> Number,
  min: Number,
  max: Number,
  step: Number,
  onAnyElementChanged: MutableList<() -> Unit>,
  onChange: (Float) -> Unit
) {
  horizontalGroup {
    it.fillX()
    space(5f)
    visLabel(name)
    visSlider(min.toFloat(), max.toFloat(), step.toFloat()) {
      this.name = name
      setValue(srcValueGetter().toFloat())

      onAnyElementChanged += {
        setValue(srcValueGetter().toFloat())
      }
      onChange {
        onChange(this.value)
        updateAllValues(onAnyElementChanged)
      }
    }
  }
}
