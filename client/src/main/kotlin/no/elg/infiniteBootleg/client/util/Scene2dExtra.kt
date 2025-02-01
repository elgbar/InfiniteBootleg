package no.elg.infiniteBootleg.client.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.PopupMenu
import com.kotcrab.vis.ui.widget.Separator
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.isShown
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.scene2d.KTable
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.defaultStyle
import ktx.scene2d.table
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.events.InterfaceEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.inventory.container.InterfaceId
import no.elg.infiniteBootleg.core.util.applyIf
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
fun ClientWorld.confirmWindow(title: String, text: String, whenDenied: VisWindow.() -> Unit = {}, whenConfirmed: VisWindow.() -> Unit): VisWindow {
  return ibVisWindowClosed(title, title) {
    isMovable = false
    isModal = true

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
          this@ibVisWindowClosed.whenConfirmed()
          this@ibVisWindowClosed.fadeOut()
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
          this@ibVisWindowClosed.whenDenied()
          this@ibVisWindowClosed.fadeOut()
        }
      }
      visLabel("") {
        it.expandX()
        it.center()
      }
    }
    centerWindow()
    onAnyKeysDownEvent(Input.Keys.ESCAPE, Input.Keys.BACK, catchEvent = true) {
      this@ibVisWindowClosed.fadeOut()
    }
    pack()
    fadeOut(0f)
  }
}

@Scene2dDsl
fun ClientWorld.okWindow(title: String, labelUpdater: MutableMap<VisWindow, VisWindow.() -> Unit>, whenConfirmed: VisWindow.() -> Unit, text: () -> String): VisWindow {
  return ibVisWindowClosed(title, title) {
    isMovable = false
    isModal = true
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
        this@ibVisWindowClosed.whenConfirmed()
        this@ibVisWindowClosed.fadeOut()
      }
    }

    pack()
    centerWindow()
    fadeOut(0f)
  }
}

/**
 * A [IBVisWindow] that is initially closed
 */
@Scene2dDsl
inline fun ClientWorld.ibVisWindowClosed(title: String, interfaceId: InterfaceId = title, style: String = defaultStyle, init: IBVisWindow.() -> Unit = {}): IBVisWindow {
  contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
  return IBVisWindow(title, style, this, interfaceId).apply(init)
}

@Suppress("jol")
@Scene2dDsl
class IBVisWindow(
  title: String,
  styleName: String,
  val world: ClientWorld,
  val interfaceId: InterfaceId
) : VisWindow(title, styleName), KTable, Disposable {

  private val interfaceManager = world.render.interfaceManager

  init {
    interfaceManager.addInterface(interfaceId, this)
  }

  public override fun close() {
    if (isShown()) {
      fadeOut(0f)
      EventManager.dispatchEvent(InterfaceEvent.Closed(interfaceId))
    }
  }

  /** Add and fade in this window if it is not [isShown] */
  fun show(stage: Stage, center: Boolean = true, fadeTime: Float = 0f) {
    if (!isShown()) {
      EventManager.dispatchEvent(InterfaceEvent.Opening(interfaceId))
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

  override fun dispose() {
    close()
    interfaceManager.removeInterface(interfaceId)
  }
}

fun Table.setIBDefaults(space: Boolean = true, pad: Boolean = true) {
  defaults().applyIf(space) { space(5f) }.applyIf(pad) { padLeft(2.5f).padRight(2.5f).padBottom(2.5f) }
}
