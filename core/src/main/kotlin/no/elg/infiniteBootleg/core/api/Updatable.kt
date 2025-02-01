package no.elg.infiniteBootleg.core.api

/**
 * @author Elg
 */
interface Updatable {
  /** Update the state, might be called every frame  */
  fun update()
}
