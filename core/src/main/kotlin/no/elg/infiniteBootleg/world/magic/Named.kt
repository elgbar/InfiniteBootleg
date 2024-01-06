package no.elg.infiniteBootleg.world.magic

interface Named {
  /**
   * The name to display to the user
   */
  val displayName: String

  /**
   * The name to use when serializing. This must be stable and unique to the type
   */
  val serializedName: String get() = displayName
}

interface Description {
  val description: String
}
