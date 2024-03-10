package no.elg.infiniteBootleg.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.PopupMenu
import com.kotcrab.vis.ui.widget.Separator
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.isShown
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.scene2d.KTable
import ktx.scene2d.RootWidget
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.StageWidget
import ktx.scene2d.defaultStyle
import ktx.scene2d.table
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTextButton
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

val platformButtonPadding: Float
  get() = 10f
val platformSpacing: Float
  get() = 5f
val platformCheckBoxSize: Float
  get() = 20f

@Scene2dDsl
fun padAndSpace(cell: Cell<*>) {
  cell.pad(platformButtonPadding)
  cell.space(platformSpacing)
}

/**
 * Alias for [PopupMenu.addSeparator] to make it blend better in with the scene 2d DSL, but without
 * the padding
 */
@Scene2dDsl
fun PopupMenu.separator() {
  add(Separator("menu")).fill().expand().row()
}

fun Button.onInteract(
  stage: Stage,
  vararg keyShortcut: Int,
  playClick: Boolean = true,
  catchEvent: Boolean = false,
  interaction: Button.() -> Unit
) {
  this.onInteract(
    stage = stage,
    keyShortcuts = arrayOf(keyShortcut),
    playClick = playClick,
    catchEvent = catchEvent,
    interaction = interaction
  )
}

/**
 * Call [interaction] when either the user clicks on the menu item or when pressing all the given
 * keys.
 */
fun Button.onInteract(
  stage: Stage,
  vararg keyShortcuts: IntArray,
  catchEvent: Boolean = false,
  playClick: Boolean = true,
  interaction: Button.() -> Unit
) {
  val interactionWithSound: Button.() -> Unit = {
    if (playClick) {
    }
    interaction()
  }
  onChange(interactionWithSound)

  if (keyShortcuts.isNotEmpty()) {
    if (this is MenuItem) {
      val first = keyShortcuts.firstOrNull { it.isNotEmpty() } ?: return
      setShortcut(*first)
    }
    for (keyShortcut in keyShortcuts) {
      if (keyShortcut.isEmpty()) continue
      stage += onAllKeysDownEvent(
        *keyShortcut,
        catchEvent = catchEvent,
        listener = {
          if (!isDisabled) {
            interactionWithSound()
          }
        }
      )
    }
  }
}

operator fun Stage.plusAssign(eventListener: EventListener) {
  addListener(eventListener)
}

/** Call listener if a key down event where the keycode is the given key is fired */
inline fun <T : Actor> T.onKeyDown(keycode: Int, catchEvent: Boolean = false, onlyWhenShown: Boolean = false, crossinline listener: T.() -> Unit) =
  onKeyDown(catchEvent) { eventKey ->
    if (eventKey == keycode && (!onlyWhenShown || isShown())) {
      listener()
    }
  }

/** Call listener when all of the given keys are pressed and one of them are in the fired onKeyDown event */
inline fun <T : Actor> T.onAllKeysDownEvent(vararg keycodes: Int, catchEvent: Boolean = false, onlyWhenShown: Boolean = false, crossinline listener: T.() -> Unit): EventListener {
  require(keycodes.isNotEmpty()) { "At least one key must be given" }
  return this.onKeyDown(catchEvent) { eventKey ->
    if ((!onlyWhenShown || isShown()) && eventKey in keycodes && keycodes.all {
        it == eventKey || Gdx.input.isKeyPressed(
          it
        )
      }
    ) {
      listener()
    }
  }
}

/** Call listener when any of the given keys are pressed */
inline fun <T : Actor> T.onAnyKeysDownEvent(vararg keycodes: Int, catchEvent: Boolean = false, onlyWhenShown: Boolean = false, crossinline listener: T.() -> Unit): EventListener {
  require(keycodes.isNotEmpty()) { "At least one key must be given" }
  return this.onKeyDown(catchEvent) { eventKey ->
    if ((!onlyWhenShown || isShown()) && eventKey in keycodes) {
      listener()
    }
  }
}

@Scene2dDsl
fun StageWidget.confirmWindow(title: String, text: String, whenDenied: VisWindow.() -> Unit = {}, whenConfirmed: VisWindow.() -> Unit): VisWindow {
  return this.ibVisWindow(title) {
    isMovable = false
    isModal = true
    close()

    visLabel(text)
    row()

    table { cell ->

      cell.fillX()
      cell.expandX()
      cell.space(10f)
      cell.pad(platformSpacing)

      row()

      visLabel("") {
        it.expandX()
        it.center()
      }

      visTextButton("Yes") {
        pad(platformButtonPadding)
        it.expandX()
        it.center()
        onClick {
          this@ibVisWindow.whenConfirmed()
          this@ibVisWindow.fadeOut()
        }
      }
      visLabel("") {
        it.expandX()
        it.center()
      }

      visTextButton("No") {
        pad(platformButtonPadding)
        it.expandX()
        it.center()
        onClick {
          this@ibVisWindow.whenDenied()
          this@ibVisWindow.fadeOut()
        }
      }
      visLabel("") {
        it.expandX()
        it.center()
      }
    }
    centerWindow()
    onAnyKeysDownEvent(Input.Keys.ESCAPE, Input.Keys.BACK, catchEvent = true) {
      this@ibVisWindow.fadeOut()
    }
    pack()
    fadeOut(0f)
  }
}

@Scene2dDsl
fun StageWidget.okWindow(title: String, labelUpdater: MutableMap<VisWindow, VisWindow.() -> Unit>, whenConfirmed: VisWindow.() -> Unit, text: () -> String): VisWindow {
  return ibVisWindow(title) {
    isMovable = false
    isModal = true
    this.close()
    val label = visLabel("")

    labelUpdater[this] = {
      label.setText(text())
      pack()
      centerWindow()
    }

    row()

    visTextButton("OK") {
      this.pad(platformButtonPadding)
      it.expandX()
      it.center()
      it.space(10f)
      it.pad(platformSpacing)
      onClick {
        this@ibVisWindow.whenConfirmed()
        this@ibVisWindow.fadeOut()
      }
    }

    pack()
    centerWindow()
    fadeOut(0f)
  }
}

@Scene2dDsl
inline fun RootWidget.ibVisWindow(title: String, style: String = defaultStyle, noinline onClose: () -> Unit = {}, init: IBVisWindow.() -> Unit = {}): IBVisWindow {
  contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
  return storeActor(IBVisWindow(title, style, onClose)).apply(init)
}

/**
 * A [IBVisWindow] that is initially closed
 */
@Scene2dDsl
inline fun ibVisWindowClosed(title: String, style: String = defaultStyle, noinline onClose: () -> Unit = {}, init: IBVisWindow.() -> Unit = {}): IBVisWindow {
  contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
  return IBVisWindow(title, style, onClose).apply(init)
}

@Scene2dDsl
class IBVisWindow(title: String, styleName: String, val onClose: () -> Unit) : VisWindow(title, styleName), KTable {
  public override fun close() {
    fadeOut(0f)
    onClose()
  }

  /** Add and fade in this window if it is is not [isShown] */
  fun show(stage: Stage, center: Boolean = true, fadeTime: Float = FADE_TIME) {
    if (!isShown()) {
      isVisible = true
      stage.addActor(fadeIn(fadeTime))
      if (center) {
        centerWindow()
      }
    }
  }

  /** Toggle if this window is shown or not */
  fun toggleShown(stage: Stage, center: Boolean = true) {
    if (!isShown()) {
      show(stage, center)
    } else {
      close()
    }
  }
}

/** Toggle if this window is shown or not */
@Suppress("NOTHING_TO_INLINE") // must be inlined otherwise it does not work (wtf!??)
@Deprecated("Use the extension function instead", ReplaceWith("this.closeOnEscape()"))
inline fun IBVisWindow.hideOnEscape() {
  onAnyKeysDownEvent(Input.Keys.ESCAPE, Input.Keys.BACK, catchEvent = true) {
    close()
  }
}

fun Table.setIBDefaults(space: Boolean = true, pad: Boolean = true) {
  defaults().applyIf(space) { space(5f) }.applyIf(pad) { padLeft(2.5f).padRight(2.5f).padBottom(2.5f) }
}
