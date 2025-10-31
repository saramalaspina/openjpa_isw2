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

package org.apache.openjpa.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests for the CacheMap Constructor")
class CacheMapConstructorISWTest {

    private static Stream<Arguments> provideConstructorArguments() {
        return Stream.of(
                // The structure of the arguments is:
                // Test Name, lru, max, expectedException, expectedCacheSize
                Arguments.of("LRU with Unlimited Cache but Invalid Size", true, -1, IllegalArgumentException.class, -1),
                Arguments.of("Non-LRU with Disabled Cache", false, 0, null, 0),
                Arguments.of("LRU with Minimum Positive Cache", true, 2, null, 2),
                Arguments.of("Non-LRU with Typical Cache Size", false, 100, null, 100),

                // Test added after Jacoco report
                Arguments.of("LRU with Unlimited Cache and Valid Size", true, -2, null, -1)
        );
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideConstructorArguments")
    void constructorShouldInitializeStateCorrectly(String testName, boolean lru, int max,  Class<? extends Throwable> expectedException, int expectedCacheSize) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> {
                new CacheMap(lru, max);
            }, "An exception of type " + expectedException.getSimpleName() + " was expected for this configuration.");
        } else {
            CacheMap map = new CacheMap(lru, max);
            assertAll("State verification for: " + testName,
                    () -> assertNotNull(map, "The CacheMap object should not be null."),
                    () -> assertTrue(map.isEmpty(), "A newly created map should be empty."),
                    () -> assertEquals(lru, map.isLRU(), "The LRU state is not as expected."),
                    () -> assertEquals(expectedCacheSize, map.getCacheSize(), "The main cache size is not as expected."),
                    () -> assertEquals(-1, map.getSoftReferenceSize(), "The soft cache size is not as expected.")
            );
        }
    }
}