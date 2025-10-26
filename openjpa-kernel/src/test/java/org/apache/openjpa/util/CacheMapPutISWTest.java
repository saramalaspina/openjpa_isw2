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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("Tests for the CacheMap.put() method")
class CacheMapPutISWTest {

    private static class InspectableCacheMap extends CacheMap {

        // Counters to register the number of calls to entryAdded and entryRemoved methods
        public int addedCounter = 0;
        public int removedCounter = 0;

        public InspectableCacheMap(boolean lru, int max) {
            super(lru, max);
        }

        @Override
        protected void entryAdded(Object key, Object value) {
            super.entryAdded(key, value);
            addedCounter++;
        }

        @Override
        protected void entryRemoved(Object key, Object value, boolean expired) {
            super.entryRemoved(key, value, expired);
            removedCounter++;
        }

        public void resetCounters(){
            addedCounter = 0;
            removedCounter = 0;
        }

    }

    @Nested
    @DisplayName("Robustness and Edge Cases")
    class RobustnessTests {

        @Test
        @DisplayName("put() with null key should not throw NullPointerException")
        void putWithNullKeyShouldNotThrowNPE() {
            InspectableCacheMap map = new InspectableCacheMap(true, 100);
            map.put(null, 10);

            assertEquals(1, map.size(), "Map size should be 1 after adding a null key.");
            assertTrue(map.containsKey(null), "Map must report that it contains the null key.");
            assertEquals(10, map.get(null), "The value associated with the null key must be correct.");
            assertEquals(1, map.addedCounter, "entryAdded should be called once for a new null key.");
            assertEquals(0, map.removedCounter, "entryRemoved should not be called for a new null key.");
        }

        @Test
        @DisplayName("put() with null value should not throw NullPointerException")
        void putWithNullValueShouldNotThrowNPE() {
            InspectableCacheMap map = new InspectableCacheMap(true, 100);
            map.put("new_key", null);

            assertEquals(1, map.size(), "Map size should be 1 after adding a null key.");
            assertNull(map.get("new_key"), "The value associated with the key must be null.");
            assertEquals(1, map.addedCounter, "entryAdded should be called once for a new entry with null value.");
            assertEquals(0, map.removedCounter, "entryRemoved should not be called for a new entry.");
        }

        @Test
        @DisplayName("put() on a disabled cache (maxSize=0) should do nothing")
        void putOnDisabledCacheShouldDoNothing() {
            InspectableCacheMap map = new InspectableCacheMap(false, 0);
            Object result = map.put("new_key", 10);

            assertNull(result, "The return value should be null as the cache is disabled.");
            assertTrue(map.isEmpty(), "The element should not be inserted in a disabled cache.");
            assertEquals(0, map.addedCounter, "entryAdded should not be called on a disabled cache.");
            assertEquals(0, map.removedCounter, "entryRemoved should not be called on a disabled cache.");
        }
    }

    @Nested
    @DisplayName("Basic Operations (No Overflow)")
    class BasicOperationTests {

        @Test
        @DisplayName("put() on an empty map should insert the element")
        void putOnEmptyMapShouldInsertElement() {
            InspectableCacheMap map = new InspectableCacheMap(false, 100);
            Object oldValue = map.put("new_key", "isw");

            assertNull(oldValue, "Putting a new key should return null.");
            assertEquals(1, map.size());
            assertEquals("isw", map.get("new_key"));
            assertEquals(1, map.addedCounter, "entryAdded should be called once for a new entry.");
            assertEquals(0, map.removedCounter, "entryRemoved should not be called for a new entry.");
        }

        @Test
        @DisplayName("put() on an existing key in cacheMap should update the value")
        void putOnExistingKeyInCacheMapShouldUpdateValue() {
            InspectableCacheMap map = new InspectableCacheMap(true, 2);
            map.put("key_in_cacheMap", "old_value");
            map.resetCounters(); // Reset after setup

            Object oldValue = map.put("key_in_cacheMap", 10);

            assertEquals("old_value", oldValue, "The old value should be returned.");
            assertEquals(1, map.size(), "The map size should not change.");
            assertEquals(10, map.get("key_in_cacheMap"), "The value should be updated.");
            assertEquals(1, map.addedCounter, "entryAdded should be called once for the new value.");
            assertEquals(1, map.removedCounter, "entryRemoved should be called once for the old value.");
        }
    }

    @Nested
    @DisplayName("Overflow Scenarios")
    class OverflowTests {

        @Test
        @DisplayName("put() on a full LRU map should evict to an empty softMap")
        void putOnFullLruMapShouldEvictToSoftMap() {
            InspectableCacheMap map = new InspectableCacheMap(true, 2);
            map.put("K1", "V1"); // K1 is the least recently used
            map.put("K2", "V2");
            map.resetCounters(); // Reset after setup

            map.put("new_key", 10); // This should evict K1 because of the LRU policy

            assertEquals(3, map.size(), "Total size should be 3 (2 in cacheMap, 1 in softMap).");
            assertTrue(map.cacheMap.containsKey("K2") && map.cacheMap.containsKey("new_key"));
            assertTrue(map.softMap.containsKey("K1"));
            assertEquals(1, map.addedCounter, "entryAdded should be called once for the new entries.");
            assertEquals(0, map.removedCounter, "entryRemoved should not be called on a overflow.");
        }

        @Test
        @DisplayName("put() on a full map with a full softMap should cause cascade eviction")
        void putOnFullMapWithFullSoftMapShouldCauseCascadeEviction() {
            InspectableCacheMap map = new InspectableCacheMap(false, 2); // non-LRU map
            map.setSoftReferenceSize(2); // Set a limit to the softMap

            // Fill both caches completely
            map.put("K1", "V1");
            map.put("K2", "V2");
            map.put("K3", "V3"); // Evicts K1 or K2 to softMap
            map.put("K4", "V4"); // Evicts the other to softMap. softMap is now full

            map.resetCounters(); // Reset after setup

            map.put("new_key", "isw"); // This will cause a cascade eviction

            assertEquals(4, map.size(), "Total size should not exceed the combined max sizes (2+2).");
            assertTrue(map.containsKey("new_key"), "The new element must be in the map.");
            // We can't know which element was definitively removed because of the non-LRU policy, but we know one was.
            // A simple check is that the total count of original keys is now 3 instead of 4
            long originalKeysPresent = Stream.of("K1", "K2", "K3", "K4").filter(map::containsKey).count();
            assertEquals(3, originalKeysPresent, "One of the original 4 keys must have been definitively evicted.");
            assertEquals(1, map.addedCounter, "entryAdded should be called for the new entry.");
            assertEquals(1, map.removedCounter, "entryRemoved should be called once due to cascade eviction.");
        }
    }

    @Nested
    @DisplayName("State Interaction Scenarios")
    class StateInteractionTests {

        @Test
        @DisplayName("put() on an existing key in softMap should promote it to cacheMap")
        void putOnExistingKeyInSoftMapShouldPromoteIt() {
            InspectableCacheMap map = new InspectableCacheMap(true, 2);
            map.put("key_in_softMap", "old_value");
            map.put("K1", "value1");
            map.put("K2", "value2"); // Evicts "key_in_softMap" (LRU) to softMap

            map.resetCounters(); // Reset after setup

            Object oldValue = map.put("key_in_softMap", "isw"); // "key_in_softMap" returns to cacheMap and evicts "K1" (LRU) to softMap

            assertEquals("old_value", oldValue, "The old value should be returned.");
            assertEquals("isw", map.get("key_in_softMap"), "The value should be updated.");
            assertFalse(map.softMap.containsKey("key_in_softMap"));
            assertTrue(map.cacheMap.containsKey("key_in_softMap"));
            assertEquals(3, map.size(), "Total size should remain 3.");
            assertEquals(1, map.addedCounter, "entryAdded should be called for the promoted value.");
            assertEquals(1, map.removedCounter, "entryRemoved should be called for the old value from softMap.");
        }

        @Test
        @DisplayName("put() on a pinned key should update its value and keep it pinned, when old value added first in cacheMap")
        void putOnPinnedKeyShouldUpdateValue() {
            InspectableCacheMap map = new InspectableCacheMap(false, 10);
            map.put("key_in_pinnedMap", "old_value");
            map.pin("key_in_pinnedMap");

            map.resetCounters(); // Reset after setup

            Object oldValue = map.put("key_in_pinnedMap", 10);

            assertEquals("old_value", oldValue, "The old value should be returned.");
            assertEquals(1, map.size(), "Map size should not change.");
            assertEquals(10, map.get("key_in_pinnedMap"), "The value should be updated.");
            assertFalse(map.cacheMap.containsKey("key_in_pinnedMap"));
            assertTrue(map.getPinnedKeys().contains("key_in_pinnedMap"), "The key should remain in the pinned set.");
            assertEquals(1, map.addedCounter, "entryAdded should be called for the updated pinned value.");
            assertEquals(1, map.removedCounter, "entryRemoved should be called for the old pinned value.");
        }

        // Test added after first Jacoco report
        @Test
        @DisplayName("put() on a pinned key with no value should add the value directly in pinnedMap")
        void putOnPinnedKeyWithNoValue() {
            InspectableCacheMap map = new InspectableCacheMap(false, 10);
            map.pin("key_in_pinnedMap");
            Object oldValue = map.put("key_in_pinnedMap", 10);

            assertNull(oldValue, "Old value should be null");
            assertEquals(1, map.size(), "Map size should be 1 after adding value to pinned key.");
            assertEquals(10, map.get("key_in_pinnedMap"), "The value should be correct.");
            assertFalse(map.cacheMap.containsKey("key_in_pinnedMap"));
            assertTrue(map.getPinnedKeys().contains("key_in_pinnedMap"), "The key should remain in the pinned set.");
            assertEquals(1, map.addedCounter, "entryAdded should be called once when value is first set for a pinned key.");
            assertEquals(0, map.removedCounter, "entryRemoved should not be called when a pinned key gets its first value.");
        }
    }
}