/*
 * Copyright 2015 Michael Rozumyanskiy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.michaelrocks.lightsaber

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import javax.inject.Named
import javax.inject.Singleton

class ChildInjectionTest {
  private lateinit var lightsaber: Lightsaber

  @Before
  fun createLightsaber() {
    lightsaber = Lightsaber()
  }

  @Test
  fun testCreateSingletonBeforeChildInjector() {
    val parentInjector = lightsaber.createInjector(ParentModule())
    val parentString = parentInjector.getInstance<String>()
    assertEquals("Parent String", parentString)
    val childInjector = lightsaber.createChildInjector(parentInjector, ChildModule())
    assertSame(parentString, parentInjector.getInstance<String>())
    assertSame(parentString, childInjector.getInstance<String>())
  }

  @Test
  fun testCreateSingletonAfterChildInjector() {
    val parentInjector = lightsaber.createInjector(ParentModule())
    val childInjector = lightsaber.createChildInjector(parentInjector, ChildModule())
    val parentString = parentInjector.getInstance<String>()
    assertEquals("Parent String", parentString)
    assertSame(parentString, parentInjector.getInstance<String>())
    assertSame(parentString, childInjector.getInstance<String>())
  }

  @Test
  fun testCreateSingletonInChildInjector() {
    val parentInjector = lightsaber.createInjector(ParentModule())
    val childInjector = lightsaber.createChildInjector(parentInjector, ChildModule())
    val childString = childInjector.getInstance<String>()
    assertEquals("Parent String", childString)
    assertSame(childString, parentInjector.getInstance<String>())
    assertSame(childString, childInjector.getInstance<String>())
  }

  @Test
  fun testCreateSingletonInTwoChildInjectors() {
    val parentInjector = lightsaber.createInjector(ParentModule())
    val childInjector1 = lightsaber.createChildInjector(parentInjector, ChildModule())
    val childInjector2 = lightsaber.createChildInjector(parentInjector, ChildModule())
    val childObject1 = childInjector1.getInstance<Any>()
    val childObject2 = childInjector2.getInstance<Any>()
    assertEquals("Child Object", childObject1)
    assertEquals("Child Object", childObject2)
    assertNotSame(childObject1, childObject2)
  }

  @Test(expected = ConfigurationException::class)
  fun testCreateChildInjectorWithParentModule() {
    val parentInjector = lightsaber.createInjector(ParentModule())
    val childInjector = lightsaber.createChildInjector(parentInjector, ParentModule())
    val childString = childInjector.getInstance<String>()
    assertEquals("Parent String", childString)
  }

  @Module
  private class ParentModule {
    @Provides
    @Singleton
    fun provideString(): String = StringBuilder("Parent String").toString()
  }

  @Module
  private class ChildModule {
    @Provides
    @Singleton
    fun provideObject(): Any = StringBuilder("Child Object").toString()

    @Provides
    @Singleton
    @Named("Child String")
    fun provideNamedString(): String = StringBuilder("Child String").toString()
  }
}