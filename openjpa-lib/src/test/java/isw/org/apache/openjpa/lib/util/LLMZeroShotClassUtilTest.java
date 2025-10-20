/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package isw.org.apache.openjpa.lib.util;

import org.apache.openjpa.lib.util.ClassUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JUnit 5 test case for {@link ClassUtil#toClass(String, boolean, ClassLoader)}.
 */
class LLMZeroShotClassUtilTest {

    // A public static nested class for testing purposes
    public static class NestedTestClass {
    }

    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Test
    @DisplayName("Test toClass with a null string should throw NullPointerException")
    void testToClassWithNullString() {
        assertThrows(NullPointerException.class, () -> ClassUtil.toClass(null, false, classLoader),
                "Calling toClass with a null string should throw NullPointerException.");
    }

    @Test
    @DisplayName("Test toClass with an empty string should throw IllegalArgumentException")
    void testToClassWithEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.toClass("", false, classLoader),
                "Calling toClass with an empty string should result in an IllegalArgumentException.");
    }

    @Test
    @DisplayName("Test toClass with a non-existent class should throw IllegalArgumentException")
    void testToClassWithNonExistentClass() {
        String nonExistentClassName = "org.apache.openjpa.lib.util.NonExistentClass";
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.toClass(nonExistentClassName, false, classLoader),
                "Should throw IllegalArgumentException for a class that cannot be found.");
    }

    private static Stream<Arguments> primitiveClassesProvider() {
        return Stream.of(
                Arguments.of("byte", byte.class),
                Arguments.of("char", char.class),
                Arguments.of("double", double.class),
                Arguments.of("float", float.class),
                Arguments.of("int", int.class),
                Arguments.of("long", long.class),
                Arguments.of("short", short.class),
                Arguments.of("boolean", boolean.class),
                Arguments.of("void", void.class)
        );
    }

    @ParameterizedTest(name = "Test toClass for primitive type ''{0}'' with resolve={1}")
    @MethodSource("primitiveClassesProvider")
    @DisplayName("Test toClass correctly handles all primitive types")
    void testToClassWithPrimitiveTypes(String className, Class<?> expectedClass) {
        // Test with resolve = false
        assertEquals(expectedClass, ClassUtil.toClass(className, false, classLoader),
                "Failed to convert '" + className + "' to its primitive class type with resolve=false.");

        // Test with resolve = true
        assertEquals(expectedClass, ClassUtil.toClass(className, true, classLoader),
                "Failed to convert '" + className + "' to its primitive class type with resolve=true.");
    }


    private static Stream<Arguments> standardClassesProvider() {
        return Stream.of(
                Arguments.of("java.lang.String", String.class),
                Arguments.of("java.lang.Object", Object.class),
                Arguments.of("java.util.Date", java.util.Date.class),
                Arguments.of(NestedTestClass.class.getName(), NestedTestClass.class)
        );
    }

    @ParameterizedTest(name = "Test toClass for standard class ''{0}'' with resolve={1}")
    @MethodSource("standardClassesProvider")
    @DisplayName("Test toClass handles fully qualified class names")
    void testToClassWithStandardClasses(String className, Class<?> expectedClass) {
        // Test with resolve = false
        assertEquals(expectedClass, ClassUtil.toClass(className, false, classLoader));

        // Test with resolve = true
        assertEquals(expectedClass, ClassUtil.toClass(className, true, classLoader));
    }

    @Test
    @DisplayName("Test toClass with null ClassLoader uses context ClassLoader")
    void testToClassWithNullClassLoader() {
        Class<?> resultClass = ClassUtil.toClass("java.lang.Integer", false, null);
        assertNotNull(resultClass, "The resulting class should not be null.");
        assertEquals(Integer.class, resultClass, "Should correctly load class using the thread's context class loader.");
    }

    private static Stream<Arguments> arrayClassesProvider() {
        return Stream.of(
                // Primitive arrays
                Arguments.of("int[]", int[].class),
                Arguments.of("double[][]", double[][].class),
                Arguments.of("boolean[][][]", boolean[][][].class),
                // Object arrays
                Arguments.of("java.lang.String[]", String[].class),
                Arguments.of("java.lang.Object[][]", Object[][].class),
                Arguments.of(NestedTestClass.class.getName() + "[]", NestedTestClass[].class)
        );
    }

    @ParameterizedTest(name = "Test toClass for array type ''{0}''")
    @MethodSource("arrayClassesProvider")
    @DisplayName("Test toClass handles primitive and object arrays of various dimensions")
    void testToClassWithArrayTypes(String className, Class<?> expectedClass) {
        // Test with resolve = false
        assertEquals(expectedClass, ClassUtil.toClass(className, false, classLoader),
                "Failed to convert '" + className + "' to its array class type with resolve=false.");

        // Test with resolve = true
        assertEquals(expectedClass, ClassUtil.toClass(className, true, classLoader),
                "Failed to convert '" + className + "' to its array class type with resolve=true.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"java.lang.String[", "int[[]]", "[]", "java.lang.NonExistent[]"})
    @DisplayName("Test toClass with invalid array syntax throws exception")
    void testToClassWithInvalidArraySyntax(String invalidClassName) {
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.toClass(invalidClassName, false, classLoader),
                "Should throw IllegalArgumentException for invalid array syntax: " + invalidClassName);
    }
}
