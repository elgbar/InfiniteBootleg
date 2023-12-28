package no.elg.infiniteBootleg.util

import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisSelectBox
import ktx.collections.GdxArray
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.defaultStyle
import no.elg.infiniteBootleg.world.magic.Named
import java.util.Locale
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun <T> defaultStringify(item: T): String =
  when (item) {
    is Named -> item.displayName
    is Enum<*> -> item.name.replace('_', ' ').lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    else -> item.toString()
  }

@Scene2dDsl
inline fun <reified T> KWidget<*>.visIBSelectBoxForSealedClass(
  selectedItem: T? = null,
  style: String = defaultStyle,
  noinline stringify: (T) -> String = ::defaultStringify,
  init: IBVisSelectBox<T>.() -> Unit = {}
): IBVisSelectBox<T> where T : Enum<T> = visIBSelectBox(GdxArray(sealedSubclassObjectInstances<T>().toTypedArray()), selectedItem, style, stringify, init)

@Scene2dDsl
inline fun <reified T> KWidget<*>.visIBSelectBoxForEnum(
  selectedItem: T? = null,
  style: String = defaultStyle,
  noinline stringify: (T) -> String = ::defaultStringify,
  init: IBVisSelectBox<T>.() -> Unit = {}
): IBVisSelectBox<T> where T : Enum<T> = visIBSelectBox(GdxArray(enumValues<T>()), selectedItem, style, stringify, init)

@Scene2dDsl
inline fun <T> KWidget<*>.visIBSelectBox(
  items: GdxArray<T>,
  selectedItem: T? = null,
  style: String = defaultStyle,
  noinline stringify: (T) -> String = ::defaultStringify,
  init: IBVisSelectBox<T>.() -> Unit = {}
): IBVisSelectBox<T> {
  contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
  val selectBox = IBVisSelectBox(items, selectedItem, style, stringify)
  storeActor(selectBox)
  selectBox.init()
  return selectBox
}

class IBVisSelectBox<T>(
  items: GdxArray<T>,
  selectedItem: T? = null,
  styleName: String = defaultStyle,
  private val stringify: (T) -> String = ::defaultStringify
) : VisSelectBox<T>(styleName) {
  public override fun toString(item: T): String = stringify(item)

  init {
    // Internal items array has to be copied, as it is cleared by the setter method.
    this.items = Array(items)
    this.selected = selectedItem
  }
}
