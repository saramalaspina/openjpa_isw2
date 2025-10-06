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

package isw.org.apache.openjpa.util;

import org.apache.openjpa.util.CacheMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests for the CacheMap.setSoftReferenceSize() method")
class CacheMapSetSoftReferenceSizeTest {

    private static Stream<Arguments> provideSizeArguments() {
        return Stream.of(
                // The structure of the arguments is:
                // Test Name, size, expectedSize
                Arguments.of("TC1: Set Unlimited Size", -1, -1),
                Arguments.of("TC2: Set Zero Size (Disabled)", 0, 0),
                Arguments.of("TC3: Set Minimum Positive Size", 1, 1),
                Arguments.of("TC4: Set Typical Size", 100, 100)
        );
    }

    @ParameterizedTest(name = "{index}: {0} with size={1}")
    @MethodSource("provideSizeArguments")
    void setSoftReferenceSizeShouldUpdateStateCorrectly(String testName, int size, int expectedSize) {
        CacheMap map = new CacheMap(false, 100);
        map.setSoftReferenceSize(size);
        assertEquals(expectedSize, map.getSoftReferenceSize(), "The soft reference size should be updated to the set value.");
    }

}
