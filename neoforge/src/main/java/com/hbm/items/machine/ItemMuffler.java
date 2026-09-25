// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ItemMuffler extends Item {

    public ItemMuffler(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos target = MultiblockSurface.coreOfAny(level, ctx.getClickedPos());
        if (target == null) target = ctx.getClickedPos();

        BlockEntity be = level.getBlockEntity(target);
        if (!(be instanceof BlockEntityMachineBase machine) || machine.isMuffled())
            return InteractionResult.PASS;

        if (!level.isClientSide()) {
            machine.setMuffled();
            Player player = ctx.getPlayer();
            level.playSound(
                    null,
                    player != null ? player.blockPosition() : target,
                    ModSounds.UPGRADE_PLUG.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            ctx.getItemInHand().consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
