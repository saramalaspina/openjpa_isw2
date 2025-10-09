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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests for the ClassUtil.toClass() method")
class ClassUtilToClassTest {

    // ClassLoader that will fail to load any class
    private static final ClassLoader emptyClassLoader = new URLClassLoader(new URL[0], null);

    // The default system ClassLoader, which can find application and system classes
    private static final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

    private static Stream<Arguments> provideToClassArguments() {
        return Stream.of(
                // The structure of the arguments is:
                // Test Name, className, resolve, loader, expectedResult, expectedException

                // === Failure Scenarios ===
                Arguments.of("Null class name", null, true, systemClassLoader, null, NullPointerException.class),
                Arguments.of("Empty class name", "", false, systemClassLoader, null, IllegalArgumentException.class),
                Arguments.of("Non-existent class name", "com.NonExistentClass", true, systemClassLoader, null, IllegalArgumentException.class),
                Arguments.of("Failing ClassLoader (Mock) for a project class", "org.apache.openjpa.lib.util.ClassUtil", false, emptyClassLoader, null, IllegalArgumentException.class),

                // === Success Scenarios ===
                Arguments.of("Standard reference type with system loader", "org.apache.openjpa.lib.util.ClassUtil", false, systemClassLoader, ClassUtil.class, null),
                Arguments.of("Standard reference type with null loader", "java.lang.String", true, null, String.class, null),
                Arguments.of("Primitive type", "int", false, systemClassLoader, int.class, null),
                Arguments.of("Reference type array with null loader", "java.lang.String[]", true, null, String[].class, null),
                Arguments.of("Primitive type array", "int[]", false, systemClassLoader, int[].class, null)
        );
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideToClassArguments")
    void toClassShouldHandleVariousInputsCorrectly(String testName, String className, boolean resolve, ClassLoader loader, Class<?> expectedResult, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> {
                ClassUtil.toClass(className, resolve, loader);
            }, "An exception of type " + expectedException.getSimpleName() + " was expected for this input.");
        } else {
            Class<?> actualResult = ClassUtil.toClass(className, resolve, loader);
            assertNotNull(actualResult, "The resulting class should not be null for a successful conversion.");
            assertEquals(expectedResult, actualResult, "The returned Class object is not the one expected.");
        }
    }
}