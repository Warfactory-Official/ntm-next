// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import java.util.function.IntFunction;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockModel<P> {

    static Identifier base(Identifier blockId) {
        return Identifier.fromNamespaceAndPath(
                blockId.getNamespace(), "block/" + blockId.getPath() + "_base");
    }

    static Identifier base(Block block) {
        return base(BuiltInRegistries.BLOCK.getKey(block));
    }

    static Material.Baked slot(
            ModelBaker baker, ResolvedModel carrier, TextureSlots slots, String slot) {
        if (slots.getMaterial(slot) == null) {
            throw new IllegalStateException(
                    carrier.debugName()
                            + " defines no texture slot '"
                            + slot
                            + "', which the block model resolving it needs; the carrier and the model that reads it "
                            + "are written apart, so one of the two names the slot the other does not");
        }
        return baker.materials().resolveSlot(slots, slot, carrier);
    }

    static String[][] partsByMask(IntFunction<String[]> selectParts) {
        String[][] byMask = new String[64][];
        for (int mask = 0; mask < 64; mask++) byMask[mask] = selectParts.apply(mask);
        return byMask;
    }

    P prepare();

    BlockStateModel.UnbakedRoot root(P prepared, Block block, BlockState state);

    default Prepared prepared() {
        P prepared = prepare();
        return (block, state) -> root(prepared, block, state);
    }

    @FunctionalInterface
    interface Prepared {
        BlockStateModel.UnbakedRoot root(Block block, BlockState state);
    }
}
