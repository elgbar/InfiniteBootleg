package no.elg.infiniteBootleg

import com.badlogic.gdx.utils.Disposable

interface CheckableDisposable : Disposable {

  val disposed: Boolean
}
