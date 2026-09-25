// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.lib.Library;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class BlueprintPoolSyncPayload extends ThreadedPayload {

    public static final Type<BlueprintPoolSyncPayload> TYPE =
            new Type<>(Library.id("blueprint_pool_sync"));
    public static final StreamCodec<ByteBuf, BlueprintPoolSyncPayload> STREAM_CODEC =
            streamCodec(BlueprintPoolSyncPayload::decode);

    private final List<String> pools;

    private BlueprintPoolSyncPayload(List<String> pools) {
        this.pools = pools;
    }

    public static BlueprintPoolSyncPayload of() {
        return new BlueprintPoolSyncPayload(List.copyOf(GenericRecipes.pools().keySet()));
    }

    private static BlueprintPoolSyncPayload decode(ByteBuf buf) {
        FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
        int count = friendly.readInt();
        List<String> pools = new ArrayList<>(count);
        for (int i = 0; i < count; i++) pools.add(friendly.readUtf());
        return new BlueprintPoolSyncPayload(pools);
    }

    public static void apply(BlueprintPoolSyncPayload payload) {
        RecipeSource.publishClientPoolNames(payload.pools);
    }

    public List<String> pools() {
        return pools;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
        friendly.writeInt(pools.size());
        for (String pool : pools) friendly.writeUtf(pool);
    }

    @Override
    public @NotNull Type<BlueprintPoolSyncPayload> type() {
        return TYPE;
    }
}
