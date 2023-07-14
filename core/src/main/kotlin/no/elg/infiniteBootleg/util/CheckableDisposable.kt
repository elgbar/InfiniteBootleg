package no.elg.infiniteBootleg.util

import com.badlogic.gdx.utils.Disposable

interface CheckableDisposable : Disposable {

  /**
   * If this has been disposed
   */
  val isDisposed: Boolean
}
