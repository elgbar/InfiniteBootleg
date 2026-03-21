package no.elg.infiniteBootleg.client.screens.hud

import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.spinner.FloatSpinnerModel
import kotlinx.coroutines.runBlocking
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
import no.elg.infiniteBootleg.core.util.IBVisSelectBox
import no.elg.infiniteBootleg.core.util.sealedSubclassObjectInstances
import no.elg.infiniteBootleg.core.util.toAbled
import no.elg.infiniteBootleg.core.util.toTitleCase
import no.elg.infiniteBootleg.core.util.visIBSelectBox
import java.math.BigDecimal
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KMutableProperty0

fun updateAllValues(onAnyElementChanged: MutableList<suspend () -> Unit>) {
  for (onChange in onAnyElementChanged) {
    runBlocking { onChange() }
  }
}

@Scene2dDsl
fun KTable.toggleableDebugButton(
  name: String,
  description: String? = null,
  style: String = "debug-menu-button",
  onAnyElementChanged: MutableList<suspend () -> Unit>,
  property: KMutableProperty0<Boolean>,
  effectOnToggle: () -> Unit = {}
): VisTextButton =
  toggleableDebugButton(name, description, style, onAnyElementChanged, property::get) {
    property.set(!property.get())
    effectOnToggle()
  }

@Scene2dDsl
fun KTable.toggleableDebugButton(
  name: String,
  description: String? = null,
  style: String = "debug-menu-button",
  onAnyElementChanged: MutableList<suspend () -> Unit>,
  booleanGetter: () -> Boolean,
  onToggle: () -> Unit
): VisTextButton = suspendedToggleableDebugButton(name, description, style, onAnyElementChanged, booleanGetter, safeGetter = true, onToggle)

@Scene2dDsl
fun KTable.suspendedToggleableDebugButton(
  name: String,
  description: String? = null,
  style: String = "debug-menu-button",
  onAnyElementChanged: MutableList<suspend () -> Unit>,
  booleanGetter: suspend () -> Boolean,
  safeGetter: Boolean = false,
  onToggle: () -> Unit
): VisTextButton =
  visTextButton(name, style) {
    pad(5f)
    isDisabled = if (safeGetter) {
      runBlocking { booleanGetter() }
    } else {
      true
    }

    val tooltipLabel: VisLabel
    fun tooltipText() = description ?: "$name is ${isDisabled.toAbled()}"
    visTextTooltip(tooltipText()) {
      tooltipLabel = this.content as VisLabel
      tooltipLabel.setAlignment(Align.left)
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
  onAnyElementChanged: MutableList<suspend () -> Unit>,
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
  onAnyElementChanged: MutableList<suspend () -> Unit>,
  onChange: (Float) -> Unit
) = suspendedFloatSpinner(name, srcValueGetter, safeGetter = true, min, max, step, decimals, onAnyElementChanged, onChange)

@Scene2dDsl
fun KTable.suspendedFloatSpinner(
  name: String,
  srcValueGetter: suspend () -> Number,
  safeGetter: Boolean = false,
  min: Number,
  max: Number,
  step: Number,
  decimals: Int,
  onAnyElementChanged: MutableList<suspend () -> Unit>,
  onChange: (Float) -> Unit
) {
  val initialValue = if (safeGetter) runBlocking { srcValueGetter().toString() } else "0"
  val model = FloatSpinnerModel(initialValue, min.toString(), max.toString(), step.toString(), decimals)
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
  onAnyElementChanged: MutableList<suspend () -> Unit>,
  initialElement: T,
  name: String = T::class.java.simpleName.toTitleCase(),
  noinline onChange: (T) -> Unit = {}
): IBVisSelectBox<T> {
  val items = enumValues<T>().toGdxArray()
  return genericSelector(onAnyElementChanged, name, items, initialElement, onChange)
}

@Scene2dDsl
inline fun <reified T : Any> KWidget<*>.sealedSelector(
  onAnyElementChanged: MutableList<suspend () -> Unit>,
  initialElement: T,
  name: String = T::class.java.simpleName.toTitleCase(),
  noinline onChange: (T) -> Unit = {}
): IBVisSelectBox<T> {
  val items = sealedSubclassObjectInstances<T>().toGdxArray()
  return genericSelector(onAnyElementChanged, name, items, initialElement, onChange)
}

@Scene2dDsl
fun <T> KWidget<*>.genericSelector(
  onAnyElementChanged: MutableList<suspend () -> Unit>,
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
  contract { callsInPlace(theRow, InvocationKind.EXACTLY_ONCE) }
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
  onAnyElementChanged: MutableList<suspend () -> Unit>,
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
