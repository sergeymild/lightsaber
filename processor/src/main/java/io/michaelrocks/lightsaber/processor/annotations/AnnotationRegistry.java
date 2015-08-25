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

package io.michaelrocks.lightsaber.processor.annotations;

import org.apache.commons.lang3.Validate;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationRegistry {
    private final Map<Type, AnnotationData> unresolvedDefaultsByType = new HashMap<>();
    private final Map<Type, AnnotationData> resolvedDefaultsByType = new HashMap<>();

    public void addAnnotationDefaults(final AnnotationData annotationDefaults) {
        if (annotationDefaults.isResolved()) {
            Validate.isTrue(!unresolvedDefaultsByType.containsKey(annotationDefaults.getType()));
            resolvedDefaultsByType.put(annotationDefaults.getType(), annotationDefaults);
        } else {
            Validate.isTrue(!resolvedDefaultsByType.containsKey(annotationDefaults.getType()));
            unresolvedDefaultsByType.put(annotationDefaults.getType(), annotationDefaults);
        }
    }

    public AnnotationData resolveAnnotation(final AnnotationData annotation) {
        if (annotation.isResolved()) {
            return annotation;
        }

        return new AnnotationResolver().resolve(annotation);
    }

    boolean hasUnresolvedDefaults(final Type annotationType) {
        return unresolvedDefaultsByType.containsKey(annotationType);
    }

    boolean hasResolvedDefaults(final Type annotationType) {
        return resolvedDefaultsByType.containsKey(annotationType);
    }

    private class AnnotationResolver {
        private final Map<String, Object> values = new HashMap<>();
        private boolean resolved = true;

        AnnotationData resolve(final AnnotationData annotation) {
            return resolve(annotation, true);
        }

        private AnnotationData resolve(final AnnotationData annotation, final boolean applyDefaults) {
            if (applyDefaults) {
                applyDefaults(annotation);
            }
            for (final Map.Entry<String, Object> entry : annotation.getValues().entrySet()) {
                values.put(entry.getKey(), resolveObject(entry.getValue()));
            }
            return new AnnotationData(annotation.getType(), Collections.unmodifiableMap(values), resolved);
        }

        private void applyDefaults(final AnnotationData annotation) {
            final AnnotationData defaults = resolveDefaults(annotation.getType());
            if (defaults == null) {
                resolved = false;
                return;
            }

            resolved &= defaults.isResolved();
            values.putAll(defaults.getValues());
        }

        private AnnotationData resolveDefaults(final Type annotationType) {
            AnnotationData resolvedDefaults = resolvedDefaultsByType.get(annotationType);
            if (resolvedDefaults == null) {
                final AnnotationData unresolvedDefaults = unresolvedDefaultsByType.remove(annotationType);
                if (unresolvedDefaults != null) {
                    resolvedDefaults = unresolvedDefaults.isResolved()
                            ? unresolvedDefaults
                            : new AnnotationResolver().resolve(unresolvedDefaults, false);
                    resolvedDefaultsByType.put(annotationType, resolvedDefaults);
                }
            }
            return resolvedDefaults;
        }

        private Object resolveObject(final Object value) {
            if (value instanceof AnnotationData) {
                final AnnotationData annotation = resolveAnnotation((AnnotationData) value);
                resolved &= annotation.isResolved();
                return annotation;
            } else if (value instanceof List<?>) {
                return resolveArray((List<?>) value);
            } else {
                return value;
            }
        }

        private List<?> resolveArray(final List<?> array) {
            final List<Object> resolvedArray = new ArrayList<>(array.size());
            for (final Object value : array) {
                resolvedArray.add(resolveObject(value));
            }
            return resolvedArray;
        }
    }
}
