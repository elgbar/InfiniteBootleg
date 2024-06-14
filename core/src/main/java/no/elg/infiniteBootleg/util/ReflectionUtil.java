package no.elg.infiniteBootleg.util;

import java.lang.reflect.Field;

/**
 * @author karl henrik
 * @since 0.1.0
 */
@SuppressWarnings("unused")
public final class ReflectionUtil {

  private ReflectionUtil() {
  }

  /**
   * @param clazz The class to get the static field from
   * @param field The name of the field to get
   * @return The object with the name {@code field} from the object {@code object}
   * @throws NoSuchFieldException   For same reason as {@link Field#get(Object)}
   * @throws IllegalAccessException For same reason as {@link Field#get(Object)}
   */
  public static Object getStaticField(Class<?> clazz, String field)
    throws NoSuchFieldException, IllegalAccessException {
    Field objectField = clazz.getDeclaredField(field);
    objectField.setAccessible(true);
    return objectField.get(null);
  }
}
