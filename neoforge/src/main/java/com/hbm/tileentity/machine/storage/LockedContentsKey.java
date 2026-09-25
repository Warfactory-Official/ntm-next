// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.lib.Library;
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
                    Library.id("locked_contents_key"),
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
