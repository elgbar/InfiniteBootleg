package no.elg.infiniteBootleg.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author karl henrik
 * @since 0.1.0
 */
@SuppressWarnings("unused")
public final class ReflectionUtil {

  private ReflectionUtil() {}

  /**
   * Get object from superclass
   *
   * @param object The object to get the field from
   * @param field The field to get the object of
   * @return The object with the name {@code field} from the object {@code object}
   * @throws NoSuchFieldException For same reason as {@link Field#get(Object)}
   * @throws IllegalAccessException For same reason as {@link Field#get(Object)}
   */
  public static Object getSuperField(Object object, String field)
      throws NoSuchFieldException, IllegalAccessException {
    Class<?> c = object.getClass().getSuperclass();
    Field objectField = c.getDeclaredField(field);
    objectField.setAccessible(true);
    Object result = objectField.get(object);
    objectField.setAccessible(false);
    return result;
  }

  /**
   * @param object The object to get the field from
   * @param field The name of the field to get
   * @return The object with the name {@code field} from the object {@code object}
   * @throws NoSuchFieldException For same reason as {@link Field#get(Object)}
   * @throws IllegalAccessException For same reason as {@link Field#get(Object)}
   */
  public static Object getField(Object object, String field)
      throws NoSuchFieldException, IllegalAccessException {
    Class<?> c = object.getClass();
    Field objectField = c.getDeclaredField(field);
    objectField.setAccessible(true);
    Object result = objectField.get(object);
    objectField.setAccessible(false);
    return result;
  }

  /**
   * @param clazz The class to get the static field from
   * @param field The name of the field to get
   * @return The object with the name {@code field} from the object {@code object}
   * @throws NoSuchFieldException For same reason as {@link Field#get(Object)}
   * @throws IllegalAccessException For same reason as {@link Field#get(Object)}
   */
  public static Object getStaticField(Class<?> clazz, String field)
      throws NoSuchFieldException, IllegalAccessException {
    Field objectField = clazz.getDeclaredField(field);
    objectField.setAccessible(true);
    Object result = objectField.get(null);
    objectField.setAccessible(false);
    return result;
  }

  /**
   * Modify an inaccessible field
   *
   * @param object The object to get the field from
   * @param field The field to get the object of
   * @param newValue The new value of the field {@code field} in the object {@code object}
   * @throws NoSuchFieldException For same reason as {@link Field#get(Object)}
   * @throws IllegalAccessException For same reason as {@link Field#get(Object)}
   */
  public static void modifyField(Object object, String field, Object newValue)
      throws NoSuchFieldException, IllegalAccessException {
    Class<?> c = object.getClass();
    Field objectField = c.getDeclaredField(field);
    objectField.setAccessible(true);
    objectField.set(object, newValue);
    objectField.setAccessible(false);
  }

  /**
   * Modify an inaccessible static field
   *
   * @param clazz The clazz to get the field from
   * @param field The field to get the object of
   * @param newValue The new value of the field {@code field} in the object {@code object}
   * @throws NoSuchFieldException For same reason as {@link Class#getDeclaredField(String)}
   * @throws IllegalAccessException For same reason as {@link Field#set(Object, Object)}
   */
  public static void modifyStaticField(Class<?> clazz, String field, Object newValue)
      throws NoSuchFieldException, IllegalAccessException {
    Field objectField = clazz.getDeclaredField(field);
    objectField.setAccessible(true);
    objectField.set(null, newValue);
  }

  /**
   * Modify an inaccessible field of the super class
   *
   * @param object The object to get the field from
   * @param field The field to get the object of
   * @param newValue The new value of the field {@code field} in the object {@code object}
   * @throws NoSuchFieldException For same reason as {@link Field#get(Object)}
   * @throws IllegalAccessException For same reason as {@link Field#get(Object)}
   */
  public static void modifySuperField(Object object, String field, Object newValue)
      throws NoSuchFieldException, IllegalAccessException {
    Class<?> c = object.getClass().getSuperclass();
    Field objectField = c.getDeclaredField(field);
    objectField.setAccessible(true);
    objectField.set(object, newValue);
    objectField.setAccessible(false);
  }

  public static Method getMethod(Class<?> clazz, String name, Class<?>... args) {
    try {
      return clazz.getDeclaredMethod(name, args);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }
}
