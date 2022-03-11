package no.elg.infiniteBootleg

import com.badlogic.gdx.utils.Disposable

interface CheckableDisposable : Disposable {

  /**
   * If this has been disposed
   */
  fun isDisposed(): Boolean
}
