package no.elg.infiniteBootleg

import de.tomgrill.gdxtesting.GdxTestRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(GdxTestRunner::class)
@TestInstance(Lifecycle.PER_CLASS)
open class TestGraphic {

  @Test
  fun dummy() {
    // This dummy test is needed for @RunWith(GdxTestRunner.class) to work
  }

  companion object {
    init {
      Settings.schedulerThreads = 0
      Settings.client = false
      Settings.loadWorldFromDisk = false
      Settings.debug = true
    }
  }
}
