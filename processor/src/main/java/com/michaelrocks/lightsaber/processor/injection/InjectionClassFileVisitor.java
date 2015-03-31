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

package com.michaelrocks.lightsaber.processor.injection;

import com.michaelrocks.lightsaber.processor.ProcessorContext;
import com.michaelrocks.lightsaber.processor.generation.ClassProducer;
import com.michaelrocks.lightsaber.processor.io.ClassFileVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;

public class InjectionClassFileVisitor extends ClassFileVisitor {
    private final ClassProducer classProducer;
    private final ProcessorContext processorContext;

    public InjectionClassFileVisitor(final ClassFileVisitor classFileVisitor, final ClassProducer classProducer,
            final ProcessorContext processorContext) {
        super(classFileVisitor);
        this.classProducer = classProducer;
        this.processorContext = processorContext;
    }

    @Override
    public void visitClassFile(final String path, final byte[] classData) throws IOException {
        final ClassReader classReader = new ClassReader(classData);
        final ClassWriter classWriter =
                new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classReader.accept(new RootVisitor(classWriter, classProducer, processorContext), ClassReader.SKIP_FRAMES);
        super.visitClassFile(path, classWriter.toByteArray());
    }
}