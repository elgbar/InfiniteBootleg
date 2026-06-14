package no.elg.infiniteBootleg.core.world

/**
 * Categories of materials useful to group similar [Material] together
 */
enum class MaterialCategory {
  /**
   * Loose natural occurring material
   */
  SOIL,

  /**
   * Hard natural occurring material
   */
  PLAIN_ROCK,

  /**
   * Natural material containing a resource
   */
  ORE,

  /**
   * Living stuff, which are also material
   */
  ORGANIC,

  /**
   * Man-made material
   */
  CRAFTED,

  /**
   * Magically created material
   */
  MAGIC
}
