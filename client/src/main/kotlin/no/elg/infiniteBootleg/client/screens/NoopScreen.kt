package no.elg.infiniteBootleg.client.screens

/**
 * A screen that does nothing
 *
 * @author Elg
 */
class NoopScreen : AbstractScreen() {
  override fun render(delta: Float) = Unit

  override fun create() = Unit
}
