package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.PopupMenu
import com.kotcrab.vis.ui.widget.Separator
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisWindow
import com.kotcrab.vis.ui.widget.VisWindow.FADE_TIME
import ktx.actors.isShown
import ktx.actors.onChange
import ktx.actors.onKeyDown
import ktx.scene2d.Scene2dDsl

/**
 * Alias for [PopupMenu.addSeparator] to make it blend better in with the scene 2d DSL, but without
 * the padding
 */
@Scene2dDsl
fun PopupMenu.separator() {
  add(Separator("menu")).fill().expand().row()
}

fun Button.onInteract(stage: Stage, vararg keyShortcut: Int, catchEvent: Boolean = false, interaction: Button.() -> Unit) {
  this.onInteract(
    stage = stage,
    keyShortcuts = arrayOf(keyShortcut),
    catchEvent = catchEvent,
    interaction = interaction
  )
}

/**
 * Call [interaction] when either the user clicks on the menu item or when pressing all the given
 * keys.
 */
fun Button.onInteract(stage: Stage, vararg keyShortcuts: IntArray, catchEvent: Boolean = false, interaction: Button.() -> Unit) {
  onChange(interaction)

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
            interaction()
          }
        }
      )
    }
  }
}

/** Add and fade in this window if it is not [isShown] */
fun VisWindow.show(stage: Stage, center: Boolean = true, fadeTime: Float = FADE_TIME) {
  isVisible = true
  if (!isShown()) {
    stage.addActor(fadeIn(fadeTime))
    if (center) {
      centerWindow()
    }
  }
}

/** Alias for [VisWindow.fadeOut] with a default fadeout duration of `0f` */
fun VisWindow.hide(fadeTime: Float = 0f) {
  fadeOut(fadeTime)
  isVisible = false
}

/** Toggle if this window is shown or not */
fun VisWindow.toggleShown(stage: Stage, center: Boolean = true) {
  if (!isShown()) {
    show(stage, center)
  } else {
    hide()
  }
}

/** Toggle if this window is shown or not */
fun VisWindow.hideOnEscape() {
  onKeyDown(Input.Keys.ESCAPE) { hide() }
}

fun VisWindow.addHideButton(fadeTime: Float = 0f) {
  val titleLabel = titleLabel
  val titleTable = titleTable

  val closeButton = VisImageButton("close-window")
  titleTable.add(closeButton).padRight(-padRight + 0.7f)
  closeButton.addListener(object : ChangeListener() {
    override fun changed(event: ChangeEvent, actor: Actor) {
      hide(fadeTime)
    }
  })
  closeButton.addListener(object : ClickListener() {
    override fun touchDown(
      event: InputEvent,
      x: Float,
      y: Float,
      pointer: Int,
      button: Int
    ): Boolean {
      event.cancel()
      return true
    }
  })
  if (titleLabel.labelAlign == Align.center && titleTable.children.size == 2) titleTable.getCell(titleLabel).padLeft(closeButton.width * 2)
}

fun Table.setIBDefaults() {
  defaults().space(5f).padLeft(2.5f).padRight(2.5f).padBottom(2.5f)
}

operator fun Stage.plusAssign(eventListener: EventListener) {
  addListener(eventListener)
}

/** Call listener if a key down event where the keycode is the given key is fired */
inline fun <T : Actor> T.onKeyDown(keycode: Int, catchEvent: Boolean = false, onlyWhenShown: Boolean = false, crossinline listener: T.() -> Unit): EventListener {
  return onKeyDown(catchEvent) { eventKey ->
    if (eventKey == keycode && (!onlyWhenShown || isShown())) {
      listener()
    }
  }
}

/** Call listener when all of the given keys are pressed and one of them are in the fired onKeyDown event */
inline fun <T : Actor> T.onAllKeysDownEvent(vararg keycodes: Int, catchEvent: Boolean = false, onlyWhenShown: Boolean = false, crossinline listener: T.() -> Unit): EventListener {
  require(keycodes.isNotEmpty()) { "At least one key must be given" }
  return this.onKeyDown(catchEvent) { eventKey ->
    if ((!onlyWhenShown || isShown()) && eventKey in keycodes && keycodes.all { it == eventKey || Gdx.input.isKeyPressed(it) }) {
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
