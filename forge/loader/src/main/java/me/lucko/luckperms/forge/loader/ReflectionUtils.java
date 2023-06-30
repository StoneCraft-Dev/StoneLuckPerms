/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.forge.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtils {

    public static Field getField(final Class<?> clazz, final String fieldName)
            throws NoSuchFieldException {
        try {
            dumpFields(clazz);

            return clazz.getDeclaredField(fieldName);
        } catch (final NoSuchFieldException e) {
            final Class<?> superclass = clazz.getSuperclass();

            if (superclass != null) {
                return getField(superclass, fieldName);
            }

            throw e;
        }
    }

    public static Object getNecessaryFieldValue(final Object instance, final String fieldName) {
        try {
            return getFieldValue(instance, fieldName);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Error getting necessary field '%s': %s", fieldName,
                            e.getClass().getName()));
        }
    }

    public static Object getFieldValue(final Object instance, final String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        return getFieldValue(instance.getClass(), instance, fieldName);
    }

    public static Object getFieldValue(final Class<?> clazz, final Object instance,
            final String fieldName) throws NoSuchFieldException, IllegalAccessException {
        final Field field = getField(clazz, fieldName);

        field.setAccessible(true);

        return field.get(instance);
    }

    public static void setNecessaryFieldValue(final Object instance, final String fieldName,
            final Object value) {
        try {
            setFieldValue(instance, fieldName, value);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Error setting necessary field '%s': %s", fieldName,
                            e.getClass().getName()));
        }
    }

    public static void setFieldValue(final Object instance, final String fieldName,
            final Object value) throws NoSuchFieldException, IllegalAccessException {
        final Field field = getField(instance.getClass(), fieldName);

        field.setAccessible(true);
        field.set(instance, value);
    }

    public static Object invokeNecessaryMethod(final Object instance, final String methodName) {
        try {
            return invokeMethod(instance, methodName);
        } catch (final NoSuchMethodException | InvocationTargetException |
                       IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Error invoking necessary method '%s': %s", methodName,
                            e.getClass().getName()));
        }
    }

    public static Object invokeMethod(final Object instance, final String methodName)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        dumpMethods(instance.getClass());

        final Method method = instance.getClass().getMethod(methodName);

        return method.invoke(instance);
    }

    // TODO: Remove usages
    public static void dumpFields(final Class<?> clazz) {
        final File file1 = new File(clazz.getSimpleName() + "-field-dump.txt");

        if (!file1.exists()) {
            try (final PrintWriter writer = new PrintWriter(file1, "UTF-8")) {
                writer.println("Path: " + clazz.getProtectionDomain().getCodeSource().getLocation()
                        .getPath());

                final Field[] fields = clazz.getDeclaredFields();

                for (final Field field : fields) {
                    writer.println(field);
                }
            } catch (final FileNotFoundException | UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void dumpMethods(final Class<?> clazz) {
        final File file = new File(clazz.getSimpleName() + "-method-dump.txt");

        if (!file.exists()) {
            try (final PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.println("Path: " + clazz.getProtectionDomain().getCodeSource().getLocation()
                        .getPath());

                final Method[] methods = clazz.getDeclaredMethods();

                for (final Method method : methods) {
                    writer.println(method);
                }
            } catch (final FileNotFoundException | UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
