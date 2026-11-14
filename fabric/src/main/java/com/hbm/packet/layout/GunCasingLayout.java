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

package com.hbm.packet.layout;

import com.hbm.client.effect.DetailEffects;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;

public final class GunCasingLayout {
    public static final int VARIANT = 25;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 83;
    public static final int MAX_BODY_BYTES = 1048576;

    private GunCasingLayout() {}

    public static EncodedPayload encode(
            double x,
            double y,
            double z,
            double mX,
            double mY,
            double mZ,
            float yaw,
            float pitch,
            float mPitch,
            float mYaw,
            String casing,
            boolean smoking,
            int smokeLife,
            double smokeLift,
            int nodeLife,
            boolean legacy) {
        int casingBytes = PacketValues.utf8Length(casing, 2097151, 2097151);
        long bodyBytesWide = 82L + (PacketValues.varIntSize(casingBytes) + casingBytes);
        if (bodyBytesWide > 1048576) throw new EncoderException("Packet exceeds frame limit");
        int bodyBytes = (int) bodyBytesWide;
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyEffects.TYPE, bodyBytes, MAX_BODY_BYTES, true, false, false);
        try {
            ByteBuf encodedBuffer = payload.buffer();
            int bufferStart = encodedBuffer.writerIndex();
            Object storageBase;
            long storageAddress;
            if (encodedBuffer.hasArray()) {
                storageBase = encodedBuffer.array();
                storageAddress = BA_BASE + encodedBuffer.arrayOffset() + bufferStart;
            } else {
                storageBase = null;
                storageAddress = encodedBuffer.memoryAddress() + bufferStart;
            }
            {
                long xBits = doubleToRawLongBits(x);
                long yBits = doubleToRawLongBits(y);
                long zBits = doubleToRawLongBits(z);
                long mXBits = doubleToRawLongBits(mX);
                long mYBits = doubleToRawLongBits(mY);
                long mZBits = doubleToRawLongBits(mZ);
                long yawBits = floatToRawIntBits(yaw);
                long pitchBits = floatToRawIntBits(pitch);
                long mPitchBits = floatToRawIntBits(mPitch);
                long mYawBits = floatToRawIntBits(mYaw);
                U.putLong(storageBase, storageAddress, 25L | (xBits << 5));
                U.putLong(storageBase, storageAddress + 8L, (xBits >>> 59) | (yBits << 5));
                U.putLong(storageBase, storageAddress + 16L, (yBits >>> 59) | (zBits << 5));
                U.putLong(storageBase, storageAddress + 24L, (zBits >>> 59) | (mXBits << 5));
                U.putLong(storageBase, storageAddress + 32L, (mXBits >>> 59) | (mYBits << 5));
                U.putLong(storageBase, storageAddress + 40L, (mYBits >>> 59) | (mZBits << 5));
                U.putLong(
                        storageBase,
                        storageAddress + 48L,
                        (mZBits >>> 59) | ((yawBits & 0xFFFFFFFFL) << 5) | (pitchBits << 37));
                U.putLong(
                        storageBase,
                        storageAddress + 56L,
                        ((pitchBits >>> 27) & 31L)
                                | ((mPitchBits & 0xFFFFFFFFL) << 5)
                                | (mYawBits << 37));
                U.putByte(storageBase, storageAddress + 64L, (byte) (((mYawBits >>> 27) & 31L)));
            }
            long casingEnd =
                    PacketValues.putUtf8(storageBase, storageAddress + 65L, casing, casingBytes);
            {
                long smokingBits = smoking ? 1L : 0L;
                long smokeLifeBits = smokeLife;
                long smokeLiftBits = doubleToRawLongBits(smokeLift);
                long nodeLifeBits = nodeLife;
                long legacyBits = legacy ? 1L : 0L;
                U.putLong(
                        storageBase,
                        casingEnd,
                        smokingBits | ((smokeLifeBits & 0xFFFFFFFFL) << 1) | (smokeLiftBits << 33));
                U.putLong(
                        storageBase, casingEnd + 8L, (smokeLiftBits >>> 31) | (nodeLifeBits << 33));
                U.putByte(
                        storageBase,
                        casingEnd + 16L,
                        (byte) (((nodeLifeBits >>> 31) & 1L) | (legacyBits << 1)));
            }
            encodedBuffer.writerIndex(bufferStart + bodyBytes);
            return payload;
        } catch (Throwable encodingFailure) {
            payload.release();
            throw encodingFailure;
        }
    }

    static void receiveFields(
            ByteBuf decodedBuffer,
            PayloadHandlerContext context,
            Object storageBase,
            long storageAddress,
            int bufferStart,
            int bodyBytes) {
        if (bodyBytes < MIN_BODY_BYTES) throw new DecoderException("Invalid packet length");
        long bodyLimit = storageAddress + bodyBytes;
        long xWord = U.getLong(storageBase, storageAddress);
        long xToYWord = U.getLong(storageBase, storageAddress + 8L);
        long yToZWord = U.getLong(storageBase, storageAddress + 16L);
        long zToMXWord = U.getLong(storageBase, storageAddress + 24L);
        long mXToMYWord = U.getLong(storageBase, storageAddress + 32L);
        long mYToMZWord = U.getLong(storageBase, storageAddress + 40L);
        long mZToPitchWord = U.getLong(storageBase, storageAddress + 48L);
        long pitchToMYawWord = U.getLong(storageBase, storageAddress + 56L);
        long mYawWord = (U.getByte(storageBase, storageAddress + 64L) & 255L);
        double x = longBitsToDouble(((xWord >>> 5) | (xToYWord << 59)));
        double y = longBitsToDouble(((xToYWord >>> 5) | (yToZWord << 59)));
        double z = longBitsToDouble(((yToZWord >>> 5) | (zToMXWord << 59)));
        double mX = longBitsToDouble(((zToMXWord >>> 5) | (mXToMYWord << 59)));
        double mY = longBitsToDouble(((mXToMYWord >>> 5) | (mYToMZWord << 59)));
        double mZ = longBitsToDouble(((mYToMZWord >>> 5) | (mZToPitchWord << 59)));
        float yaw = intBitsToFloat((int) (mZToPitchWord >>> 5));
        float pitch = intBitsToFloat((int) ((mZToPitchWord >>> 37) | (pitchToMYawWord << 27)));
        float mPitch = intBitsToFloat((int) (pitchToMYawWord >>> 5));
        float mYaw = intBitsToFloat((int) ((pitchToMYawWord >>> 37) | (mYawWord << 27)));
        if ((mYawWord >>> 5) != 0) throw new DecoderException("Nonzero packet padding");
        int casingBytes = PacketValues.getVarInt(storageBase, storageAddress + 65L, bodyLimit);
        long casingAddress = storageAddress + 65L + PacketValues.varIntSize(casingBytes);
        if ((casingBytes & 0xFFFFFFFFL) > 2097151 || casingBytes > bodyLimit - casingAddress)
            throw new DecoderException("Invalid text length");
        String casing = PacketValues.getUtf8(storageBase, casingAddress, casingBytes, 2097151);
        long casingEnd = casingAddress + casingBytes;
        if (bodyLimit - casingEnd != 17) throw new DecoderException("Invalid packet tail length");
        long smokingToSmokeLiftWord = U.getLong(storageBase, casingEnd);
        long smokeLiftToNodeLifeWord = U.getLong(storageBase, casingEnd + 8L);
        long nodeLifeToLegacyWord = (U.getByte(storageBase, casingEnd + 16L) & 255L);
        boolean smoking = (smokingToSmokeLiftWord & 1L) != 0;
        int smokeLife = (int) (smokingToSmokeLiftWord >>> 1);
        double smokeLift =
                longBitsToDouble(
                        ((smokingToSmokeLiftWord >>> 33) | (smokeLiftToNodeLifeWord << 31)));
        int nodeLife = (int) ((smokeLiftToNodeLifeWord >>> 33) | (nodeLifeToLegacyWord << 31));
        boolean legacy = ((nodeLifeToLegacyWord >>> 1) & 1L) != 0;
        if ((nodeLifeToLegacyWord >>> 2) != 0) throw new DecoderException("Nonzero packet padding");
        DetailEffects.gunCasing(
                x, y, z, mX, mY, mZ, yaw, pitch, mPitch, mYaw, casing, smoking, smokeLife,
                smokeLift, nodeLife, legacy);
    }
}
