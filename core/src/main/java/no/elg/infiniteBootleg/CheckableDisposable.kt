package no.elg.infiniteBootleg

import com.badlogic.gdx.utils.Disposable

interface CheckableDisposable : Disposable {

  /**
   * If this has been disposed
   */
  val disposed: Boolean

  /**
   * Call [dispose] if [disposed] is `false`
   */
  fun tryDispose() {
    if (disposed) {
      return
    }
    dispose()
  }
}
