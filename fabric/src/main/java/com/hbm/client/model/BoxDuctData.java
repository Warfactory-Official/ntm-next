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
