package no.elg.infiniteBootleg.core.console

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CmdArgNames(
  /**
   * @return Names of arguments to the annotated command (in order)
   */
  vararg val value: String
)
