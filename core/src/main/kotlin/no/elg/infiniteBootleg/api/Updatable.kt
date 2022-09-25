package no.elg.infiniteBootleg.api

/**
 * @author Elg
 */
interface Updatable {
  /** Update the state, might be called every frame  */
  fun update()
}
