// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.handler.ArmorUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class BlockBarbedWireAcid extends BlockBarbedWire {

    public static final MapCodec<BlockBarbedWireAcid> CODEC = simpleCodec(BlockBarbedWireAcid::new);

    private static final int SUIT_DAMAGE = 1;

    public BlockBarbedWireAcid(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void sting(Level level, Entity entity) {
        super.sting(level, entity);
        if (entity instanceof Player player) ArmorUtil.damageWholeSuit(player, SUIT_DAMAGE);
    }
}
