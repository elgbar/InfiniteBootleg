/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tomgrill.gdxtesting

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import io.mockk.mockk
import no.elg.infiniteBootleg.ServerMain
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.Extension

class GdxTestRunner : ApplicationListener, Extension {
  override fun create() {}
  override fun resize(width: Int, height: Int) {}
  override fun render() {}
  override fun pause() {}
  override fun resume() {}
  override fun dispose() {}

  companion object {
    @JvmStatic
    @BeforeAll
    fun beforeAll(gdxTestRunner: GdxTestRunner) {
      val conf = HeadlessApplicationConfiguration()
      HeadlessApplication(ServerMain(true, null), conf)
      Gdx.gl = mockk(relaxed = true)
      Gdx.app = mockk(relaxed = true)
      Gdx.graphics = mockk(relaxed = true)
      Gdx.input = mockk(relaxed = true)
      Gdx.net = mockk(relaxed = true)
    }
  }
}
