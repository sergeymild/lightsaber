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

package com.michaelrocks.lightsaber.processor.descriptors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.objectweb.asm.Type;

public class ParameterizedType {
    private final Type rawType;
    private final Type parameterType;

    public ParameterizedType(final Type rawType) {
        this(rawType, null);
    }

    public ParameterizedType(final Type rawType, final Type parameterType) {
        this.rawType = rawType;
        this.parameterType = parameterType;
    }

    public static ParameterizedType fromType(final Type type) {
        return new ParameterizedType(type, null);
    }

    public Type getRawType() {
        return rawType;
    }

    public Type getParameterType() {
        return parameterType;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        final ParameterizedType that = (ParameterizedType) object;
        return new EqualsBuilder()
                .append(rawType, that.rawType)
                .append(parameterType, that.parameterType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(rawType)
                .append(parameterType)
                .toHashCode();
    }

    @Override
    public String toString() {
        if (parameterType == null) {
            return rawType.toString();
        } else {
            return rawType + "<" + parameterType + ">";
        }
    }
}
