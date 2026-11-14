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

package com.hbm.inventory.material;

import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class MaterialStack implements SyncSource {

    public static final Codec<MaterialStack> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            MaterialRegistry.CODEC
                                                    .fieldOf("material")
                                                    .forGetter(stack -> stack.material),
                                            Codec.INT
                                                    .fieldOf("amount")
                                                    .forGetter(stack -> stack.amount))
                                    .apply(i, MaterialStack::new));

    public static final StreamCodec<ByteBuf, MaterialStack> STREAM_CODEC =
            StreamCodec.composite(
                    MaterialRegistry.STREAM_CODEC,
                    stack -> stack.material,
                    ByteBufCodecs.VAR_INT,
                    stack -> stack.amount,
                    MaterialStack::new);

    public final Material material;
    public int amount;

    public MaterialStack(Material material, int amount) {
        this.material = material;
        this.hbm$syncSet$MaterialStack$amount(amount);
    }

    public MaterialStack copy() {
        return new MaterialStack(material, amount);
    }

    public final void hbm$syncSet$MaterialStack$amount(int next) {
        if (this.amount != next) {
            this.amount = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(MaterialStack.class, "material", 3, 0L);
    }

    private SyncSource hbm$syncOwner;

    private int hbm$syncMask;

    private int hbm$syncBindings;

    private long hbm$syncUnits;

    public final boolean syncBound() {
        return hbm$syncOwner != null;
    }

    public final void syncChanged(int mask) {
        if (hbm$syncOwner == null) return;
        if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
        else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
    }

    public final void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    public final void bindSync(SyncSource owner, int mask) {
        bindSyncUnits(owner, mask, 0);
    }

    public final void bindSyncUnits(SyncSource owner, int mask, long units) {
        if (hbm$syncOwner != null && hbm$syncOwner != owner) {
            throw new IllegalStateException("Mutable sync state has two owners");
        }
        hbm$syncOwner = owner;
        hbm$syncMask |= mask;
        hbm$syncUnits |= units;
        if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
    }

    public final void unbindSync(SyncSource owner) {
        assert hbm$syncOwner == owner;
        if (--hbm$syncBindings != 0) return;
        SyncBindings.unbindFields(this, owner);
        hbm$syncOwner = null;
        hbm$syncMask = 0;
        hbm$syncUnits = 0;
    }
}
