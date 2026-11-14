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

package com.hbm.blockentity.machine.storage;

import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class LockedContentsKey extends SavedData {

    private static final int KEY_BYTES = 32;
    public static final SavedDataType<LockedContentsKey> TYPE =
            new SavedDataType<>(
                    RegistryUtil.id("locked_contents_key"),
                    LockedContentsKey::generate,
                    Codec.BYTE_BUFFER.xmap(
                            LockedContentsKey::new, data -> ByteBuffer.wrap(data.key.getEncoded())),
                    null);

    private final SecretKey key;

    private LockedContentsKey(ByteBuffer encoded) {
        byte[] bytes = new byte[encoded.remaining()];
        encoded.duplicate().get(bytes);
        key = new SecretKeySpec(bytes, "AES");
    }

    private static LockedContentsKey generate() {
        byte[] bytes = new byte[KEY_BYTES];
        new SecureRandom().nextBytes(bytes);
        LockedContentsKey created = new LockedContentsKey(ByteBuffer.wrap(bytes));
        created.setDirty();
        return created;
    }

    static SecretKey get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE).key;
    }
}
