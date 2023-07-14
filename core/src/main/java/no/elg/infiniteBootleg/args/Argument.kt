package no.elg.infiniteBootleg.args

/**
 * @author Elg
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Argument(
  /**
   * @return Help string
   */
  val value: String,
  /**
   * @return Single char alias
   */
  val alt: Char = '\u0000'
)
