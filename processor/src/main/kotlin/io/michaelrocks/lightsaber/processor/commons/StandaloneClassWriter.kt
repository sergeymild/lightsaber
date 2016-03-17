/*
 * Copyright 2016 Michael Rozumyanskiy
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

package io.michaelrocks.lightsaber.processor.commons

import io.michaelrocks.grip.ClassRegistry
import io.michaelrocks.grip.mirrors.ClassMirror
import io.michaelrocks.lightsaber.processor.logging.getLogger
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import java.util.*

class StandaloneClassWriter : ClassWriter {
  private val logger = getLogger()
  private val classRegistry: ClassRegistry

  constructor(flags: Int, classRegistry: ClassRegistry) : super(flags) {
    this.classRegistry = classRegistry
  }

  constructor(classReader: ClassReader, flags: Int, classRegistry: ClassRegistry) : super(classReader, flags) {
    this.classRegistry = classRegistry
  }

  override fun getCommonSuperClass(type1: String, type2: String): String {
    val hierarchy = HashSet<Type>()
    for (mirror in classRegistry.findClassHierarchy(Type.getObjectType(type1))) {
      hierarchy.add(mirror.type)
    }

    for (mirror in classRegistry.findClassHierarchy(Type.getObjectType(type2))) {
      if (mirror.type in hierarchy) {
        logger.debug("[getCommonSuperClass]: {} & {} = {}", type1, type2, mirror.access)
        return mirror.type.internalName
      }
    }

    logger.warn("[getCommonSuperClass]: {} & {} = NOT FOUND ", type1, type2)
    return Types.OBJECT_TYPE.internalName
  }

  private fun ClassRegistry.findClassHierarchy(type: Type): Sequence<ClassMirror> {
    return generateSequence(getClassMirror(type)) {
      it.superType?.let { getClassMirror(it) }
    }
  }
}
