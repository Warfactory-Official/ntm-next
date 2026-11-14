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

package com.hbm.packet.codec;

import io.netty.handler.codec.DecoderException;
import io.netty.util.concurrent.FastThreadLocal;
import java.nio.charset.StandardCharsets;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class PacketValues {
    private static final FastThreadLocal<byte[]> TEXT_BYTES =
            new FastThreadLocal<>() {
                @Override
                protected byte[] initialValue() {
                    return new byte[0];
                }
            };

    private PacketValues() {}

    public static int varIntSize(int value) {
        return (31 - Integer.numberOfLeadingZeros(value | 1)) / 7 + 1;
    }

    public static int varLongSize(long value) {
        return (63 - Long.numberOfLeadingZeros(value | 1)) / 7 + 1;
    }

    public static long putVarInt(Object storageBase, long address, int value) {
        while ((value & ~0x7F) != 0) {
            U.putByte(storageBase, address++, (byte) (value | 0x80));
            value >>>= 7;
        }
        U.putByte(storageBase, address, (byte) value);
        return address + 1;
    }

    public static long putVarLong(Object storageBase, long address, long value) {
        while ((value & ~0x7FL) != 0) {
            U.putByte(storageBase, address++, (byte) (value | 0x80));
            value >>>= 7;
        }
        U.putByte(storageBase, address, (byte) value);
        return address + 1;
    }

    public static int getVarInt(Object storageBase, long address, long limit) {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            if (address >= limit) throw new DecoderException("Truncated variable integer");
            int next = U.getByte(storageBase, address++);
            value |= (next & 0x7F) << shift;
            if (next >= 0) {
                if (next == 0 && shift != 0 || shift == 28 && next > 15)
                    throw new DecoderException("Invalid variable integer");
                return value;
            }
        }
        throw new DecoderException("Variable integer overflow");
    }

    public static long getVarLong(Object storageBase, long address, long limit) {
        long value = 0;
        for (int shift = 0; shift < 70; shift += 7) {
            if (address >= limit) throw new DecoderException("Truncated variable long");
            int next = U.getByte(storageBase, address++);
            value |= (long) (next & 0x7F) << shift;
            if (next >= 0) {
                if (next == 0 && shift != 0 || shift == 63 && next > 1)
                    throw new DecoderException("Invalid variable long");
                return value;
            }
        }
        throw new DecoderException("Variable long overflow");
    }

    public static int utf8Length(String value, int maxBytes, int maxChars) {
        if (value.length() > Math.min(maxChars, maxBytes))
            throw new IllegalArgumentException("Text exceeds limit");
        long bytes = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current < 0x80) {
                bytes++;
            } else if (current < 0x800) {
                bytes += 2;
            } else if (Character.isHighSurrogate(current)) {
                if (++index == value.length() || !Character.isLowSurrogate(value.charAt(index))) {
                    throw new IllegalArgumentException("Unpaired text surrogate");
                }
                bytes += 4;
            } else if (Character.isLowSurrogate(current)) {
                throw new IllegalArgumentException("Unpaired text surrogate");
            } else {
                bytes += 3;
            }
        }
        if (bytes > maxBytes) throw new IllegalArgumentException("Text exceeds byte limit");
        return (int) bytes;
    }

    public static long putUtf8(Object storageBase, long address, String value) {
        int byteLength = value.length();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current < 0x80) continue;
            if (current < 0x800) byteLength++;
            else {
                byteLength += 2;
                if (Character.isHighSurrogate(current)) index++;
            }
        }
        return putUtf8(storageBase, address, value, byteLength);
    }

    public static long putUtf8(Object storageBase, long address, String value, int byteLength) {
        address = putVarInt(storageBase, address, byteLength);
        for (int index = 0; index < value.length(); index++) {
            int codePoint = value.charAt(index);
            if (codePoint < 0x80) {
                U.putByte(storageBase, address++, (byte) codePoint);
            } else if (codePoint < 0x800) {
                U.putShort(
                        storageBase,
                        address,
                        (short) ((0xC0 | codePoint >>> 6) | (0x80 | codePoint & 0x3F) << 8));
                address += 2;
            } else if (!Character.isHighSurrogate((char) codePoint)) {
                U.putShort(
                        storageBase,
                        address,
                        (short) ((0xE0 | codePoint >>> 12) | (0x80 | codePoint >>> 6 & 0x3F) << 8));
                U.putByte(storageBase, address + 2, (byte) (0x80 | codePoint & 0x3F));
                address += 3;
            } else {
                codePoint = Character.toCodePoint((char) codePoint, value.charAt(++index));
                U.putInt(
                        storageBase,
                        address,
                        (0xF0 | codePoint >>> 18)
                                | (0x80 | codePoint >>> 12 & 0x3F) << 8
                                | (0x80 | codePoint >>> 6 & 0x3F) << 16
                                | (0x80 | codePoint & 0x3F) << 24);
                address += 4;
            }
        }
        return address;
    }

    public static String getUtf8(Object storageBase, long address, int length, int maxChars) {
        int utf16Length = 0;
        for (long at = address, end = address + length; at < end; ) {
            int first = U.getByte(storageBase, at++) & 255;
            if (first < 0x80) {
                utf16Length++;
                continue;
            }
            int following;
            int codePoint;
            int minimum;
            if (first >= 0xC2 && first <= 0xDF) {
                following = 1;
                codePoint = first & 0x1F;
                minimum = 0x80;
            } else if (first >= 0xE0 && first <= 0xEF) {
                following = 2;
                codePoint = first & 0x0F;
                minimum = 0x800;
            } else if (first >= 0xF0 && first <= 0xF4) {
                following = 3;
                codePoint = first & 0x07;
                minimum = 0x10000;
            } else {
                throw new DecoderException("Invalid UTF-8 lead byte");
            }
            if (following > end - at) throw new DecoderException("Truncated UTF-8 sequence");
            for (int part = 0; part < following; part++) {
                int next = U.getByte(storageBase, at++) & 255;
                if ((next & 0xC0) != 0x80) throw new DecoderException("Invalid UTF-8 continuation");
                codePoint = codePoint << 6 | next & 0x3F;
            }
            if (codePoint < minimum
                    || codePoint > 0x10FFFF
                    || codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                throw new DecoderException("Invalid UTF-8 code point");
            }
            utf16Length += codePoint >= 0x10000 ? 2 : 1;
        }
        if (utf16Length > maxChars) throw new DecoderException("Text exceeds character limit");
        if (storageBase instanceof byte[] bytes) {
            return new String(bytes, (int) (address - BA_BASE), length, StandardCharsets.UTF_8);
        }
        var bytes = TEXT_BYTES.get();
        if (bytes.length < length)
            TEXT_BYTES.set(bytes = new byte[Math.max(length, bytes.length << 1)]);
        U.copyMemory(null, address, bytes, BA_BASE, length);
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }
}
