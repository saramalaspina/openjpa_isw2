/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.util;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ClassUtilToClassTest {

    @Test
    public void testToClass() {
        Assert.assertEquals(this.getClass(), toClass("org.apache.openjpa.lib.util.ClassUtilTest"));

        Assert.assertEquals(new ClassUtilToClassTest[0].getClass(), toClass("org.apache.openjpa.lib.util.ClassUtilTest[]"));

        Assert.assertEquals(Integer.class, toClass("java.lang.Integer"));

        Assert.assertEquals(byte.class, toClass("byte"));
        Assert.assertEquals(char.class, toClass("char"));
        Assert.assertEquals(double.class, toClass("double"));
        Assert.assertEquals(float.class, toClass("float"));
        Assert.assertEquals(int.class, toClass("int"));
        Assert.assertEquals(long.class, toClass("long"));
        Assert.assertEquals(short.class, toClass("short"));
        Assert.assertEquals(boolean.class, toClass("boolean"));
        Assert.assertEquals(void.class, toClass("void"));

        Assert.assertEquals(new float[0].getClass(), toClass("float[]"));
        Assert.assertEquals(new float[0][0].getClass(), toClass("float[][]"));
        Assert.assertEquals(new long[0][0][0].getClass(), toClass("long[][][]"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonExistingClass() {
        toClass("does.not.Exist");
    }

    private Class toClass(String clazz) {
        return ClassUtil.toClass(clazz, false, this.getClass().getClassLoader());
    }

    @Test
    public void testGetClassName() {
        Assert.assertEquals("ClassUtilTest", ClassUtil.getClassName(ClassUtilToClassTest.class));
        Assert.assertEquals("ClassUtilTest$MyInnerClass", ClassUtil.getClassName(MyInnerClass.class));

        // anonymous class
        Assert.assertEquals("ClassUtilTest$1", ClassUtil.getClassName(INSTANCE.getClass()));

        // primitives
        Assert.assertEquals("byte", ClassUtil.getClassName(byte.class));
        Assert.assertEquals("char", ClassUtil.getClassName(char.class));
        Assert.assertEquals("double", ClassUtil.getClassName(double.class));
        Assert.assertEquals("float", ClassUtil.getClassName(float.class));
        Assert.assertEquals("int", ClassUtil.getClassName(int.class));
        Assert.assertEquals("long", ClassUtil.getClassName(long.class));
        Assert.assertEquals("short", ClassUtil.getClassName(short.class));
        Assert.assertEquals("boolean", ClassUtil.getClassName(boolean.class));
        Assert.assertEquals("void", ClassUtil.getClassName(void.class));

        // arrays
        Assert.assertEquals("long[]", ClassUtil.getClassName(long[].class));
        Assert.assertEquals("long[][]", ClassUtil.getClassName(long[][].class));
        Assert.assertEquals("float[][][]", ClassUtil.getClassName(float[][][].class));

        Assert.assertEquals("ClassUtilTest[]", ClassUtil.getClassName(ClassUtilToClassTest[].class));
        Assert.assertEquals("ClassUtilTest$MyInnerClass[]", ClassUtil.getClassName(MyInnerClass[].class));
        Assert.assertEquals("ClassUtilTest$MyInnerClass[][]", ClassUtil.getClassName(MyInnerClass[][].class));
    }

    @Test
    @Ignore("only needed for manual performance tests")
    public void testGetClassNamePerformance() {

        long start = System.nanoTime();
        for (int i = 1; i < 10000000; i++) {
            //X String className = Strings.getClassName(MyInnerClass.class);
            ClassUtil.getClassName(MyInnerClass.class);
        }

        long stop = System.nanoTime();
        System.out.println("took: " + TimeUnit.NANOSECONDS.toMillis(stop - start));
    }

    @Test
    public void testGetPackageName() {
        Assert.assertEquals("org.apache.openjpa.lib.util", ClassUtil.getPackageName(ClassUtilToClassTest.class));
        Assert.assertEquals("org.apache.openjpa.lib.util", ClassUtil.getPackageName(MyInnerClass.class));
        Assert.assertEquals("org.apache.openjpa.lib.util", ClassUtil.getPackageName(MyInnerClass[].class));
        Assert.assertEquals("org.apache.openjpa.lib.util", ClassUtil.getPackageName(INSTANCE.getClass()));
        Assert.assertEquals("", ClassUtil.getPackageName(long.class));
        Assert.assertEquals("", ClassUtil.getPackageName(long[].class));
    }

    private static abstract class MyInnerClass {
        // not needed
    }

    private static final MyInnerClass INSTANCE = new MyInnerClass() {
    };

}
