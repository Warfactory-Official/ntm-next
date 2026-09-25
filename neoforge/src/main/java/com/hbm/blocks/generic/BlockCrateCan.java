// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.items.ModItems;
import com.hbm.items.food.ItemConserve.EnumFoodType;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import com.hbm.world.NtmWorldgenFields;
import com.hbm.world.WorldgenHash;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlockCrateCan extends Block {

    private static final long CONTENTS = WorldgenHash.identifier(Library.id("crate_can"));

    private static List<ItemStack> poolCache;

    public BlockCrateCan(Properties props) {
        super(props);
    }

    private static List<ItemStack> pool() {
        if (poolCache == null) {
            List<ItemStack> list = new ArrayList<>();
            for (EnumFoodType type : EnumFoodType.values())
                list.add(ModItems.CANNED_CONSERVE.stack(type));
            list.add(new ItemStack(ModItems.CAN_SMART));
            list.add(new ItemStack(ModItems.CAN_CREATURE));
            list.add(new ItemStack(ModItems.CAN_REDBOMB));
            list.add(new ItemStack(ModItems.CAN_MRSUGAR));
            list.add(new ItemStack(ModItems.CAN_OVERCHARGE));
            list.add(new ItemStack(ModItems.CAN_LUNA));
            list.add(new ItemStack(ModItems.CAN_BREEN));
            list.add(new ItemStack(ModItems.CAN_BEPIS));
            list.add(new ItemStack(ModItems.PUDDING));
            poolCache = list;
        }
        return poolCache;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!held.is(ModItems.CROWBAR.get())) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            RandomSource rand = NtmWorldgenFields.get(server).random(CONTENTS, pos);
            List<ItemStack> pool = pool();
            int count = 5 + rand.nextInt(4);
            for (int i = 0; i < count; i++)
                popResource(level, pos, pool.get(rand.nextInt(pool.size())).copy());
            level.removeBlock(pos, false);
            level.playSound(null, pos, ModSounds.CRATE_BREAK.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
