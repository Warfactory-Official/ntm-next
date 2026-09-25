// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

final class BoxDuctData {
    static final int[] OFFSETS;
    static final float[] VALUES;
    static final byte[] QUADRANTS;

    static {
        try (InputStream resource =
                BoxDuctData.class.getResourceAsStream("/assets/hbm/uv/box_duct.bin")) {
            if (resource == null) throw new IllegalStateException("missing box duct model data");
            DataInputStream input = new DataInputStream(resource);
            if (input.readInt() != 0x42445556 || input.readInt() != 1 || input.readInt() != 640) {
                throw new IllegalStateException("invalid box duct model data");
            }
            int count = input.readInt();
            if (count < 640 || count > 4480)
                throw new IllegalStateException("invalid box duct part count");
            OFFSETS = new int[641];
            for (int index = 0; index < OFFSETS.length; index++) OFFSETS[index] = input.readInt();
            if (OFFSETS[0] != 0 || OFFSETS[640] != count) {
                throw new IllegalStateException("invalid box duct offsets");
            }
            VALUES = new float[count * 30];
            QUADRANTS = new byte[count];
            for (int part = 0; part < count; part++) {
                int at = part * 30;
                for (int value = 0; value < 30; value++) VALUES[at + value] = input.readFloat();
                QUADRANTS[part] = input.readByte();
            }
            if (input.read() != -1) throw new IllegalStateException("trailing box duct model data");
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private BoxDuctData() {}
}
