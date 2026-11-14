/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.packet.compat;

import com.hbm.lib.internal.UnsafeHolder;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;

public final class ZstdAccess {
    private static final ClassValue<ZstdAccess> BINDINGS =
            new ClassValue<>() {
                @Override
                protected ZstdAccess computeValue(Class<?> type) {
                    return new ZstdAccess(type);
                }
            };
    private final MethodHandle compress, decompress, oneShot, bound;
    public final Object continueDirective, flushDirective, endDirective;

    private ZstdAccess(Class<?> compressor) {
        try {
            ClassLoader loader = compressor.getClassLoader();
            Class<?> directive = Class.forName("com.github.luben.zstd.EndDirective", true, loader);
            Class<?> decoder =
                    Class.forName("com.github.luben.zstd.ZstdDecompressCtx", true, loader);
            var lookup = UnsafeHolder.IMPL_LOOKUP;
            compress =
                    lookup.unreflect(
                                    compressor.getMethod(
                                            "compressDirectByteBufferStream",
                                            ByteBuffer.class,
                                            ByteBuffer.class,
                                            directive))
                            .asType(
                                    MethodType.methodType(
                                            boolean.class,
                                            Object.class,
                                            ByteBuffer.class,
                                            ByteBuffer.class,
                                            Object.class));
            decompress =
                    lookup.unreflect(
                                    decoder.getMethod(
                                            "decompressDirectByteBufferStream",
                                            ByteBuffer.class,
                                            ByteBuffer.class))
                            .asType(
                                    MethodType.methodType(
                                            boolean.class,
                                            Object.class,
                                            ByteBuffer.class,
                                            ByteBuffer.class));
            oneShot =
                    lookup.unreflect(
                                    compressor.getMethod(
                                            "compress", ByteBuffer.class, ByteBuffer.class))
                            .asType(
                                    MethodType.methodType(
                                            int.class,
                                            Object.class,
                                            ByteBuffer.class,
                                            ByteBuffer.class));
            bound =
                    lookup.unreflect(
                            Class.forName("com.github.luben.zstd.Zstd", true, loader)
                                    .getMethod("compressBound", long.class));
            continueDirective = directive.getField("CONTINUE").get(null);
            flushDirective = directive.getField("FLUSH").get(null);
            endDirective = directive.getField("END").get(null);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static ZstdAccess of(Object compressor) {
        return BINDINGS.get(compressor.getClass());
    }

    public boolean compress(Object context, ByteBuffer out, ByteBuffer in, Object directive) {
        try {
            return (boolean) compress.invokeExact(context, out, in, directive);
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }

    public boolean decompress(Object context, ByteBuffer out, ByteBuffer in) {
        try {
            return (boolean) decompress.invokeExact(context, out, in);
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }

    public int oneShot(Object context, ByteBuffer out, ByteBuffer in) {
        try {
            return (int) oneShot.invokeExact(context, out, in);
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }

    public int bound(int size) {
        try {
            return Math.toIntExact((long) bound.invokeExact((long) size));
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }

    private static RuntimeException propagate(Throwable failure) {
        if (failure instanceof RuntimeException runtime) return runtime;
        if (failure instanceof Error error) throw error;
        return new IllegalStateException(failure);
    }
}
