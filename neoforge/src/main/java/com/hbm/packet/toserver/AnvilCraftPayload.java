// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.advancement.HbmCriteria;
import com.hbm.inventory.container.MenuAnvil;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipe.AnvilOutput;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipes;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class AnvilCraftPayload extends ThreadedPayload {

    public static final Type<AnvilCraftPayload> TYPE = new Type<>(Library.id("anvil_craft"));
    public static final StreamCodec<ByteBuf, AnvilCraftPayload> STREAM_CODEC =
            streamCodec(AnvilCraftPayload::decode);

    private final String recipeName;
    private final int mode;

    public AnvilCraftPayload(String recipeName, int mode) {
        this.recipeName = recipeName;
        this.mode = mode;
    }

    private static AnvilCraftPayload decode(ByteBuf buf) {
        return new AnvilCraftPayload(
                ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf));
    }

    public static void handleServer(AnvilCraftPayload payload, IPayloadHandlerContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        AnvilConstructionRecipe recipe =
                AnvilConstructionRecipes.INSTANCE.getRecipe(payload.recipeName);
        if (recipe == null) return;
        if (!(sp.containerMenu instanceof MenuAnvil anvil)) return;
        if (!recipe.isTierValid(anvil.tier)) return;

        List<AnvilOutput> outputs = recipe.outputs();

        int count =
                payload.mode != 1
                        ? 1
                        : outputs.size() > 1
                                ? 64
                                : outputs.get(0).stack().getMaxStackSize()
                                        / outputs.get(0).stack().getCount();

        for (int i = 0; i < count; i++) {
            if (!InventoryUtil.consumeIngredients(sp, recipe.input())) break;
            InventoryUtil.giveChanceOutputs(sp, outputs);

            HbmCriteria.itemCrafted(sp, outputs.get(0).stack());
        }

        anvil.broadcastFullState();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.STRING_UTF8.encode(buf, recipeName);
        ByteBufCodecs.VAR_INT.encode(buf, mode);
    }

    @Override
    public @NotNull Type<AnvilCraftPayload> type() {
        return TYPE;
    }
}
