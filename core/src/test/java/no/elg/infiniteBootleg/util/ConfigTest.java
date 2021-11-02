package no.elg.infiniteBootleg.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/** @author Elg */
public class ConfigTest {

  private Config config;

  @Before
  public void setUp() {
    config = new Config();
  }

  @Test
  public void testGetSet() {
    Assert.assertNull(config.get("whatever"));
    config.set("Hi", 123);
    Assert.assertEquals(123, (int) config.get("Hi"));
  }

  @SuppressWarnings({"ConstantConditions", "unused"})
  @Test(expected = ClassCastException.class)
  public void throwsClassCastException() {
    config.set("Hi", "oh no");
    int a = config.get("Hi");
  }
}
