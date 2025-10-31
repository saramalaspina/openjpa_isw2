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
import org.apache.openjpa.lib.util.LRUMap;
import org.apache.openjpa.lib.util.SizedMap;
import org.apache.openjpa.lib.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 test suite for the constructor and put method of the org.apache.openjpa.util.CacheMap class.
 */
class LLMCacheMapISWTest {

    @Nested
    @DisplayName("Constructor: public CacheMap(boolean lru, int max)")
    class ConstructorTests {

        @Test
        @DisplayName("lru=true should create an LRU-based cacheMap")
        void testConstructor_WithLruTrue_CreatesLRUMap() throws NoSuchFieldException, IllegalAccessException {
            // When a CacheMap is created with lru=true and a valid max size
            CacheMap map = new CacheMap(true, 100);

            // Then the internal cacheMap should be an instance of LRUMap
            assertTrue(map.isLRU(), "isLRU() should return true");
            Field cacheMapField = CacheMap.class.getDeclaredField("cacheMap");
            cacheMapField.setAccessible(true);
            Object internalCacheMap = cacheMapField.get(map);
            assertInstanceOf(LRUMap.class, internalCacheMap, "Internal cacheMap should be an LRUMap");
        }

        @Test
        @DisplayName("lru=false should create a concurrency-focused cacheMap")
        void testConstructor_WithLruFalse_CreatesConcurrentMap() throws NoSuchFieldException, IllegalAccessException {
            // When a CacheMap is created with lru=false
            CacheMap map = new CacheMap(false, 100);

            // Then the internal cacheMap should not be an instance of LRUMap
            assertFalse(map.isLRU(), "isLRU() should return false");
            Field cacheMapField = CacheMap.class.getDeclaredField("cacheMap");
            cacheMapField.setAccessible(true);
            Object internalCacheMap = cacheMapField.get(map);
            assertInstanceOf(ConcurrentHashMap.class, internalCacheMap, "Internal cacheMap should be a ConcurrentHashMap");
        }

        @ParameterizedTest(name = "max={0} should result in cacheSize={1}")
        @CsvSource({
                "100, 100",  // Positive case
                "0, 0",      // Boundary case: zero
                "-1, -1",    // Boundary case: negative value, special meaning
                "-10, -1"    // Boundary case: other negative value, normalized to -1
        })
        @DisplayName("Boundary analysis for 'max' parameter with non-LRU cache")
        void testConstructor_BoundaryValuesForMax_NonLru(int max, int expectedCacheSize) {
            // When a non-LRU CacheMap is created with a specific max size
            CacheMap map = new CacheMap(false, max);

            // Then the cache size reported by getCacheSize() should match the expected value
            assertEquals(expectedCacheSize, map.getCacheSize(),
                    "getCacheSize() should reflect the 'max' parameter logic");
        }

        @ParameterizedTest(name = "max={0} should result in cacheSize={1}")
        @CsvSource({
                "100, 100", // Positive case
                "2, 2",     // Boundary case: Smallest valid value for LRUMap
                "-2, -1"    // Boundary case: Negative value
        })
        @DisplayName("Boundary analysis for 'max' parameter with LRU cache")
        void testConstructor_BoundaryValuesForMax_Lru(int max, int expectedCacheSize) {
            // When an LRU CacheMap is created with a valid max size
            // Note: max values of -1, 0, and 1 cause an illegal size of 0 for the internal LRUMap constructor.
            CacheMap map = new CacheMap(true, max);

            // Then the cache size should be correctly reported
            assertEquals(expectedCacheSize, map.getCacheSize(),
                    "getCacheSize() should reflect the 'max' parameter logic for LRU maps");
        }


        @Test
        @DisplayName("Negative 'max' should set internal maxSize to Integer.MAX_VALUE")
        void testConstructor_NegativeMax_SetsInternalMaxSizeToMaxInt() throws NoSuchFieldException, IllegalAccessException {
            // When a CacheMap is created with a negative max (e.g., -2 to be LRU-compatible)
            CacheMap map = new CacheMap(true, -2);

            // Then the internal cacheMap's maxSize should be Integer.MAX_VALUE
            Field cacheMapField = CacheMap.class.getDeclaredField("cacheMap");
            cacheMapField.setAccessible(true);
            SizedMap internalCacheMap = (SizedMap) cacheMapField.get(map);
            assertEquals(Integer.MAX_VALUE, internalCacheMap.getMaxSize(),
                    "Internal maxSize should be Integer.MAX_VALUE for a negative input");
        }
    }

    @Nested
    @DisplayName("Method: public Object put(Object key, Object value)")
    class PutMethodTests {

        @ParameterizedTest(name = "lru={0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("put() with a new key should return null and increase size")
        void testPut_NewKey_ReturnsNullAndIncreasesSize(boolean lru) {
            // Given a CacheMap with a valid size
            CacheMap map = new CacheMap(lru, 10);
            String key = "key1";
            String value = "value1";

            // When put is called with a new key
            Object previousValue = map.put(key, value);

            // Then the returned value should be null and the map state should be updated
            assertNull(previousValue, "put for a new key should return null");
            assertEquals(1, map.size(), "Size should be 1 after adding one element");
            assertTrue(map.containsKey(key), "Map should contain the new key");
            assertEquals(value, map.get(key), "get should return the inserted value");
        }

        @ParameterizedTest(name = "lru={0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("put() with an existing key should return the old value and update")
        void testPut_ExistingKey_ReturnsOldValueAndUpdates(boolean lru) {
            // Given a CacheMap with an existing entry
            CacheMap map = new CacheMap(lru, 10);
            String key = "key1";
            String oldValue = "oldValue";
            String newValue = "newValue";
            map.put(key, oldValue);

            // When put is called for the same key with a new value
            Object previousValue = map.put(key, newValue);

            // Then the old value should be returned and the map state updated
            assertEquals(oldValue, previousValue, "put for an existing key should return the old value");
            assertEquals(1, map.size(), "Size should remain 1 after updating an element");
            assertEquals(newValue, map.get(key), "get should return the new value");
        }

        @Test
        @DisplayName("put() on a zero-size non-LRU cache should return null and not insert")
        void testPut_OnZeroSizeCache_ReturnsNullAndDoesNotInsert() {
            // Given a non-LRU CacheMap, created with a valid size
            CacheMap map = new CacheMap(false, 10);
            // When the cache size is then set to 0
            map.setCacheSize(0);

            // When put is called
            Object previousValue = map.put("key1", "value1");

            // Then the method should return null and the map should remain empty
            assertNull(previousValue, "put on a zero-size cache should return null");
            assertEquals(0, map.size(), "Size should be 0");
            assertFalse(map.containsKey("key1"), "Map should not contain the key");
        }


        @ParameterizedTest(name = "lru={0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("put() on a pinned key should update the pinned entry")
        void testPut_PinnedKeyWithValue_ReturnsOldValueAndUpdate(boolean lru) {
            // Given a CacheMap with a pinned key
            CacheMap map = new CacheMap(lru, 10);
            String key = "pinnedKey";
            String oldValue = "oldValue";
            String newValue = "newValue";
            map.put(key, oldValue);
            map.pin(key);

            // When put is called on the pinned key
            Object previousValue = map.put(key, newValue);

            // Then the old value is returned and the pinned value is updated
            assertEquals(oldValue, previousValue, "put on a pinned key should return its old value");
            assertEquals(1, map.size(), "Size should not change");
            assertTrue(map.getPinnedKeys().contains(key), "Key should remain pinned");
            assertEquals(newValue, map.get(key), "get should return the new value for the pinned key");
        }

        @ParameterizedTest(name = "lru={0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("put() on a pinned key with a null value should increment pinned size")
        void testPut_PinnedKeyWithoutValue_ReturnsNullAndUpdates(boolean lru) {
            // Given a CacheMap with a pinned key that has no associated value
            CacheMap map = new CacheMap(lru, 10);
            String key = "pinnedKey";
            map.pin(key); // Pin the key before it has a value
            assertNull(map.get(key));
            assertEquals(0, map.size(), "Size should be 0 for a pinned key with null value");

            // When a value is put for that pinned key
            String newValue = "newValue";
            Object previousValue = map.put(key, newValue);

            // Then the previous value is null and the size increases
            assertNull(previousValue, "Previous value for a newly-valued pinned key should be null");
            assertEquals(1, map.size(), "Size should become 1");
            assertTrue(map.getPinnedKeys().contains(key), "Key should remain pinned");
            assertEquals(newValue, map.get(key), "get should return the new value");
        }

        @Test
        @DisplayName("put() causing overflow should move entry to softMap, then promote it back")
        void testPut_PromoteFromSoftMap_ReturnsOldValueAndMovesToCacheMap() {
            // Given a CacheMap with max size 2 to easily trigger overflow
            CacheMap map = new CacheMap(true, 2); // LRU makes overflow predictable

            // When we put three items, the first should overflow to softMap
            map.put("key1", "value1"); // -> cacheMap
            map.put("key2", "value2"); // -> cacheMap
            map.put("key3", "value3"); // -> cacheMap, "key1" overflows to softMap
            assertEquals(3, map.size(), "Map should contain all three items");

            // When we put the overflowed key again
            Object previousValue = map.put("key1", "newValue1");

            // Then the old value from the softMap is returned, and the entry is promoted back to the cacheMap
            assertEquals("value1", previousValue, "Should return the old value that was in softMap");
            assertEquals(3, map.size(), "Size should remain 3");
            assertEquals("newValue1", map.get("key1"), "The value should be updated");
        }

        @ParameterizedTest(name = "lru={0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("put() with null key/value should be allowed")
        void testPut_NullsAllowedOnAllCacheTypes(boolean lru) {
            // Given an LRU or non-LRU CacheMap
            CacheMap map = new CacheMap(lru, 10);

            // Then putting a null key or null value should not throw an exception
            assertDoesNotThrow(() -> map.put(null, "value"), "Null key should be allowed");
            assertDoesNotThrow(() -> map.put("key", null), "Null value should be allowed");

            // And the entries should be retrievable
            assertEquals(2, map.size());
            assertEquals("value", map.get(null));
            assertNull(map.get("key"));
        }
    }
}