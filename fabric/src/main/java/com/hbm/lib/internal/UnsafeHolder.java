// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal;

import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;

public final class UnsafeHolder {
    public static final Unsafe U = UnsafeBootstrap.U;

    public static final long IA_BASE = U.arrayBaseOffset(int[].class);
    public static final int IA_SHIFT = arrayShift(int[].class);
    public static final long JA_BASE = U.arrayBaseOffset(long[].class);
    public static final int JA_SHIFT = arrayShift(long[].class);
    public static final long BA_BASE = U.arrayBaseOffset(byte[].class);
    public static final int BA_SHIFT = arrayShift(byte[].class);
    public static final long ZA_BASE = U.arrayBaseOffset(boolean[].class);
    public static final int ZA_SHIFT = arrayShift(boolean[].class);
    public static final long SA_BASE = U.arrayBaseOffset(short[].class);
    public static final int SA_SHIFT = arrayShift(short[].class);
    public static final long CA_BASE = U.arrayBaseOffset(char[].class);
    public static final int CA_SHIFT = arrayShift(char[].class);
    public static final long FA_BASE = U.arrayBaseOffset(float[].class);
    public static final int FA_SHIFT = arrayShift(float[].class);
    public static final long DA_BASE = U.arrayBaseOffset(double[].class);
    public static final int DA_SHIFT = arrayShift(double[].class);
    public static final long RA_BASE = U.arrayBaseOffset(Object[].class);
    public static final int RA_SHIFT = arrayShift(Object[].class);

    private UnsafeHolder() {}

    public static long offInt(int i) {
        return ((long) i << IA_SHIFT) + IA_BASE;
    }

    public static long offLong(int i) {
        return ((long) i << JA_SHIFT) + JA_BASE;
    }

    public static long offByte(int i) {
        return ((long) i << BA_SHIFT) + BA_BASE;
    }

    public static long offBoolean(int i) {
        return ((long) i << ZA_SHIFT) + ZA_BASE;
    }

    public static long offShort(int i) {
        return ((long) i << SA_SHIFT) + SA_BASE;
    }

    public static long offChar(int i) {
        return ((long) i << CA_SHIFT) + CA_BASE;
    }

    public static long offFloat(int i) {
        return ((long) i << FA_SHIFT) + FA_BASE;
    }

    public static long offDouble(int i) {
        return ((long) i << DA_SHIFT) + DA_BASE;
    }

    public static long offReference(int i) {
        return ((long) i << RA_SHIFT) + RA_BASE;
    }

    public static Object staticFieldBase(Class<?> clz, String fieldName) {
        return U.staticFieldBase(declaredField(clz, fieldName));
    }

    public static long staticFieldOffset(Class<?> clz, String fieldName) {
        return U.staticFieldOffset(declaredField(clz, fieldName));
    }

    public static long fieldOffset(Class<?> clz, String fieldName) {
        return U.objectFieldOffset(clz, fieldName);
    }

    @SuppressWarnings("unchecked")
    public static <T> T allocateInstance(Class<? extends T> clz) {
        try {
            return (T) U.allocateInstance(clz);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private static int arrayShift(Class<?> arrayClass) {
        int scale = U.arrayIndexScale(arrayClass);
        if (scale <= 0 || (scale & (scale - 1)) != 0) {
            throw new ExceptionInInitializerError(
                    "Unsupported array index scale " + scale + " for " + arrayClass.getTypeName());
        }
        return Integer.numberOfTrailingZeros(scale);
    }

    private static Field declaredField(Class<?> clz, String fieldName) {
        try {
            return clz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
