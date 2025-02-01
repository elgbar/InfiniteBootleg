package no.elg.infiniteBootleg.core.util

/**
 * @author karl henrik
 * @since 0.1.0
 */
@Suppress("unused")
object ReflectionUtil {
  /**
   * @param clazz The class to get the static field from
   * @param field The name of the field to get
   * @return The object with the name `field` from the object `object`
   * @throws NoSuchFieldException   For same reason as [Field.get]
   * @throws IllegalAccessException For same reason as [Field.get]
   */
  @Throws(NoSuchFieldException::class, IllegalAccessException::class)
  fun getStaticField(clazz: Class<*>, field: String): Any {
    val objectField = clazz.getDeclaredField(field)
    objectField.isAccessible = true
    return objectField[null]
  }
}
