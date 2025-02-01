package no.elg.infiniteBootleg.core.world.ecs.api.restriction.component

/**
 * Informational component that this entity is a tag
 */
interface TagComponent : DebuggableComponent {
  override fun hudDebug(): String = "A tag"
}
